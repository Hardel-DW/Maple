package fr.hardel.mapple.mixin.block;

import fr.hardel.mapple.optimisation.LeafDistance;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin implements LeafDistance {
    @Unique
    private int mapple$leafDistance;

    @Override
    public int mapple$leafDistance() {
        return this.mapple$leafDistance;
    }

    @Override
    public void mapple$refreshLeafDistance() {
        this.mapple$leafDistance = LeavesBlockInvoker.mapple$getDistanceAt((BlockState) (Object) this);
    }
}
