package fr.hardel.maple.mixin.block;

import fr.hardel.maple.optimisation.LeafDistance;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin implements LeafDistance {
    @Unique
    private int maple$leafDistance;

    @Override
    public int maple$leafDistance() {
        return this.maple$leafDistance;
    }

    @Override
    public void maple$refreshLeafDistance() {
        this.maple$leafDistance = LeavesBlockInvoker.maple$getDistanceAt((BlockState) (Object) this);
    }
}
