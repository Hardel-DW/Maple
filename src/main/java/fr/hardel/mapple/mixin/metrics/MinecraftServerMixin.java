package fr.hardel.mapple.mixin.metrics;

import fr.hardel.mapple.metrics.AutosaveTimer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements AutosaveTimer {
    @Unique
    private long mapple$autosaveBeganAtNanos;

    @Unique
    private long mapple$autosaveMillis;

    @Inject(method = "autoSave", at = @At("HEAD"))
    private void mapple$autosaveBegan(CallbackInfo callback) {
        this.mapple$autosaveBeganAtNanos = System.nanoTime();
    }

    @Inject(method = "autoSave", at = @At("RETURN"))
    private void mapple$autosaveEnded(CallbackInfo callback) {
        this.mapple$autosaveMillis = (System.nanoTime() - this.mapple$autosaveBeganAtNanos) / 1_000_000L;
    }

    @Override
    public long mapple$autosaveMillis() {
        return this.mapple$autosaveMillis;
    }
}
