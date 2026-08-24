package fr.hardel.mapple.mixin.section;

import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.Configuration;
import net.minecraft.world.level.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.chunk.PalettedContainer$Data")
public abstract class PalettedContainerDataMixin<T> {
    @Shadow
    @Final
    private Configuration configuration;

    @Shadow
    @Final
    private BitStorage storage;

    @Shadow
    @Final
    private Palette<T> palette;

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void mapple$writeStorageBits(FriendlyByteBuf buffer, IdMap<T> globalMap, CallbackInfo callback) {
        if (this.mapple$repacks()) {
            buffer.writeByte(this.configuration.bitsInStorage());
            this.palette.write(buffer, globalMap);
            buffer.writeFixedSizeLongArray(this.mapple$repacked().getRaw());
            callback.cancel();
        }
    }

    @Inject(method = "getSerializedSize", at = @At("HEAD"), cancellable = true)
    private void mapple$storageSerializedSize(IdMap<T> globalMap, CallbackInfoReturnable<Integer> callback) {
        if (this.mapple$repacks()) {
            int valuesPerLong = Long.SIZE / this.configuration.bitsInStorage();
            callback.setReturnValue(1 + this.palette.getSerializedSize(globalMap) + (this.storage.getSize() + valuesPerLong - 1) / valuesPerLong * 8);
        }
    }

    @Unique
    private boolean mapple$repacks() {
        return this.configuration.bitsInMemory() != this.configuration.bitsInStorage();
    }

    @Unique
    private SimpleBitStorage mapple$repacked() {
        int[] values = new int[this.storage.getSize()];
        this.storage.unpack(values);
        return new SimpleBitStorage(this.configuration.bitsInStorage(), values.length, values);
    }
}
