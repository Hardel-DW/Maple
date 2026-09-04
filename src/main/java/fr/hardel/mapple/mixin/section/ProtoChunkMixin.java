package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.SharedAirSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin {
    @Redirect(
        method = "setBlockState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ProtoChunk;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection mapple$writableSection(ProtoChunk chunk, int index, BlockPos pos, BlockState state, int flags) {
        return state.is(Blocks.AIR) ? chunk.getSection(index) : SharedAirSection.writable(chunk.getSections(), index);
    }
}
