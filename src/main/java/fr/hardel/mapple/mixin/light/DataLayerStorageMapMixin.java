package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.LayerMapAccess;
import fr.hardel.mapple.optimisation.light.PersistentLongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataLayerStorageMap.class)
public abstract class DataLayerStorageMapMixin implements LayerMapAccess {
    @Unique
    private PersistentLongMap<DataLayer> mapple$layers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$startEmpty(CallbackInfo callback) {
        this.mapple$layers = PersistentLongMap.empty();
    }

    @Redirect(method = {"getLayer", "copyDataLayer"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;get(J)Ljava/lang/Object;"))
    private Object mapple$get(Long2ObjectOpenHashMap<DataLayer> map, long sectionNode) {
        return this.mapple$layers.get(sectionNode);
    }

    @Redirect(method = "hasLayer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;containsKey(J)Z"))
    private boolean mapple$has(Long2ObjectOpenHashMap<DataLayer> map, long sectionNode) {
        return this.mapple$layers.get(sectionNode) != null;
    }

    @Redirect(method = {"setLayer", "copyDataLayer"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private Object mapple$put(Long2ObjectOpenHashMap<DataLayer> map, long sectionNode, Object layer) {
        this.mapple$layers = this.mapple$layers.with(sectionNode, (DataLayer) layer);
        return null;
    }

    @Redirect(method = "removeLayer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;remove(J)Ljava/lang/Object;"))
    private Object mapple$remove(Long2ObjectOpenHashMap<DataLayer> map, long sectionNode) {
        DataLayer removed = this.mapple$layers.get(sectionNode);
        this.mapple$layers = this.mapple$layers.without(sectionNode);
        return removed;
    }

    @Override
    public PersistentLongMap<DataLayer> mapple$layers() {
        return this.mapple$layers;
    }

    @Override
    public void mapple$share(PersistentLongMap<DataLayer> layers) {
        this.mapple$layers = layers;
    }
}
