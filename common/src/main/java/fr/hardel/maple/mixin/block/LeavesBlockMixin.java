package fr.hardel.maple.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.hardel.maple.optimisation.LeafDistance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean maple$skipUnchanged(ServerLevel level, BlockPos pos, BlockState newState, Operation<Boolean> original, @Local(argsOnly = true) BlockState state) {
        return newState != state && original.call(level, pos, newState);
    }

    @WrapOperation(
        method = {"updateDistance", "updateShape"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeavesBlock;getDistanceAt(Lnet/minecraft/world/level/block/state/BlockState;)I"),
        require = 2
    )
    private static int maple$cachedDistance(BlockState state, Operation<Integer> original) {
        return ((LeafDistance) state).maple$leafDistance();
    }
}
