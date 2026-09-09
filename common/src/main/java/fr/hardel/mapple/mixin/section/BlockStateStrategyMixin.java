package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.PackedConfiguration;
import net.minecraft.world.level.chunk.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.chunk.Strategy$1")
public abstract class BlockStateStrategyMixin {
    @Inject(method = "getConfigurationForBitCount", at = @At("HEAD"), cancellable = true)
    private void mapple$packBelowStorage(int entryBits, CallbackInfoReturnable<Configuration> callback) {
        if (entryBits >= 1 && entryBits <= 3) {
            callback.setReturnValue(PackedConfiguration.belowStorage(entryBits));
        }
    }
}
