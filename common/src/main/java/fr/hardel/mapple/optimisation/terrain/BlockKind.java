package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.world.level.block.state.BlockState;

/** What the terrain passes tell blocks apart by: air, a block holding a fluid, or any other block, which vanilla's surface calls stone. */
public final class BlockKind {
    public static final byte AIR = 0;
    public static final byte FLUID = 1;
    public static final byte SOLID = 2;

    private BlockKind() {
    }

    static byte of(BlockState state) {
        if (state.isAir()) {
            return AIR;
        }

        return state.getFluidState().isEmpty() ? SOLID : FLUID;
    }
}
