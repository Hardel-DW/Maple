package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;

/**
 * Vanilla's noise fill in vanilla's order, the aquifer keeps state between calls. Blocks go to section builders instead of the chunk, and a heightmap is
 * updated until the first block of the column its predicate accepts: every lower block returns early from update.
 */
public final class NoiseFill {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ChunkAccess chunk;
    private final NoiseChunk noiseChunk;
    private final BlockState defaultBlock;
    private final Heightmap oceanFloor;
    private final Heightmap worldSurface;
    private final SectionBuilder[] builders;
    private final BlockPos.MutableBlockPos fluidPos = new BlockPos.MutableBlockPos();

    public NoiseFill(ChunkAccess chunk, NoiseChunk noiseChunk, BlockState defaultBlock) {
        this.chunk = chunk;
        this.noiseChunk = noiseChunk;
        this.defaultBlock = defaultBlock;
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        this.builders = new SectionBuilder[chunk.getSectionsCount()];
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

        LevelChunkSection[] sections = this.chunk.getSections();
        for (int index = 0; index < this.builders.length; index++) {
            if (this.builders[index] != null) {
                sections[index] = this.builders[index].build();
            }
        }
    }

    private void fillColumn(ScopedDensityBuffer density, DensityVolume volume, int x, int z) {
        Aquifer aquifer = this.noiseChunk.aquifer();
        int blockX = volume.blockX(x);
        int blockZ = volume.blockZ(z);
        boolean oceanFloorSettled = false;
        boolean worldSurfaceSettled = false;
        for (int y = volume.sizeY() - 1; y >= 0; y--) {
            int blockY = volume.blockY(y);
            BlockState computed = aquifer.computeSubstance(blockX, blockY, blockZ, density.get(volume.indexUnchecked(x, y, z)));
            BlockState state = computed == null ? this.defaultBlock : computed;
            if (state == AIR) {
                continue;
            }

            builder(blockY).set(x, SectionPos.sectionRelative(blockY), z, state);
            oceanFloorSettled = oceanFloorSettled || settle(this.oceanFloor, Heightmap.Types.OCEAN_FLOOR_WG, x, blockY, z, state);
            worldSurfaceSettled = worldSurfaceSettled || settle(this.worldSurface, Heightmap.Types.WORLD_SURFACE_WG, x, blockY, z, state);
            if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                this.chunk.markPosForPostProcessing(this.fluidPos.set(blockX, blockY, blockZ));
            }
        }
    }

    private SectionBuilder builder(int blockY) {
        int index = this.chunk.getSectionIndex(blockY);
        SectionBuilder builder = this.builders[index];
        if (builder == null) {
            builder = new SectionBuilder(this.chunk.getSection(index));
            this.builders[index] = builder;
        }

        return builder;
    }

    private static boolean settle(Heightmap heightmap, Heightmap.Types type, int x, int blockY, int z, BlockState state) {
        heightmap.update(x, blockY, z, state);
        return type.isOpaque().test(state);
    }
}
