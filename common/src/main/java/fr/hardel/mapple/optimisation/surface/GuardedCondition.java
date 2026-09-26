package fr.hardel.mapple.optimisation.surface;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import org.jspecify.annotations.Nullable;

/**
 * A condition false outside its possible guard and true inside its certain one. Between both its vanilla evaluator decides; without one the two guards
 * are the same and the possible guard alone decides.
 */
final class GuardedCondition implements ConditionEvaluator {
    private final MaterialRuleContext context;
    private final Guard possible;
    private final @Nullable Guard certain;
    private final @Nullable ConditionEvaluator evaluator;

    GuardedCondition(MaterialRuleContext context, Guard possible, @Nullable Guard certain, @Nullable ConditionEvaluator evaluator) {
        this.context = context;
        this.possible = possible;
        this.certain = certain;
        this.evaluator = evaluator;
    }

    @Override
    public boolean test() {
        int y = this.context.blockY();
        int above = this.context.stoneDepthAbove();
        int below = this.context.stoneDepthBelow();
        if (!this.possible.contains(y, above, below)) {
            return false;
        }

        if (this.evaluator == null || this.certain != null && this.certain.contains(y, above, below)) {
            return true;
        }

        return this.evaluator.test();
    }
}
