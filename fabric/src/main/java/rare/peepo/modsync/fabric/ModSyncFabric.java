/*
package rare.peepo.modsync.fabric;

import net.fabricmc.api.ModInitializer;

import rare.peepo.modsync.ModSync;

public final class ModSyncFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        ModSync.onInitialize();
    }
}
*/

/*
 * Add this to fabric.mod.json to enable the default entry point for mods.
 * Note that uncommenting the above code will add a dependency to Fabric API
 * which must then be manually installed first before modsync can be used.
 *
  
  "entrypoints": {
    "main": [
      "rare.peepo.modsync.fabric.ModSyncFabric"
    ]
  }
  ...
  "depends": {
     ...
    "fabric-api": "*"
  }
  
*/
