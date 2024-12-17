package rare.peepo.modsync;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ModSyncPlugin implements IMixinConfigPlugin  {
    /**
     * We just hijack this and use it as our entrypoint because it's
     * called very early on during the loading of the game, before any
     * of the mod initializers are called by the loader.
     * 
     * This is useful to cut down on unneccessary load times because the
     * user will have to relaunch the game if we install or remove any
     * files.
     */
    @Override
    public void onLoad(String mixinPackage) {
        ModSync.onLoad();
    }

    @Override
    public String getRefMapperConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<String> getMixins() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // TODO Auto-generated method stub
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // TODO Auto-generated method stub
    }
}
