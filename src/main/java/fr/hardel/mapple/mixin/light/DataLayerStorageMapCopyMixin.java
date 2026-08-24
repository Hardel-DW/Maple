package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.LayerMapAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.level.lighting.BlockLightSectionStorage$BlockDataLayerStorageMap", "net.minecraft.world.level.lighting.SkyLightSectionStorage$SkyDataLayerStorageMap"})
public abstract class DataLayerStorageMapCopyMixin {
    @Redirect(method = "copy", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;"))
    private Long2ObjectOpenHashMap<DataLayer> mapple$skipClone(Long2ObjectOpenHashMap<DataLayer> map) {
        return new Long2ObjectOpenHashMap<>();
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void mapple$shareLayers(CallbackInfoReturnable<?> callback) {
        ((LayerMapAccess) callback.getReturnValue()).mapple$share(((LayerMapAccess) this).mapple$layers());
    }
}
