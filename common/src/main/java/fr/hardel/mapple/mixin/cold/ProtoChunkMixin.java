package fr.hardel.mapple.mixin.cold;

import fr.hardel.mapple.optimisation.cold.ColdChunk;
import fr.hardel.mapple.optimisation.cold.ColdStorage;
import fr.hardel.mapple.optimisation.cold.ColdStorageHolder;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** The cold state of a proto chunk: one block of bytes, sentinels in the slots, and the chunk monitor around every change of state. */
@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin implements ColdChunk {
    @Unique
    private volatile byte[] mapple$cold;

    @Override
    public void mapple$freeze() {
        synchronized (this) {
            if (this.mapple$cold != null) {
                return;
            }

            ColdStorage storage = mapple$storage();
            byte[] cold = storage.freeze(self().sections);
            for (int index = 0; index < self().sections.length; index++) {
                self().sections[index] = storage.sentinel(this, index);
            }

            this.mapple$cold = cold;
        }
    }

    @Override
    public void mapple$thaw() {
        synchronized (this) {
            byte[] cold = this.mapple$cold;
            if (cold == null) {
                return;
            }

            mapple$storage().thaw(cold, self().sections);
            this.mapple$cold = null;
        }
    }

    @Override
    public LevelChunkSection mapple$section(int index) {
        synchronized (this) {
            mapple$thaw();
            return self().sections[index];
        }
    }

    @Override
    public LevelChunkSection mapple$writableSection(int index) {
        synchronized (this) {
            mapple$thaw();
            return SharedAirSection.writable(self().sections, index);
        }
    }

    @Unique
    private ChunkAccess self() {
        return (ChunkAccess) (Object) this;
    }

    @Unique
    private ColdStorage mapple$storage() {
        return ((ColdStorageHolder) self().levelHeightAccessor).mapple$coldStorage();
    }
}
