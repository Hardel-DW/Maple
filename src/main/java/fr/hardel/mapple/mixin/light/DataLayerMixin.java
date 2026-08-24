package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.PackedDataLayer;
import java.util.Arrays;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataLayer.class)
public abstract class DataLayerMixin implements PackedDataLayer {
    @Shadow
    protected byte @Nullable [] data;

    @Shadow
    private int defaultValue;

    @Inject(method = "<init>([B)V", at = @At("RETURN"))
    private void mapple$compactUniform(byte[] data, CallbackInfo callback) {
        byte first = data[0];
        if ((first & 15) != (first >> 4 & 15)) {
            return;
        }

        for (byte value : data) {
            if (value != first) {
                return;
            }
        }

        this.data = null;
        this.defaultValue = first & 15;
    }

    @Inject(method = "set(II)V", at = @At("HEAD"), cancellable = true)
    private void mapple$keepUniform(int index, int value, CallbackInfo callback) {
        if (this.data == null && (value & 15) == this.defaultValue) {
            callback.cancel();
        }
    }

    @Override
    public byte[] mapple$packedData() {
        if (this.data != null) {
            return this.data;
        }

        byte[] packed = new byte[DataLayer.SIZE];
        Arrays.fill(packed, (byte) (this.defaultValue | this.defaultValue << 4));
        return packed;
    }
}
