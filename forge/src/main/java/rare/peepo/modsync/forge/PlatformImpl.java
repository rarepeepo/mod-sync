/*
package rare.peepo.modsync.forge;

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

public class PlatformImpl {
    public static Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }
    
    public static String[] getLaunchArguments() {
        try {
            var field = Launcher.class.getDeclaredField("argumentHandler");
            if (field == null) {
                throw new Exception("Field 'argumentHandler' could not be found on " +
                        Launcher.class.getName());
            }
            field.setAccessible(true);
            
            return ((ArgumentHandler) field.get(Launcher.INSTANCE)).buildArgumentList();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not get launch arguments from Forge launcher.", e);
        }
    }
}
*/
