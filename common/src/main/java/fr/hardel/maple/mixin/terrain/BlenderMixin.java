package fr.hardel.maple.mixin.terrain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Blender.class)
public abstract class BlenderMixin {
    @WrapOperation(method = "of", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;isOldChunkAround(Lnet/minecraft/world/level/ChunkPos;I)Z"))
    private static boolean maple$blendingDataInTheRegion(WorldGenRegion region, ChunkPos center, int range, Operation<Boolean> original) {
        for (int x = center.x() - range; x <= center.x() + range; x++) {
            for (int z = center.z() - range; z <= center.z() + range; z++) {
                if (region.getChunk(x, z).getBlendingData() != null) {
                    return true;
                }
            }
        }

        return false;
    }
}
