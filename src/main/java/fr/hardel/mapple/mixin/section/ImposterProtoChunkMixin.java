package fr.hardel.mapple.mixin.section;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ImposterProtoChunk.class)
public abstract class ImposterProtoChunkMixin {
    @Shadow
    @Final
    private LevelChunk wrapped;

    public void collectBiomesInPalette(Set<Holder<Biome>> output) {
        this.wrapped.collectBiomesInPalette(output);
    }
}
