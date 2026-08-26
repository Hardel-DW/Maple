package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.SparseDataLayer;
import fr.hardel.mapple.optimisation.light.SparseLayer;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataLayer.class)
public abstract class DataLayerMixin implements SparseDataLayer {
    @Shadow
    protected byte @Nullable [] data;

    @Shadow
    private int defaultValue;

    @Unique
    private int mapple$slabs;

    @Inject(method = "<init>([B)V", at = @At("RETURN"))
    private void mapple$compress(byte[] full, CallbackInfo callback) {
        SparseLayer.Packed packed = SparseLayer.compress(full);
        this.data = packed.data();
        this.mapple$slabs = packed.slabs();
        this.defaultValue = packed.defaultValue();
    }

    @Inject(method = "fill", at = @At("RETURN"))
    private void mapple$forgetSlabs(int value, CallbackInfo callback) {
        this.mapple$slabs = 0;
    }

    // Absent slabs hold the default value.
    @Inject(method = "get(I)I", at = @At("HEAD"), cancellable = true)
    private void mapple$sparseGet(int index, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(this.data == null ? this.defaultValue : SparseLayer.get(this.data, this.mapple$slabs, this.defaultValue, index));
    }

    // A slab is only allocated by the first value that differs from the default.
    @Inject(method = "set(II)V", at = @At("HEAD"), cancellable = true)
    private void mapple$sparseSet(int index, int value, CallbackInfo callback) {
        callback.cancel();
        value &= 15;
        int slab = index >> 8;
        if ((this.mapple$slabs & 1 << slab) == 0) {
            if (value == this.defaultValue) {
                return;
            }

            this.data = SparseLayer.withSlab(this.data, this.mapple$slabs, slab, this.defaultValue);
            this.mapple$slabs |= 1 << slab;
        }

        SparseLayer.set(this.data, this.mapple$slabs, index, value);
    }

    // Callers only read the full array, so it is expanded on demand instead of stored.
    @Inject(method = "getData", at = @At("HEAD"), cancellable = true)
    private void mapple$expandOnDemand(CallbackInfoReturnable<byte[]> callback) {
        callback.setReturnValue(SparseLayer.expand(this.data, this.mapple$slabs, this.defaultValue));
    }

    @Inject(method = "copy", at = @At("HEAD"), cancellable = true)
    private void mapple$compactCopy(CallbackInfoReturnable<DataLayer> callback) {
        DataLayer copy = new DataLayer(this.defaultValue);
        if (this.data != null) {
            ((SparseDataLayer) copy).mapple$adopt(this.data.clone(), this.mapple$slabs);
        }

        callback.setReturnValue(copy);
    }

    @Override
    public void mapple$adopt(byte[] data, int slabs) {
        this.data = data;
        this.mapple$slabs = slabs;
    }
}
