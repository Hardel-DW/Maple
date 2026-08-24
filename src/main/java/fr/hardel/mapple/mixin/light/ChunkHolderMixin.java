package fr.hardel.mapple.mixin.light;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
    @Redirect(method = "sectionLightChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;markUnsaved()V"))
    private void mapple$keepSavedState(ChunkAccess chunk) {
    }
}
