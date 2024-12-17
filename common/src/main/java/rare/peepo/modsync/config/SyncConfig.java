package rare.peepo.modsync.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import rare.peepo.modsync.Util;

public class SyncConfig {
    final List<InstallEntry> install;
    final List<String> remove;

    public SyncConfig() {
        install = new ArrayList<InstallEntry>();
        remove = new ArrayList<String>();
    }

    public List<InstallEntry> getInstallEntries() {
        return install;
    }

    public List<String> getRemoveEntries() {
        return remove;
    }

    public static SyncConfig parse(String json) {
        var ret = new SyncConfig();
        var gson = new Gson();
        var j = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        var baseUrl = "";
        for (var e : j.get("install").getAsJsonObject().entrySet()) {
            if (e.getKey().equals("baseUrl")) {
                baseUrl = e.getValue().getAsString();
                if (!baseUrl.endsWith("/"))
                    baseUrl = baseUrl + "/";
            } else {
                ret.install.addAll(
                        parseEntries(e.getKey(), baseUrl, e.getValue().getAsJsonArray())
                );
            }
        }
        if (j.has("remove")) {
            for (var e : j.get("remove").getAsJsonArray())
                ret.remove.add(e.getAsString());
        }
        return ret;
    }

    static List<InstallEntry> parseEntries(String dir, String baseUrl, JsonArray entries) {
        var ret = new ArrayList<InstallEntry>();
        for (var e : entries) {
            var entry = new InstallEntry();
            var url = "";
            if (e.isJsonPrimitive()) {
                url = e.getAsString();
            } else {
                var j = e.getAsJsonObject();
                url = j.get("url").getAsString();
                if (j.has("sha1"))
                    entry.Sha1 = j.get("sha1").getAsString().toLowerCase();
                if (j.has("target"))
                    entry.Target = j.get("target").getAsString();
                if (j.has("action"))
                    entry.Action = j.get("action").getAsString();
                if (j.has("name"))
                    entry.Path = Util.resolvePath(dir, j.get("name").getAsString());
            }
            if (!url.toLowerCase().startsWith("http"))
                url = baseUrl + url;
            try {
                entry.Url = new URL(url);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (entry.Path == null) {
                entry.Path = Util.resolvePath(dir,
                        new File(entry.Url.getPath()).getName());
            }
            ret.add(entry);
        }
        return ret;
    }
}
