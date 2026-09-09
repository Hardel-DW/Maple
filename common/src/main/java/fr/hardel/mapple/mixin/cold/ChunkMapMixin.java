package fr.hardel.mapple.mixin.cold;

import fr.hardel.mapple.optimisation.cold.ColdStorageHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A generation task releases its claims when it is done; the holder it leaves unclaimed may rest. */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "releaseGeneration", at = @At("TAIL"))
    private void mapple$restTheReleasedHolder(GenerationChunkHolder holder, CallbackInfo callback) {
        ((ColdStorageHolder) this.level).mapple$coldStorage().rest(holder);
    }
}
