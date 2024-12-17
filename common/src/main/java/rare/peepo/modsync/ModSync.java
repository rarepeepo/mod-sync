package rare.peepo.modsync;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import rare.peepo.modsync.config.Config;
import rare.peepo.modsync.config.InstallEntry;
import rare.peepo.modsync.config.SyncAction;
import rare.peepo.modsync.config.SyncConfig;

public final class ModSync {
    public static final String ID = "modsync";
    public static final String NAME = "Mod Sync";
    
    // This is the entry point for the mod loaders.
    public static void onInitialize() {
    }
    
    // This is called before any mods are initialized.
    public static void onLoad() {
        var numInstalled = 0;
        List<String> remove = new ArrayList<String>();
        var errors = new ArrayList<String>();
        try {
            Config.init();
            if(Config.Url == null) {
                Log.info("No download url configured, exiting.");
            } else if (Config.Action == SyncAction.IGNORE) {
                Log.info("Skipping sync due to config setting.");
            } else {
                Log.info("Downloading sync config from {}", Config.Url);
                var syncConfig = getSyncConfig (Config.Url);
                var installEntries = syncConfig.getInstallEntries();
                Log.info("Processing sync config...");
                numInstalled = processInstallEntries(installEntries, errors);
                remove = processRemoveEntries(
                        syncConfig.getRemoveEntries(), installEntries, errors
                );
            }
        } catch (Exception e) {
            Log.error("Error while syncing files: {}", e);
        }
        if (!errors.isEmpty()) {
            // Show an error dialog?
        }
        if (!remove.isEmpty())
            Util.runRemove(remove);
        // TODO:
        // Now that we have actually managed to extract the launch arguments,
        // we might be able to restart the mod launcher process automatically.
        // Not sure we want that, though.
        if (numInstalled > 0 || !remove.isEmpty())
            alertAndExit();
    }
    
    static SyncConfig getSyncConfig (URL url) {
        
        return SyncConfig.parse(
                Util.downloadString(url)
        );
        /*
        try {
            var p = Util.getGameDirectory();
            var f = p.resolve("../sync.config.json").toFile();
            if (!f.exists())
                f = p.resolve("../../sync.config.json").toFile();
            var reader = new BufferedReader(new FileReader(f.toString()));
            try {
                var sb = new StringBuilder();
                var s = "";
                while ((s = reader.readLine()) != null)
                    sb.append(s);
                var json = sb.toString();
                return SyncConfig.parse(json);
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        */
    }
    
    static int processInstallEntries(List<InstallEntry> installEntries, List<String> errors) {
        var numInstalled = 0;
        for (var e : installEntries) {
            try {
                if(processInstallEntry(e))
                    numInstalled++;
            } catch (Exception ex) {
                Log.error("Error processing install entry for {}: {}", e.Path, ex);
                errors.add(ex.getMessage());
            }
        }
        return numInstalled;
    }
    
    static boolean processInstallEntry(InstallEntry e) {
        if (!Files.exists(e.Path)) {
            if (Config.Action == SyncAction.PROMPT && !confirmAction(e.Path, false))
                return false;
            Log.info("Downloading {} to {}", e.Url, e.Path);
            Util.downloadFile(e.Url, e.Path);
        } else {
            // The file already exists. If the entry does not provide a SHA-1 hash,
            // we ignore it.
            if (e.Sha1 == null)
                return false;
            // Compute hash of our local file.
            var hash = Util.computeSha1(e.Path);
            if (e.Sha1.equals(hash))
                return false;
            if (Config.Action == SyncAction.PROMPT && !confirmAction(e.Path, false))
                return false;
            // Our local file is different from the server's file, so we overwrite the
            // local file with one from the server.
            Log.info("Downloading {} and overwriting existing file {} due to " +
                     "hash mismatch: {} <-> {}", e.Url, e.Path, e.Sha1, hash);
            Util.downloadFile(e.Url, e.Path);
        }
        // TODO:
        // If e.Target != null
        //  Deal with merge stuff.
        return true;
    }
    
    static List<String> processRemoveEntries(List<String> removeEntries, List<InstallEntry> installEntries,
            List<String> errors) {
        var remove = new ArrayList<String>();
        // We ignore files that are installed by install entries. Otherwise we might end up
        // deleting our own files that we just installed.
        var installFiles = new HashSet<Path>();
        for (var e : installEntries)
            installFiles.add(e.Path);
        for (var e : removeEntries) {
            try {
                remove.addAll(
                        processRemoveEntry(e, installFiles)
                );
            } catch (Exception ex) {
                Log.error("Error processing remove entry {}: {}", e, ex);
                errors.add(ex.getMessage());
            }
        }
        return remove;
    }
    
    static List<String> processRemoveEntry(String pattern, Set<Path> installFiles) {
        // Surprisingly, it's fairly slow to run a pattern like "mods/some-file-*" on
        // the .minecraft base directory, so we narrow the directory down at the expense
        // that glob patterns are now restricted to filenames.
        var p = pattern.replace('\\', '/');
        var i = p.lastIndexOf('/');
        var fileName = p.substring(i < 0 ? 0 : i + 1);
        var relPath = p.substring(0, i < 0 ? 0 : i);
        var dir = Util.resolvePath(relPath);
        
        var remove = new ArrayList<String>();
        if (!Files.exists(dir))
            return remove;
        for (var file : Util.getFiles(dir, fileName)) {
//        for (var file : Util.getFiles(Util.getGameDirectory(), pattern)) {
            if(installFiles.contains(file)) {
                continue;
            } else {
                if (Config.Action == SyncAction.PROMPT && !confirmAction(file, true))
                    continue;
                // We can't delete the files here because the launcher locks the .jar files
                // in the mods directory. Instead we add them to a list and pass that to a
                // little utility program that waits for the launcher process to exit and
                // then deletes the files later on.
                Log.info("Marking '{}' for removal because of matching removal pattern '{}'",
                        file, pattern);
                remove.add(
                        file.toAbsolutePath().normalize().toString()
                );
            }
        }
        return remove;
    }
    
    static void alertAndExit() {
        System.setProperty("java.awt.headless", "false");
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch(Exception e) {}
        var dialog = new JDialog();
        dialog.setAlwaysOnTop(true);
        var s = "New mod files were installed. Please start the game one more time.";
        Log.info(s);
        JOptionPane.showMessageDialog(dialog, s, NAME,
                JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
    
    static boolean confirmAction(Path filePath, boolean remove) {
        System.setProperty("java.awt.headless", "false");
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch(Exception e) {}
        var dialog = new JDialog();
        dialog.setAlwaysOnTop(true);
        var s = NAME + " wants to download the new file \"" + filePath.getFileName() +
                "\" and install it to:\n\n" + filePath.getParent() +
                "\n\nDo you want to install it?";
        if (remove) {
            s = NAME + " wants to remove the following file:\n\n" + filePath +
                    "\r\n\r\nDo you want to remove it?";
        }
        return JOptionPane.showConfirmDialog(dialog, s, NAME,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
