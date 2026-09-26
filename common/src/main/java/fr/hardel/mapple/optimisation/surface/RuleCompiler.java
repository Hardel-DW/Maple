package fr.hardel.mapple.optimisation.surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.util.Interval;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.AbovePreliminarySurfaceCondition;
import net.minecraft.world.level.levelgen.material.condition.BiomeCondition;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.HoleCondition;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.condition.NoiseThresholdCondition;
import net.minecraft.world.level.levelgen.material.condition.NotCondition;
import net.minecraft.world.level.levelgen.material.condition.SteepCondition;
import net.minecraft.world.level.levelgen.material.condition.StoneDepthCondition;
import net.minecraft.world.level.levelgen.material.condition.TemperatureCondition;
import net.minecraft.world.level.levelgen.material.condition.VerticalGradientCondition;
import net.minecraft.world.level.levelgen.material.condition.WaterCondition;
import net.minecraft.world.level.levelgen.material.condition.YCondition;
import net.minecraft.world.level.levelgen.material.rule.BandlandsRule;
import net.minecraft.world.level.levelgen.material.rule.BlockRule;
import net.minecraft.world.level.levelgen.material.rule.ConditionRule;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.jspecify.annotations.Nullable;

/**
 * Compiles the rules of one chunk, each vanilla type knowing the bounds of the blocks it can fire on. Sequences are flattened and guard their entries,
 * branches that can never fire are left out, and a sequence ends at an entry that always fires. Conditions decided by their bounds need no evaluator,
 * the others keep vanilla's. Unknown types compile and run by their own code, with no bounds.
 */
final class RuleCompiler {
    private static final CompiledCondition TRUE = new CompiledCondition(Bounds.ALL, Bounds.ALL, null);
    private static final CompiledCondition FALSE = new CompiledCondition(Bounds.NONE, Bounds.NONE, null);
    private static final CompiledRule NEVER = new CompiledRule((blockX, blockY, blockZ) -> null, Bounds.NONE, false);
    private static final Set<Class<? extends MaterialCondition>> SHARED = Set.of(YCondition.class, AbovePreliminarySurfaceCondition.class,
        StoneDepthCondition.class, VerticalGradientCondition.class, BiomeCondition.class, WaterCondition.class, NoiseThresholdCondition.class,
        TemperatureCondition.class, HoleCondition.class, SteepCondition.class);

    private final MaterialRuleContext context;
    private final DensitySamplerSet samplers;
    private final DensityVolume volume;
    private final int minY;
    private final Interval secondary;
    private final IntSupplier surfaceDepth;
    private final IntSupplier minSurfaceLevel;
    private final Map<MaterialCondition, CompiledCondition> conditions = new HashMap<>();
    private final Map<CompiledCondition, ConditionEvaluator> evaluators = new IdentityHashMap<>();
    private final List<Guard> columnGuards = new ArrayList<>();
    private final List<OreVein> veins = new ArrayList<>();

    RuleCompiler(MaterialRuleContext context, RandomState randomState, DensitySamplerSet samplers, DensityVolume volume, int minY) {
        this.context = context;
        this.samplers = samplers;
        this.volume = volume;
        this.minY = minY;
        this.secondary = randomState.getOrCreateNoise(Noises.SURFACE_SECONDARY).range();
        this.surfaceDepth = context::surfaceDepth;
        this.minSurfaceLevel = context::getMinSurfaceLevel;
    }

    SurfaceRules compile(MaterialRule root) {
        RuleEvaluator evaluator = rule(root).evaluator();
        return new SurfaceRules(evaluator, this.columnGuards.toArray(Guard[]::new), this.veins.toArray(OreVein[]::new));
    }

    private CompiledRule rule(MaterialRule rule) {
        return switch (rule) {
            case MaterialRule.HolderHolder holder -> rule(holder.holder().value());
            case BlockRule block -> new CompiledRule(block, Bounds.ALL, true);
            case BandlandsRule bandlands -> new CompiledRule(bandlands.compile(this.context), Bounds.ALL, true);
            case SequenceRule sequence -> sequence(sequence);
            case ConditionRule condition -> condition(condition);
            case OreVeinRule vein -> oreVein(vein);
            default -> new CompiledRule(rule.compile(this.context), Bounds.ALL, false);
        };
    }

