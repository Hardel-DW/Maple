package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.chunk.ChunkIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin implements ChunkIndex {
    @Shadow
    @Final
    private Long2ObjectMap<?> loadedChunks;

    @Override
    public void mapple$unloadChunk(long chunkKey) {
        this.loadedChunks.remove(chunkKey);
    }
}
