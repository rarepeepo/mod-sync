package rare.peepo.modsync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import rare.peepo.modsync.ModSync;
import rare.peepo.modsync.Util;

public final class Config {
    private static Path path;
    
    public static URL Url;
    public static SyncAction Action = SyncAction.PROMPT;
    
    public static void init() {
        try {
            var dir = Util.getGameDirectory();
            var file = ModSync.ID + ".json";
            var temp = dir.resolve("mods").resolve(file);
            path = dir.resolve("config").resolve(file);
            if (Files.exists(temp)) {
                Files.createDirectories(path.getParent());
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); 
            }
            if (Files.exists(path))
                parseConfig(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static void parseConfig(Path path) {
        try {
            var j = new Gson().fromJson(
                    Files.readString(path), JsonObject.class);
            if (j.has("url"))
                Config.Url = new URL(j.get("url").getAsString());
            if (j.has("syncAction"))
                Config.Action = SyncAction.valueOf(j.get("syncAction").getAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void save() {
        try {
            var s = new JsonObject();
            if (Config.Url != null)
                s.addProperty("url", Config.Url.toString());
            if (Config.Action != null)
                s.addProperty("syncAction", Config.Action.toString());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    new GsonBuilder().setPrettyPrinting().create().toJson(s)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
