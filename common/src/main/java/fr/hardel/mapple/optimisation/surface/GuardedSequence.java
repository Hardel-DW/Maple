package fr.hardel.mapple.optimisation.surface;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jspecify.annotations.Nullable;

/** Vanilla's sequence, first result wins, where an entry is skipped for a block outside its guard: it would have returned nothing. */
final class GuardedSequence implements SurfaceRules.SurfaceRule {
    private final SurfaceRules.Context context;
    private final SurfaceRules.SurfaceRule[] rules;
    private final @Nullable Guard[] guards;

    GuardedSequence(SurfaceRules.Context context, SurfaceRules.SurfaceRule[] rules, @Nullable Guard[] guards) {
        this.context = context;
        this.rules = rules;
        this.guards = guards;
    }

    @Override
    public @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
        int above = this.context.stoneDepthAbove;
        int below = this.context.stoneDepthBelow;
        for (int index = 0; index < this.rules.length; index++) {
            Guard guard = this.guards[index];
            if (guard != null && !guard.contains(blockY, above, below)) {
                continue;
            }

            BlockState state = this.rules[index].tryApply(blockX, blockY, blockZ);
            if (state != null) {
                return state;
            }
        }

        return null;
    }
}
