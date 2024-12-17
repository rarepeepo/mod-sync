package rare.peepo.modsync;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Util {
    
    public static String downloadString(URL url) {
        try (var in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void downloadFile(URL url, Path filePath) {
        try (var in = url.openStream()) {
            Files.createDirectories(filePath.getParent());
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String computeSha1(Path filePath) {
        try {
            try (var in = Files.newInputStream(filePath)) {
                var sha = MessageDigest.getInstance("SHA-1");
                var buf = new byte[8192];
                var len = in.read(buf);
                while (len != -1) {
                    sha.update(buf, 0, len);
                    len = in.read(buf);
                }
                var s = new StringBuilder();
                for (var b : sha.digest())
                    s.append(String.format("%02x", b));
                return s.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static List<Path> getFiles(Path directory, String globPattern) {
        try {
            var p = directory.toString().replace("\\", "/");
            if (!p.endsWith("/"))
                p = p + "/";
            p = p + globPattern;
            var matcher = FileSystems.getDefault().getPathMatcher("glob:" + p);
            try(var stream = Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .filter(matcher::matches)) {
                return stream.collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // Make sure path can't escape the base game directory, see:
    // https://stackoverflow.com/a/33084369
    // https://stackoverflow.com/a/34658355
    public static Path resolvePath(String first, String... more) {
        var basePath = getGameDirectory().normalize().toAbsolutePath();
        var p = Path.of(first, more);
        if (p.isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative: " + p);
        }
        // Join the two paths together, then normalize so that any ".." elements
        // in the userPath can remove parts of baseDirPath.
        // (e.g. "/foo/bar/baz" + "../attack" -> "/foo/bar/attack")
        var resolvedPath = basePath.resolve(p).normalize();
        // Make sure the resulting path is still within the required directory.
        // (In the example above, "/foo/bar/attack" is not.)
        if (!resolvedPath.startsWith(basePath)) {
          throw new IllegalArgumentException("Path '" + resolvedPath +
                  "' escapes the base path '" +basePath + "'");
        }
        return resolvedPath;
    }
    
    public static Process runRemove(List<String> args) {
        try {
            // Extract Remove.class from our Jar and run that separately, rather than
            // running it from within our own Jar. Otherwise we wouldn't be able to
            // replace our own mod because Java keeps the Jar file locked when it's
            // being run.
            var name = "Remove";
            var cname = name + ".class";
            var path = Path.of(ModSync.ID, cname).toAbsolutePath();
            var res = Util.class.getResourceAsStream("/" + cname);
            Files.createDirectories(path.getParent());
            Files.copy(res, path, StandardCopyOption.REPLACE_EXISTING);
            
            var javaHome = System.getProperty("java.home");
            var javaBin = Path.of(javaHome, "bin", "javaw").toString();
            var classpath = System.getProperty("java.class.path") + ";" + path.getParent();
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-cp");
            command.add(classpath);
            command.add(name);
            if (args != null)
                command.addAll(args);
            return new ProcessBuilder(command)
                    .inheritIO()
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Gets the path to the .jar file the given class is located in.
     * 
     * See
     * https://stackoverflow.com/a/320595
     */
    @SuppressWarnings("rawtypes")
    static String getJarPath(Class clazz) {
        try {
            // For whatever reason on Forge this returns a string like
            // jar:file:///C:/path/to/jar/file/mod.jar!/
            var loc = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
            var uri = new URI(loc.replaceAll("^jar:(.+)!/$", "$1"));
        
            return new File(uri).getPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Gets the path to the game directory.
     */
    public static Path getGameDirectory() {
        return Path.of(getJarPath(Util.class))
                .getParent()  // .minecraft/mods
                .getParent(); // .minecraft
    }
    
    /**
     * Relaunches the game.
     * 
     * This isn't used, it's just here in case I want to mess with it further in the
     * future. It currently works for Fabric, but doesn't work for Forge.
     * 
     * Fabric:
     *  The entry point is "net.fabricmc.loader.impl.launch.knot.KnotClient" and it
     *  just takes all the parameters as arguments to main() as you would expect.
     *  
     * Forge:
     *  The entry point apparently is "net.minecraftforge.bootstrap.ForgeBootstrap"
     *  and it also expects it's  parameters as arguments to main().
     *  
     *  The Qt Launcher application does not directly invoke this, though, but rather
     *  invokes yet another "forgewrapper" jar that then apparently adds additional
     *  command line arguments such as "--launch_target forge_client" and possibly
     *  others and then in turn invokes Forge's bootstrap stuff.
     *  
     *  I don't think it's worth to even investigate this any further, it's way too
     *  prone to breaking or falling apart at the slightest change of the mod
     *  loader and would only couple our mod tightly to specific mod loader versions.
     *  
     *  And even if you succeeded in launching the game, the Qt Launcher application
     *  would no longer be updating it's log output window since the original process
     *  is gone and it'd just confuse users, so it's probably not a good idea
     *  anyway.
     */
    /*
    public static Process relaunchGame() {
        try {
            var javaHome = System.getProperty("java.home");
            var javaBin = Path.of(javaHome, "bin", "javaw").toString();
            var classpath = System.getProperty("java.class.path");
            // Fabric
            var className = "net.fabricmc.loader.impl.launch.knot.KnotClient";
            // Forge
            // var className = "net.minecraftforge.bootstrap.ForgeBootstrap"
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            // https://stackoverflow.com/a/4160543/3700562
            for (var a : ManagementFactory.getRuntimeMXBean().getInputArguments())
                command.add(a);
            command.add("-cp");
            command.add(classpath);
            command.add(className);
            for (var p : Platform.getLaunchArguments())
                    command.add(p);
            return new ProcessBuilder(command)
                    .inheritIO()
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    */
}
