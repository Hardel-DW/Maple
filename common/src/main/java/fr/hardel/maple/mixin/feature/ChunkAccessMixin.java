package fr.hardel.maple.mixin.feature;

import fr.hardel.maple.optimisation.feature.OceanFloorPeak;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** From TERRAIN on a proto chunk's writes update the final heightmaps only: its worldgen ones are frozen, so their peak is read once. */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements OceanFloorPeak {
    @Unique
    private int maple$oceanFloorPeak = Integer.MIN_VALUE;

    @Override
    public int maple$oceanFloorPeak() {
        if (!((Object) this instanceof ProtoChunk proto) || proto.getClass() != ProtoChunk.class || !proto.getPersistedStatus().isOrAfter(ChunkStatus.TERRAIN)) {
            return Integer.MAX_VALUE;
        }

        if (this.maple$oceanFloorPeak == Integer.MIN_VALUE) {
            int peak = Integer.MIN_VALUE;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    peak = Math.max(peak, proto.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) + 1);
                }
            }

            this.maple$oceanFloorPeak = peak;
        }

        return this.maple$oceanFloorPeak;
    }
}
