package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.chunk.ChunkIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin implements ChunkIndex {
    @Shadow
    @Final
    private Long2ObjectMap<?> storage;

    @Shadow
    @Final
    private LongSet loadedChunks;

    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    @Override
    public void mapple$unloadChunk(long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        for (int sectionY = this.levelHeightAccessor.getMinSectionY(); sectionY <= this.levelHeightAccessor.getMaxSectionY(); sectionY++) {
            this.storage.remove(SectionPos.asLong(x, sectionY, z));
        }

        this.loadedChunks.remove(chunkKey);
    }
}
