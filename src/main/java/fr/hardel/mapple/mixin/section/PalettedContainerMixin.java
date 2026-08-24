package fr.hardel.mapple.mixin.section;

import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin {
    @Unique
    private static final ThreadingDetector UNUSED_DETECTOR = new ThreadingDetector("PalettedContainer");

    @Unique
    private @Nullable Thread mapple$owner;

    @Redirect(
        method = {
            "<init>(Ljava/lang/Object;Lnet/minecraft/world/level/chunk/Strategy;)V",
            "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;)V",
            "<init>(Lnet/minecraft/world/level/chunk/Strategy;Lnet/minecraft/world/level/chunk/Configuration;Lnet/minecraft/util/BitStorage;Lnet/minecraft/world/level/chunk/Palette;)V"
        },
        at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/util/ThreadingDetector;"),
        require = 3
    )
    private static ThreadingDetector mapple$sharedDetector(String name) {
        return UNUSED_DETECTOR;
    }

    @Inject(method = "acquire", at = @At("HEAD"), cancellable = true)
    private void mapple$acquire(CallbackInfo callback) {
        synchronized (this) {
            Thread owner = this.mapple$owner;
            if (owner != null) {
                throw ThreadingDetector.makeThreadingException("PalettedContainer", owner);
            }

            this.mapple$owner = Thread.currentThread();
        }

        callback.cancel();
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void mapple$release(CallbackInfo callback) {
        synchronized (this) {
            this.mapple$owner = null;
        }

        callback.cancel();
    }
}
