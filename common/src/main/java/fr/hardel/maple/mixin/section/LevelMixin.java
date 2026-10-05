package fr.hardel.maple.mixin.section;

import fr.hardel.maple.optimisation.section.SharedSectionData;
import fr.hardel.maple.optimisation.section.SharedSectionDataHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin implements SharedSectionDataHolder {
    @Unique
    private SharedSectionData maple$sharedSectionData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void maple$createSharedSectionData(CallbackInfo callback) {
        this.maple$sharedSectionData = new SharedSectionData(((Level) (Object) this).palettedContainerFactory());
    }

    @Override
    public SharedSectionData maple$sharedSectionData() {
        return this.maple$sharedSectionData;
    }
}
