package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.SharedSectionData;
import fr.hardel.mapple.optimisation.section.SharedSectionDataHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin implements SharedSectionDataHolder {
    @Unique
    private SharedSectionData mapple$sharedSectionData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$createSharedSectionData(CallbackInfo callback) {
        this.mapple$sharedSectionData = new SharedSectionData(((Level) (Object) this).palettedContainerFactory());
    }

    @Override
    public SharedSectionData mapple$sharedSectionData() {
        return this.mapple$sharedSectionData;
    }
}
