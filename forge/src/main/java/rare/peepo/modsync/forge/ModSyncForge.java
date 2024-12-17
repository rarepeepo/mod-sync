package rare.peepo.modsync.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import rare.peepo.modsync.ModSync;
import rare.peepo.modsync.gui.screen.ConfigScreen;

@Mod(ModSync.ID)
public final class ModSyncForge {
    @SuppressWarnings("removal")
    public ModSyncForge() {
        // Run our common setup.
        ModSync.onInitialize();
        
        if(FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (client, parent) -> new ConfigScreen(parent))
            );
        }
    }
}
