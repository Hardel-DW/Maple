package fr.hardel.maple.mixin.block;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LeavesBlock.class)
public interface LeavesBlockInvoker {
    @Invoker("getDistanceAt")
    static int maple$getDistanceAt(BlockState state) {
        throw new AssertionError();
    }
}
