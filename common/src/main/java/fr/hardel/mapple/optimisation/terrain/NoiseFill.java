package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's noise fill in vanilla's order, the aquifer keeps state between calls. Blocks go to the section builders instead of the chunk, and each
 * worldgen heightmap follows the column as vanilla's update on every block would.
 */
public final class NoiseFill {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ChunkAccess chunk;
    private final NoiseChunk noiseChunk;
    private final BlockState defaultBlock;
    private final ColumnHeightmap oceanFloor;
    private final ColumnHeightmap worldSurface;
    private final SectionBuilders sections;
    private final BlockPos.MutableBlockPos fluidPos = new BlockPos.MutableBlockPos();
    private final Aquifer.@Nullable NoiseBasedAquifer noiseAquifer;
    private boolean fluidUpdate;

    public NoiseFill(ChunkAccess chunk, NoiseChunk noiseChunk, BlockState defaultBlock) {
        this.chunk = chunk;
        this.noiseChunk = noiseChunk;
        this.defaultBlock = defaultBlock;
        this.oceanFloor = new ColumnHeightmap(chunk, Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = new ColumnHeightmap(chunk, Heightmap.Types.WORLD_SURFACE_WG);
        this.sections = new SectionBuilders(chunk);
        this.noiseAquifer = noiseChunk.aquifer().getClass() == Aquifer.NoiseBasedAquifer.class ? (Aquifer.NoiseBasedAquifer) noiseChunk.aquifer() : null;
    }

    public void run(DensitySampler.Bound finalDensity) {
        DensityVolume volume = this.noiseChunk.volume();
        try (ScopedDensityBuffer density = finalDensity.sampleVolume(volume)) {
            for (int z = 0; z < volume.sizeZ(); z++) {
                for (int x = 0; x < volume.sizeX(); x++) {
                    fillColumn(density, volume, x, z);
                }
            }
        }

        this.sections.build();
    }

    /** The builder is looked up again only when the column enters another section. */
    private void fillColumn(ScopedDensityBuffer density, DensityVolume volume, int x, int z) {
        Aquifer aquifer = this.noiseChunk.aquifer();
        int blockX = volume.blockX(x);
        int blockZ = volume.blockZ(z);
        this.oceanFloor.moveTo(x, z);
        this.worldSurface.moveTo(x, z);
        SectionBuilder builder = null;
        int builderSection = Integer.MIN_VALUE;
        for (int y = volume.sizeY() - 1; y >= 0; y--) {
            int blockY = volume.blockY(y);
            BlockState computed = substance(aquifer, blockX, blockY, blockZ, density.get(volume.indexUnchecked(x, y, z)));
            BlockState state = computed == null ? this.defaultBlock : computed;
            if (state == AIR) {
                continue;
            }

            if (SectionPos.blockToSectionCoord(blockY) != builderSection) {
                builderSection = SectionPos.blockToSectionCoord(blockY);
                builder = this.sections.at(blockY);
            }

            int index = builder.index(x, SectionPos.sectionRelative(blockY), z);
            builder.set(index, state);
            this.oceanFloor.write(blockY, state);
            this.worldSurface.write(blockY, state);
            if (builder.kind(index) == BlockKind.FLUID && this.fluidUpdate) {
                this.chunk.markPosForPostProcessing(this.fluidPos.set(blockX, blockY, blockZ));
            }
        }
    }

    /**
     * The aquifer's substance and its fluid update flag. The noise based aquifer's two first branches are taken here, as each still costs a call there:
     * a solid block, then a block above its sampled height, which takes the global fluid. Neither schedules a fluid update.
     */
    private @Nullable BlockState substance(Aquifer aquifer, int blockX, int blockY, int blockZ, double density) {
        if (this.noiseAquifer == null || !(density > 0.0) && blockY <= this.noiseAquifer.skipSamplingAboveY) {
            BlockState computed = aquifer.computeSubstance(blockX, blockY, blockZ, density);
            this.fluidUpdate = aquifer.shouldScheduleFluidUpdate();
            return computed;
        }

        this.fluidUpdate = false;
        return density > 0.0 ? null : this.noiseAquifer.globalFluidPicker.computeFluid(blockX, blockY, blockZ).at(blockY);
    }
}
