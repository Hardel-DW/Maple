package fr.hardel.mapple.optimisation.terrain;

import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A worldgen heightmap along the columns of a chunk, each written top-down, left as vanilla's update on every block leaves it. A rejected block only acts
 * at the first available height minus one; after an accepted block every lower one returns early. Columns may interleave, each keeps its own state.
 * The predicate is kept for the last state seen.
 */
final class ColumnHeightmap {
    private final Heightmap heightmap;
    private final Predicate<BlockState> opaque;
    private final int[] firstAvailable = new int[256];
    private final boolean[] settled = new boolean[256];
    private @Nullable BlockState lastState;
    private boolean lastOpaque;

    ColumnHeightmap(ChunkAccess chunk, Heightmap.Types type) {
        this.heightmap = chunk.getOrCreateHeightmapUnprimed(type);
        this.opaque = type.isOpaque();
        for (int column = 0; column < 256; column++) {
            this.firstAvailable[column] = this.heightmap.getFirstAvailable(column & 15, column >> 4);
        }
    }

    /** x and z inside the chunk. */
    void write(int x, int blockY, int z, BlockState state) {
        int column = x | z << 4;
        if (this.settled[column]) {
            return;
        }

        if (opaque(state)) {
            this.heightmap.update(x, blockY, z, state);
            this.settled[column] = true;
            return;
        }

        if (blockY == this.firstAvailable[column] - 1) {
            this.heightmap.update(x, blockY, z, state);
            this.firstAvailable[column] = this.heightmap.getFirstAvailable(x, z);
        }
    }

    private boolean opaque(BlockState state) {
        if (state != this.lastState) {
            this.lastState = state;
            this.lastOpaque = this.opaque.test(state);
        }

        return this.lastOpaque;
    }
}
