package fr.hardel.mapple.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.hardel.mapple.optimisation.LeafDistance;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @WrapOperation(
        method = {"updateDistance", "updateShape"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeavesBlock;getDistanceAt(Lnet/minecraft/world/level/block/state/BlockState;)I"),
        require = 2
    )
    private static int mapple$cachedDistance(BlockState state, Operation<Integer> original) {
        return ((LeafDistance) state).mapple$leafDistance();
    }
}
