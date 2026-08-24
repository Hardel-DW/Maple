package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.LightDataListener;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageMixin {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private LongSet mapple$chunksWithChangedData = new LongOpenHashSet();

    @Inject(method = "setStoredLevel", at = @At("HEAD"))
    private void mapple$trackChangedData(long blockNode, int level, CallbackInfo callback) {
        this.mapple$chunksWithChangedData.add(ChunkPos.pack(SectionPos.blockToSectionCoord(BlockPos.getX(blockNode)), SectionPos.blockToSectionCoord(BlockPos.getZ(blockNode))));
    }

    @Inject(method = "swapSectionMap", at = @At("HEAD"))
    private void mapple$publishChangedData(CallbackInfo callback) {
        if (!this.mapple$chunksWithChangedData.isEmpty()) {
            ((LightDataListener) this.chunkSource).mapple$onLightDataChanged(this.mapple$chunksWithChangedData);
            this.mapple$chunksWithChangedData = new LongOpenHashSet();
        }
    }
}
