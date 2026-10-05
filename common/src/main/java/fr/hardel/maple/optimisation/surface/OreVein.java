package fr.hardel.maple.optimisation.surface;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's ore vein evaluator, which returns nothing where its density is not positive. The prefilled density gives each column the lowest and highest
 * y it is positive at, the range the vein can fire in. Outside the volume vanilla samples the density directly: a column holding stone out of the
 * volume opens its range on that side.
 */
final class OreVein implements RuleEvaluator, AutoCloseable {
    private final OreVeinRule rule;
    private final DensityVolume volume;
    private final ScopedDensityBuffer density;
    private final DensitySampler.Bound densitySampler;
    private final DensitySampler.Bound richnessSampler;
    private final DensitySampler.Bound fillerGapSampler;
    private final PositionalRandomFactory randomFactory;
    private final int[] lowest;
    private final int[] highest;
    private final boolean openBelow;
    private @Nullable ScopedDensityBuffer richness;
    private int low;
    private int high;

    OreVein(OreVeinRule rule, MaterialRuleContext context, DensitySamplerSet samplers, DensityVolume volume, int minY) {
        this.rule = rule;
        this.volume = volume;
        this.densitySampler = samplers.get(rule.density());
        this.richnessSampler = samplers.get(rule.richness());
        this.fillerGapSampler = samplers.get(rule.fillerGap());
        this.randomFactory = context.getOrCreateRandomFactory(Identifier.withDefaultNamespace("ore"));
        this.density = this.densitySampler.sampleVolume(volume);
        this.lowest = new int[volume.sizeX() * volume.sizeZ()];
        this.highest = new int[this.lowest.length];
        this.openBelow = minY < volume.minBlockY();
        scan();
    }

    int low() {
        return this.low;
    }

    int high() {
        return this.high;
    }

    void moveTo(int blockX, int blockZ, int topY) {
        int column = blockX - this.volume.minBlockX() + (blockZ - this.volume.minBlockZ()) * this.volume.sizeX();
        this.low = this.openBelow ? Integer.MIN_VALUE : this.lowest[column];
        this.high = topY > this.volume.maxBlockY() ? Integer.MAX_VALUE : this.highest[column];
    }

    @Override
    public @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
        int index = this.volume.indexOfBlock(blockX, blockY, blockZ);
        float density = index == DensityVolume.NO_BLOCK ? this.densitySampler.sampleValue(blockX, blockY, blockZ) : this.density.get(index);
        if (density <= 0.0F) {
            return null;
        }

        RandomSource random = this.randomFactory.at(blockX, blockY, blockZ);
        if (random.nextFloat() > density) {
            return null;
        }

        if (random.nextFloat() < richness(index, blockX, blockY, blockZ) && this.fillerGapSampler.sampleValue(blockX, blockY, blockZ) < 0.0F) {
            return random.nextFloat() < this.rule.rawOreChance() ? this.rule.rawOreBlock() : this.rule.oreBlock();
        }

        return this.rule.fillerBlock();
    }

    /** Prefilled on the first ore candidate, as vanilla's prefill gives the same values at any time. */
    private float richness(int index, int blockX, int blockY, int blockZ) {
        if (index == DensityVolume.NO_BLOCK) {
            return this.richnessSampler.sampleValue(blockX, blockY, blockZ);
        }

        if (this.richness == null) {
            this.richness = this.richnessSampler.sampleVolume(this.volume);
        }

        return this.richness.get(index);
    }

    /** The buffers go back to the pool of the noise chunk, as vanilla's own do. */
    @Override
    public void close() {
        this.density.close();
        if (this.richness != null) {
            this.richness.close();
        }
    }

    private void scan() {
        int sizeY = this.volume.sizeY();
        for (int column = 0; column < this.lowest.length; column++) {
            int start = column * sizeY;
            int first = sizeY;
            int last = -1;
            for (int y = 0; y < sizeY; y++) {
                if (!(this.density.get(start + y) <= 0.0F)) {
                    first = Math.min(first, y);
                    last = y;
                }
            }

            this.lowest[column] = this.volume.blockY(first);
            this.highest[column] = this.volume.blockY(last);
        }
    }
}
