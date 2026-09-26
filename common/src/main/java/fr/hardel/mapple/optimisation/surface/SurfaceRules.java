package fr.hardel.mapple.optimisation.surface;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import org.jspecify.annotations.Nullable;

/** The compiled rules of one chunk. Each column first sets the ranges of its veins, then resolves the guards reading the column. */
final class SurfaceRules {
    private final RuleEvaluator root;
    private final Guard[] columnGuards;
    private final OreVein[] veins;

    SurfaceRules(RuleEvaluator root, Guard[] columnGuards, OreVein[] veins) {
        this.root = root;
        this.columnGuards = columnGuards;
        this.veins = veins;
    }

    /** After the context moved to the column, topY being the highest block the rules will see. */
    void moveTo(int blockX, int blockZ, int topY) {
        for (OreVein vein : this.veins) {
            vein.moveTo(blockX, blockZ, topY);
        }

        for (Guard guard : this.columnGuards) {
            guard.resolve();
        }
    }

    @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
        return this.root.tryApply(blockX, blockY, blockZ);
    }
}
