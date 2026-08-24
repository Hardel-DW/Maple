package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.persistent.PersistentHolderMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkMap;updatingChunkMap:Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;", opcode = Opcodes.PUTFIELD))
    private void mapple$persistentHolders(ChunkMap map, Long2ObjectLinkedOpenHashMap<ChunkHolder> vanillaMap) {
        this.updatingChunkMap = new PersistentHolderMap<>();
    }
}
