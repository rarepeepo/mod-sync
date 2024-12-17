
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.slf4j.LoggerFactory;

public class Remove {
    
    /**
     * The launcher locks the .jar files in the mods directory so we can't delete
     * them from within that process so we run this program to do the job for us.
     * 
     * @param args A list of files to delete.
     */
    public static void main(String[] args) {
        try {
            // Wait for parent process to end.
            var parent = ProcessHandle.current().parent().orElse(null);
            if (parent != null)
                parent.onExit().get();
            
            var files = new HashMap<Path, Integer>();
            var maxRetry = 3;
            for (var f : args)
                files.put(Path.of(f), maxRetry);

            for (var i = 0; i < maxRetry; i++) {
                var error = false;
                for (var pair : files.entrySet()) {
                    try {
                        if (pair.getValue() > 0) {
                            log("Deleting '{}'", pair.getKey());
                            Files.delete(pair.getKey());
                            pair.setValue(0);
                        }
                    } catch (Exception e) {
                        error = true;
                        pair.setValue(pair.getValue() - 1);
                        log("Failed to delete '{}', {} attempts left",
                                pair.getKey(), pair.getValue());
                        log(e.toString());
                    }
                }
                if (error)
                    Thread.sleep(250 + i * 250);
                else
                    break;
            }
        } catch (Exception e) {
            log("An unexpected error occurred: {}", e.getMessage());
            log(e.toString());
        }
    }
    
    static void log(String format, Object... arguments) {
        var name = Remove.class.getSimpleName();
        LoggerFactory.getLogger(name).info(
                String.format("[%s] %s", name, format), arguments
        );
    }
}
