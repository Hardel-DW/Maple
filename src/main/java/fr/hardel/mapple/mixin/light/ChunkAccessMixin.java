package fr.hardel.mapple.mixin.light;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Shadow
    private boolean isLightCorrect;

    @Inject(method = "setLightCorrect", at = @At("HEAD"), cancellable = true)
    private void mapple$ignoreUnchanged(boolean lightCorrect, CallbackInfo callback) {
        if (this.isLightCorrect == lightCorrect) {
            callback.cancel();
        }
    }
}
