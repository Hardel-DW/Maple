package fr.hardel.mapple.optimisation.surface;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jspecify.annotations.Nullable;

/** The compiled rules of one chunk. Each column resolves again the guards reading the column. */
final class CompiledRules {
    private final SurfaceRules.SurfaceRule root;
    private final Guard[] columnGuards;

    CompiledRules(SurfaceRules.SurfaceRule root, Guard[] columnGuards) {
        this.root = root;
        this.columnGuards = columnGuards;
    }

    /** After the context moved to the column. */
    void moveToColumn() {
        for (Guard guard : this.columnGuards) {
            guard.resolve();
        }
    }

    @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
        return this.root.tryApply(blockX, blockY, blockZ);
    }
}
