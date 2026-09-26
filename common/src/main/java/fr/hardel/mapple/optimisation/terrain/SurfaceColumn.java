package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The column of the surface pass on a proto chunk before light: ProtoChunk's own read and write, without its per call status and heightmap lookups.
 * A write reaches the section and the two worldgen heightmaps, as ProtoChunk's does at that status.
 */
final class SurfaceColumn implements BlockColumn {
    private final ProtoChunk chunk;
    private final int minY;
    private final int maxY;
    private final Heightmap oceanFloor;
    private final Heightmap worldSurface;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    SurfaceColumn(ProtoChunk chunk, int minY, int maxY) {
        this.chunk = chunk;
        this.minY = minY;
        this.maxY = maxY;
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
    }

    void moveTo(int blockX, int blockZ) {
        this.pos.setX(blockX).setZ(blockZ);
    }

    @Override
    public BlockState getBlock(int blockY) {
        if (this.chunk.isOutsideBuildHeight(blockY)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        LevelChunkSection section = this.chunk.getSection(this.chunk.getSectionIndex(blockY));
        return section.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : section.getBlockState(this.pos.getX() & 15, blockY & 15, this.pos.getZ() & 15);
    }

    @Override
    public void setBlock(int blockY, BlockState state) {
        if (blockY < this.minY || blockY > this.maxY) {
            return;
        }

        this.pos.setY(blockY);
        write(state);
        if (!state.getFluidState().isEmpty()) {
            this.chunk.markPosForPostProcessing(this.pos);
        }
    }

    private void write(BlockState state) {
        if (this.chunk.isOutsideBuildHeight(this.pos.getY())) {
            return;
        }

        LevelChunkSection section = this.chunk.getSection(this.chunk.getSectionIndex(this.pos.getY()));
        if (section.hasOnlyAir() && state.is(Blocks.AIR)) {
            return;
        }

        int localX = this.pos.getX() & 15;
        int localZ = this.pos.getZ() & 15;
        section.setBlockState(localX, this.pos.getY() & 15, localZ, state);
        this.oceanFloor.update(localX, this.pos.getY(), localZ, state);
        this.worldSurface.update(localX, this.pos.getY(), localZ, state);
    }
}
