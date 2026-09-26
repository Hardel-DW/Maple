package fr.hardel.mapple.optimisation.surface;

import fr.hardel.mapple.optimisation.terrain.BlockKind;
import fr.hardel.mapple.optimisation.terrain.SectionBuilders;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The column of the surface pass on a proto chunk before light, read and written through its section builders. A write that changes a block updates the
 * two worldgen heightmaps as ProtoChunk's does at that status, with vanilla's update reading the builders: the sections keep their old content until
 * the pass ends. Rewriting the same block leaves them as they are.
 */
final class SurfaceColumn implements BlockColumn {
    private final ProtoChunk chunk;
    private final SectionBuilders sections;
    private final int minY;
    private final int maxY;
    private final Heightmap oceanFloor;
    private final Heightmap worldSurface;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int localX;
    private int localZ;

    SurfaceColumn(ProtoChunk chunk, SectionBuilders sections, int minY, int maxY) {
        this.chunk = chunk;
        this.sections = sections;
        this.minY = minY;
        this.maxY = maxY;
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
    }

    void moveTo(int blockX, int blockZ) {
        this.pos.setX(blockX).setZ(blockZ);
        this.localX = blockX & 15;
        this.localZ = blockZ & 15;
    }

    @Override
    public BlockState getBlock(int blockY) {
        if (this.chunk.isOutsideBuildHeight(blockY)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        return this.sections.get(this.localX, blockY, this.localZ);
    }

    /** The kind of getBlock's state. */
    byte kind(int blockY) {
        return this.chunk.isOutsideBuildHeight(blockY) ? BlockKind.AIR : this.sections.kind(this.localX, blockY, this.localZ);
    }

    @Override
    public void setBlock(int blockY, BlockState state) {
        if (blockY < this.minY || blockY > this.maxY) {
            return;
        }

        if (!this.chunk.isOutsideBuildHeight(blockY) && this.sections.set(this.localX, blockY, this.localZ, state)) {
            update(this.oceanFloor, Heightmap.Types.OCEAN_FLOOR_WG.isOpaque(), blockY, state);
            update(this.worldSurface, Heightmap.Types.WORLD_SURFACE_WG.isOpaque(), blockY, state);
        }

        if (!state.getFluidState().isEmpty()) {
            this.chunk.markPosForPostProcessing(this.pos.setY(blockY));
        }
    }

    /** Heightmap.update, which returns at once below the top of the column. */
    private void update(Heightmap heightmap, Predicate<BlockState> opaque, int blockY, BlockState state) {
        int firstAvailable = heightmap.getFirstAvailable(this.localX, this.localZ);
        if (blockY <= firstAvailable - 2) {
            return;
        }

        if (opaque.test(state)) {
            if (blockY >= firstAvailable) {
                heightmap.setHeight(this.localX, this.localZ, blockY + 1);
            }

            return;
        }

        if (firstAvailable - 1 != blockY) {
            return;
        }

        for (int y = blockY - 1; y >= this.chunk.getMinY(); y--) {
            if (opaque.test(getBlock(y))) {
                heightmap.setHeight(this.localX, this.localZ, y + 1);
                return;
            }
        }

        heightmap.setHeight(this.localX, this.localZ, this.chunk.getMinY());
    }
}
