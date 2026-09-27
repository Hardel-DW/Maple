package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;

/**
 * Vanilla's noise fill with every call to the noise chunk in vanilla's order: its caches, the aquifer and the ore veins depend on it. Blocks go to the
 * section builders instead of the chunk, and each worldgen heightmap follows its columns as vanilla's update on every block would.
 */
public final class NoiseFill {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ChunkAccess chunk;
    private final NoiseChunk noiseChunk;
    private final BlockState defaultBlock;
    private final Aquifer aquifer;
    private final ColumnHeightmap oceanFloor;
    private final ColumnHeightmap worldSurface;
    private final SectionBuilders sections;
    private final int cellWidth;
    private final int cellHeight;
    private final BlockPos.MutableBlockPos fluidPos = new BlockPos.MutableBlockPos();

    public NoiseFill(ChunkAccess chunk, NoiseChunk noiseChunk, BlockState defaultBlock) {
        this.chunk = chunk;
        this.noiseChunk = noiseChunk;
        this.defaultBlock = defaultBlock;
        this.aquifer = noiseChunk.aquifer();
        this.oceanFloor = new ColumnHeightmap(chunk, Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = new ColumnHeightmap(chunk, Heightmap.Types.WORLD_SURFACE_WG);
        this.sections = new SectionBuilders(chunk);
        this.cellWidth = noiseChunk.cellWidth();
        this.cellHeight = noiseChunk.cellHeight();
    }

    public void run(int cellMinY, int cellCountY) {
        this.noiseChunk.initializeForFirstCellX();
        int cellCount = 16 / this.cellWidth;
        for (int cellX = 0; cellX < cellCount; cellX++) {
            this.noiseChunk.advanceCellX(cellX);
            for (int cellZ = 0; cellZ < cellCount; cellZ++) {
                fillCellColumn(cellX, cellZ, cellMinY, cellCountY);
            }

            this.noiseChunk.swapSlices();
        }

        this.noiseChunk.stopInterpolation();
        this.sections.build();
    }

    /** The builder is looked up again only when the cells enter another section. */
    private void fillCellColumn(int cellX, int cellZ, int cellMinY, int cellCountY) {
        SectionBuilder builder = null;
        int builderSection = Integer.MIN_VALUE;
        for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
            this.noiseChunk.selectCellYZ(cellY, cellZ);
            for (int yInCell = this.cellHeight - 1; yInCell >= 0; yInCell--) {
                int posY = (cellMinY + cellY) * this.cellHeight + yInCell;
                if (SectionPos.blockToSectionCoord(posY) != builderSection) {
                    builderSection = SectionPos.blockToSectionCoord(posY);
                    builder = this.sections.at(posY);
                }

                this.noiseChunk.updateForY(posY, (double) yInCell / this.cellHeight);
                fillLayer(builder, cellX, cellZ, posY);
            }
        }
    }

    private void fillLayer(SectionBuilder builder, int cellX, int cellZ, int posY) {
        int minBlockX = this.chunk.getPos().getMinBlockX() + cellX * this.cellWidth;
        int minBlockZ = this.chunk.getPos().getMinBlockZ() + cellZ * this.cellWidth;
        for (int xInCell = 0; xInCell < this.cellWidth; xInCell++) {
            int posX = minBlockX + xInCell;
            this.noiseChunk.updateForX(posX, (double) xInCell / this.cellWidth);
            for (int zInCell = 0; zInCell < this.cellWidth; zInCell++) {
                int posZ = minBlockZ + zInCell;
                this.noiseChunk.updateForZ(posZ, (double) zInCell / this.cellWidth);
                BlockState computed = this.noiseChunk.getInterpolatedState();
                BlockState state = computed == null ? this.defaultBlock : computed;
                if (state != AIR) {
                    write(builder, posX, posY, posZ, state);
                }
            }
        }
    }

    private void write(SectionBuilder builder, int posX, int posY, int posZ, BlockState state) {
        int x = posX & 15;
        int z = posZ & 15;
        int index = builder.index(x, posY & 15, z);
        builder.set(index, state);
        this.oceanFloor.write(x, posY, z, state);
        this.worldSurface.write(x, posY, z, state);
        if (this.aquifer.shouldScheduleFluidUpdate() && builder.kind(index) == BlockKind.FLUID) {
            this.chunk.markPosForPostprocessing(this.fluidPos.set(posX, posY, posZ));
        }
    }
}
