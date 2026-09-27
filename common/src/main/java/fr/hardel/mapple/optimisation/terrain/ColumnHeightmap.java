package fr.hardel.mapple.optimisation.terrain;

import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A worldgen heightmap along a column written top-down, left as vanilla's update on every block leaves it. A rejected block only acts at the first
 * available height minus one; after an accepted block every lower one returns early. The predicate is kept for the last state seen.
 */
final class ColumnHeightmap {
    private final Heightmap heightmap;
    private final Predicate<BlockState> opaque;
    private @Nullable BlockState lastState;
    private boolean lastOpaque;
    private int x;
    private int z;
    private int firstAvailable;
    private boolean settled;

    ColumnHeightmap(ChunkAccess chunk, Heightmap.Types type) {
        this.heightmap = chunk.getOrCreateHeightmapUnprimed(type);
        this.opaque = type.isOpaque();
    }

    void moveTo(int x, int z) {
        this.x = x;
        this.z = z;
        this.firstAvailable = this.heightmap.getFirstAvailable(x, z);
        this.settled = false;
    }

    void write(int blockY, BlockState state) {
        if (this.settled) {
            return;
        }

        if (opaque(state)) {
            this.heightmap.update(this.x, blockY, this.z, state);
            this.settled = true;
            return;
        }

        if (blockY == this.firstAvailable - 1) {
            this.heightmap.update(this.x, blockY, this.z, state);
            this.firstAvailable = this.heightmap.getFirstAvailable(this.x, this.z);
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
