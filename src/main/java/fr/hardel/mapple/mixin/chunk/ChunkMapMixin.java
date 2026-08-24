package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.persistent.PersistentHolderMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;", ordinal = 0))
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> mapple$persistentHolders() {
        return new PersistentHolderMap<>();
    }
}
