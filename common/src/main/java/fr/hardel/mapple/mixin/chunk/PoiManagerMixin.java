package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.chunk.ChunkIndex;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin implements ChunkIndex {
    @Shadow
    @Final
    private LongSet loadedChunks;

    @Override
    public void mapple$unloadChunk(long chunkKey) {
        SectionStorage<?, ?> sections = (SectionStorage<?, ?>) (Object) this;
        synchronized (sections.loadLock) {
            if (sections.pendingLoads.containsKey(chunkKey)) {
                return;
            }

            int x = ChunkPos.getX(chunkKey);
            int z = ChunkPos.getZ(chunkKey);
            for (int sectionY = sections.levelHeightAccessor.getMinSectionY(); sectionY <= sections.levelHeightAccessor.getMaxSectionY(); sectionY++) {
                sections.storage.remove(SectionPos.asLong(x, sectionY, z));
            }

            sections.loadedChunks.remove(chunkKey);
        }

        this.loadedChunks.remove(chunkKey);
    }
}
