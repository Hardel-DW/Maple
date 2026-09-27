package fr.hardel.mapple.optimisation.surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.jspecify.annotations.Nullable;

/**
 * Compiles the rules of one chunk, each vanilla type knowing the bounds of the blocks it can fire on. Sequences are flattened and guard their entries,
 * branches that can never fire are left out, and a sequence ends at an entry that always fires. Conditions decided by their bounds need no evaluator,
 * the others keep vanilla's. Unknown types compile and run by their own code, with no bounds.
 */
final class RuleCompiler {
    private static final CompiledRule NEVER = new CompiledRule((blockX, blockY, blockZ) -> null, Bounds.NONE, false);
    private static final Set<Class<? extends SurfaceRules.ConditionSource>> SHARED = Set.of(SurfaceRules.YConditionSource.class,
        SurfaceRules.AbovePreliminarySurface.class, SurfaceRules.StoneDepthCheck.class, SurfaceRules.VerticalGradientConditionSource.class,
        SurfaceRules.BiomeConditionSource.class, SurfaceRules.WaterConditionSource.class, SurfaceRules.NoiseThresholdConditionSource.class,
        SurfaceRules.Temperature.class, SurfaceRules.Hole.class, SurfaceRules.Steep.class);

    private final SurfaceRules.Context context;
    private final double secondary;
    private final IntSupplier surfaceDepth;
    private final IntSupplier minSurfaceLevel;
    private final Map<SurfaceRules.ConditionSource, CompiledCondition> conditions = new HashMap<>();
    private final Map<CompiledCondition, SurfaceRules.Condition> evaluators = new IdentityHashMap<>();
    private final List<Guard> columnGuards = new ArrayList<>();

    RuleCompiler(SurfaceRules.Context context, RandomState randomState) {
        this.context = context;
        this.secondary = randomState.getOrCreateNoise(Noises.SURFACE_SECONDARY).maxValue();
        this.surfaceDepth = () -> context.surfaceDepth;
        this.minSurfaceLevel = context::getMinSurfaceLevel;
    }

    CompiledRules compile(SurfaceRules.RuleSource root) {
        SurfaceRules.SurfaceRule evaluator = rule(root).evaluator();
        return new CompiledRules(evaluator, this.columnGuards.toArray(Guard[]::new));
    }

    private CompiledRule rule(SurfaceRules.RuleSource rule) {
        return switch (rule) {
            case SurfaceRules.BlockRuleSource block -> new CompiledRule(block.rule(), Bounds.ALL, true);
            case SurfaceRules.Bandlands bandlands -> new CompiledRule(bandlands.apply(this.context), Bounds.ALL, true);
            case SurfaceRules.SequenceRuleSource sequence -> sequence(sequence);
            case SurfaceRules.TestRuleSource condition -> condition(condition);
            default -> new CompiledRule(rule.apply(this.context), Bounds.ALL, false);
        };
    }

    private CompiledRule sequence(SurfaceRules.SequenceRuleSource sequence) {
        List<CompiledRule> entries = new ArrayList<>();
        flatten(sequence, entries);
        if (entries.size() <= 1) {
            return entries.isEmpty() ? NEVER : entries.getFirst();
        }

        Bounds possible = Bounds.NONE;
        SurfaceRules.SurfaceRule[] rules = new SurfaceRules.SurfaceRule[entries.size()];
        Guard[] guards = new Guard[entries.size()];
        for (int index = 0; index < rules.length; index++) {
            CompiledRule entry = entries.get(index);
            possible = possible.hull(entry.possible());
            rules[index] = entry.evaluator();
            guards[index] = entry.possible().isAll() ? null : guard(entry.possible());
        }

        return new CompiledRule(new GuardedSequence(this.context, rules, guards), possible, entries.getLast().always());
    }

    /** Appends the entries of a sequence, nested sequences inline, and tells whether the last one always fires: nothing after it is reached. */
    private boolean flatten(SurfaceRules.SequenceRuleSource sequence, List<CompiledRule> entries) {
        for (SurfaceRules.RuleSource entry : sequence.sequence()) {
            if (entry instanceof SurfaceRules.SequenceRuleSource nested) {
                if (flatten(nested, entries)) {
                    return true;
                }

                continue;
            }

            CompiledRule compiled = rule(entry);
            if (compiled.possible().isNone()) {
                continue;
            }

            entries.add(compiled);
            if (compiled.always()) {
                return true;
            }
        }

        return false;
    }

    private CompiledRule condition(SurfaceRules.TestRuleSource rule) {
        CompiledCondition condition = condition(rule.ifTrue());
        if (condition.possible().isNone()) {
            return NEVER;
        }

        CompiledRule then = rule(rule.thenRun());
        Bounds possible = condition.possible().meet(then.possible());
        if (possible.isNone()) {
            return NEVER;
        }

        if (condition.certain().isAll()) {
            return then;
        }

        SurfaceRules.Condition test = this.evaluators.computeIfAbsent(condition, this::evaluator);
        SurfaceRules.SurfaceRule next = then.evaluator();
        return new CompiledRule((blockX, blockY, blockZ) -> test.test() ? next.tryApply(blockX, blockY, blockZ) : null, possible, false);
    }