    private CompiledRule sequence(SequenceRule sequence) {
        List<CompiledRule> entries = new ArrayList<>();
        flatten(sequence, entries);
        if (entries.size() <= 1) {
            return entries.isEmpty() ? NEVER : entries.getFirst();
        }

        Bounds possible = Bounds.NONE;
        RuleEvaluator[] rules = new RuleEvaluator[entries.size()];
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
    private boolean flatten(SequenceRule sequence, List<CompiledRule> entries) {
        for (MaterialRule entry : sequence.sequence()) {
            MaterialRule unwrapped = unwrap(entry);
            if (unwrapped instanceof SequenceRule nested) {
                if (flatten(nested, entries)) {
                    return true;
                }

                continue;
            }

            CompiledRule compiled = rule(unwrapped);
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

    private CompiledRule condition(ConditionRule rule) {
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

        ConditionEvaluator test = this.evaluators.computeIfAbsent(condition, this::evaluator);
        RuleEvaluator next = then.evaluator();
        return new CompiledRule((blockX, blockY, blockZ) -> test.test() ? next.tryApply(blockX, blockY, blockZ) : null, possible, false);
    }

    private CompiledRule oreVein(OreVeinRule rule) {
        if (SharedConstants.DEBUG_ORE_VEINS || SharedConstants.DEBUG_DISABLE_ORE_VEINS) {
            return new CompiledRule(rule.compile(this.context), Bounds.ALL, false);
        }

        OreVein vein = new OreVein(rule, this.context, this.samplers, this.volume, this.minY);
        this.veins.add(vein);
        Bounds possible = Bounds.atLeast(Bounds.Y, Limit.column(0, 1, vein::low)).meet(Bounds.atMost(Bounds.Y, Limit.column(0, 1, vein::high)));
        return new CompiledRule(vein, possible, false);
    }

    private CompiledCondition condition(MaterialCondition condition) {
        MaterialCondition unwrapped = unwrap(condition);
        if (unwrapped instanceof NotCondition not) {
            return not(condition(not.target()));
        }

        if (!SHARED.contains(unwrapped.getClass())) {
            return opaque(unwrapped);
        }

        CompiledCondition compiled = this.conditions.get(unwrapped);
        if (compiled == null) {
            compiled = leaf(unwrapped);
            this.conditions.put(unwrapped, compiled);
        }

        return compiled;
    }

    private CompiledCondition leaf(MaterialCondition condition) {
        return switch (condition) {
            case YCondition y when !y.addStoneDepth() -> exact(Bounds.atLeast(Bounds.Y,
                Limit.column(this.context.resolveAnchorY(y.anchor()), y.surfaceDepthMultiplier(), this.surfaceDepth)));
            case AbovePreliminarySurfaceCondition _ -> exact(Bounds.atLeast(Bounds.Y, Limit.column(0, 1, this.minSurfaceLevel)));
            case StoneDepthCondition depth -> stoneDepth(depth);
            case VerticalGradientCondition gradient -> gradient(gradient);
            case BiomeCondition biome -> biome(biome);
            default -> opaque(condition);
        };
    }

    /** Vanilla's depth limit plus a secondary part within the range of its noise, widened by one for rounding. */
    private CompiledCondition stoneDepth(StoneDepthCondition condition) {
        int axis = condition.surfaceType() == CaveSurface.CEILING ? Bounds.BELOW : Bounds.ABOVE;
        Limit depth = Limit.column(1 + condition.offset(), condition.addSurfaceDepth() ? 1 : 0, this.surfaceDepth);
        if (condition.secondaryDepthRange() == 0) {
            return exact(Bounds.atMost(axis, depth));
        }

        if (!Float.isFinite(this.secondary.min()) || !Float.isFinite(this.secondary.max())) {
            return opaque(condition);
        }

        int first = (int) Mth.map(this.secondary.min(), -1.0, 1.0, 0.0, condition.secondaryDepthRange());
        int second = (int) Mth.map(this.secondary.max(), -1.0, 1.0, 0.0, condition.secondaryDepthRange());
        Bounds possible = Bounds.atMost(axis, depth.shift(Math.max(first, second) + 1));
        Bounds certain = Bounds.atMost(axis, depth.shift(Math.min(first, second) - 1));
        return new CompiledCondition(possible, certain, condition.compile(this.context));
    }

    /** True at and below one y, false at and above another, random between. */
    private CompiledCondition gradient(VerticalGradientCondition condition) {
        int trueAtAndBelow = this.context.resolveAnchorY(condition.trueAtAndBelow());
        int highest = Math.max(trueAtAndBelow, this.context.resolveAnchorY(condition.falseAtAndAbove()) - 1);
        Bounds certain = Bounds.atMost(Bounds.Y, Limit.constant(trueAtAndBelow));
        if (highest == trueAtAndBelow) {
            return exact(certain);
        }

        return new CompiledCondition(Bounds.atMost(Bounds.Y, Limit.constant(highest)), certain, condition.compile(this.context));
    }

    /** Vanilla's own folding on the biomes the chunk can hold. */
    private CompiledCondition biome(BiomeCondition condition) {
        Set<Holder<Biome>> possibleBiomes = this.context.possibleBiomes();
        if (possibleBiomes == null) {
            return opaque(condition);
        }

        if (condition.biomes().stream().noneMatch(possibleBiomes::contains)) {
            return FALSE;
        }

        if (possibleBiomes.stream().allMatch(condition.biomes()::contains)) {
            return TRUE;
        }

        return opaque(condition);
    }

    /** The complement of an exact condition limited on one side stays exact, any other is decided between its bounds by negating it. */
    private CompiledCondition not(CompiledCondition condition) {
        Bounds possible = condition.certain().complement(true);
        Bounds certain = condition.possible().complement(false);
        if (condition.evaluator() == null && possible.equals(certain)) {
            return new CompiledCondition(possible, certain, null);
        }

        ConditionEvaluator target = this.evaluators.computeIfAbsent(condition, this::evaluator);
        return new CompiledCondition(possible, certain, () -> !target.test());
    }

    private ConditionEvaluator evaluator(CompiledCondition condition) {
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

    private CompiledCondition opaque(MaterialCondition condition) {
        return new CompiledCondition(Bounds.ALL, Bounds.NONE, condition.compile(this.context));
    }

    private static CompiledCondition exact(Bounds bounds) {
        return new CompiledCondition(bounds, bounds, null);
    }

    private static MaterialRule unwrap(MaterialRule rule) {
        return rule instanceof MaterialRule.HolderHolder holder ? unwrap(holder.holder().value()) : rule;
    }

    private static MaterialCondition unwrap(MaterialCondition condition) {
        return condition instanceof MaterialCondition.HolderHolder holder ? unwrap(holder.holder().value()) : condition;
    }

    /** Where a rule can fire, and whether it fires on every block it sees. */
    private record CompiledRule(RuleEvaluator evaluator, Bounds possible, boolean always) {}

    /** Where a condition can hold and where it surely holds; the evaluator decides between, none when both are the same. */
    private record CompiledCondition(Bounds possible, Bounds certain, @Nullable ConditionEvaluator evaluator) {}
}