    private CompiledCondition condition(SurfaceRules.ConditionSource condition) {
        if (condition instanceof SurfaceRules.NotConditionSource not) {
            return not(condition(not.target()));
        }

        if (!SHARED.contains(condition.getClass())) {
            return opaque(condition);
        }

        CompiledCondition compiled = this.conditions.get(condition);
        if (compiled == null) {
            compiled = leaf(condition);
            this.conditions.put(condition, compiled);
        }

        return compiled;
    }

    private CompiledCondition leaf(SurfaceRules.ConditionSource condition) {
        return switch (condition) {
            case SurfaceRules.YConditionSource y when !y.addStoneDepth() -> exact(Bounds.atLeast(Bounds.Y,
                Limit.column(y.anchor().resolveY(this.context.context), y.surfaceDepthMultiplier(), this.surfaceDepth)));
            case SurfaceRules.AbovePreliminarySurface _ -> exact(Bounds.atLeast(Bounds.Y, Limit.column(0, 1, this.minSurfaceLevel)));
            case SurfaceRules.StoneDepthCheck depth -> stoneDepth(depth);
            case SurfaceRules.VerticalGradientConditionSource gradient -> gradient(gradient);
            default -> opaque(condition);
        };
    }

    /** Vanilla's depth limit plus a secondary part within the range of its noise, widened by one for rounding. */
    private CompiledCondition stoneDepth(SurfaceRules.StoneDepthCheck condition) {
        int axis = condition.surfaceType() == CaveSurface.CEILING ? Bounds.BELOW : Bounds.ABOVE;
        Limit depth = Limit.column(1 + condition.offset(), condition.addSurfaceDepth() ? 1 : 0, this.surfaceDepth);
        if (condition.secondaryDepthRange() == 0) {
            return exact(Bounds.atMost(axis, depth));
        }

        int first = (int) Mth.map(-this.secondary, -1.0, 1.0, 0.0, condition.secondaryDepthRange());
        int second = (int) Mth.map(this.secondary, -1.0, 1.0, 0.0, condition.secondaryDepthRange());
        Bounds possible = Bounds.atMost(axis, depth.shift(Math.max(first, second) + 1));
        Bounds certain = Bounds.atMost(axis, depth.shift(Math.min(first, second) - 1));
        return new CompiledCondition(possible, certain, condition.apply(this.context));
    }

    /** True at and below one y, false at and above another, random between. */
    private CompiledCondition gradient(SurfaceRules.VerticalGradientConditionSource condition) {
        int trueAtAndBelow = condition.trueAtAndBelow().resolveY(this.context.context);
        int highest = Math.max(trueAtAndBelow, condition.falseAtAndAbove().resolveY(this.context.context) - 1);
        Bounds certain = Bounds.atMost(Bounds.Y, Limit.constant(trueAtAndBelow));
        if (highest == trueAtAndBelow) {
            return exact(certain);
        }

        return new CompiledCondition(Bounds.atMost(Bounds.Y, Limit.constant(highest)), certain, condition.apply(this.context));
    }

    /** The complement of an exact condition limited on one side stays exact, any other is decided between its bounds by negating it. */
    private CompiledCondition not(CompiledCondition condition) {
        Bounds possible = condition.certain().complement(true);
        Bounds certain = condition.possible().complement(false);
        if (condition.evaluator() == null && possible.equals(certain)) {
            return new CompiledCondition(possible, certain, null);
        }

        SurfaceRules.Condition target = this.evaluators.computeIfAbsent(condition, this::evaluator);
        return new CompiledCondition(possible, certain, () -> !target.test());
    }

    private SurfaceRules.Condition evaluator(CompiledCondition condition) {
        if (condition.evaluator() != null && condition.possible().isAll() && condition.certain().isNone()) {
            return condition.evaluator();
        }

        Guard certain = condition.evaluator() == null || condition.certain().isNone() ? null : guard(condition.certain());
        return new GuardedCondition(this.context, guard(condition.possible()), certain, condition.evaluator());
    }

    private Guard guard(Bounds bounds) {
        Guard guard = new Guard(bounds);
        if (bounds.readsColumn()) {
            this.columnGuards.add(guard);
        } else {
            guard.resolve();
        }

        return guard;
    }

    private CompiledCondition opaque(SurfaceRules.ConditionSource condition) {
        return new CompiledCondition(Bounds.ALL, Bounds.NONE, condition.apply(this.context));
    }

    private static CompiledCondition exact(Bounds bounds) {
        return new CompiledCondition(bounds, bounds, null);
    }

    /** Where a rule can fire, and whether it fires on every block it sees. */
    private record CompiledRule(SurfaceRules.SurfaceRule evaluator, Bounds possible, boolean always) {}

    /** Where a condition can hold and where it surely holds; the evaluator decides between, none when both are the same. */
    private record CompiledCondition(Bounds possible, Bounds certain, SurfaceRules.@Nullable Condition evaluator) {}
}
