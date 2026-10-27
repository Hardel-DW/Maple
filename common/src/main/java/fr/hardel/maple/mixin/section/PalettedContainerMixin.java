package fr.hardel.maple.mixin.section;

import fr.hardel.maple.optimisation.section.NoCheckThreadingDetector;
import fr.hardel.maple.optimisation.section.SharedSectionData;
import fr.hardel.maple.optimisation.section.SingleValueSharing;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> implements SingleValueSharing {
    @Unique
    private static final ThreadingDetector NO_CHECK = new NoCheckThreadingDetector();

    @Shadow
    private volatile PalettedContainer.Data<T> data;

    @Shadow
    private PalettedContainer.Data<T> createOrReuseData(PalettedContainer.@Nullable Data<T> oldData, int targetBits) {
        throw new AssertionError();
    }

    @Redirect(
        method = {
            "<init>(Ljava/lang/Object;Lnet/minecraft/world/level/chunk/Strategy;)V",
            "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;)V",
            "<init>(Lnet/minecraft/world/level/chunk/Strategy;Lnet/minecraft/world/level/chunk/Configuration;Lnet/minecraft/util/BitStorage;Lnet/minecraft/world/level/chunk/Palette;)V"
        },
        at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/util/ThreadingDetector;"),
        require = 3
    )
    private static ThreadingDetector maple$noDetector(String name) {
        return NO_CHECK;
    }

    /** The one vanilla path that writes into a data in place, so it never reuses one: that data may be shared. */
    @Redirect(
        method = "read",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/PalettedContainer;createOrReuseData(Lnet/minecraft/world/level/chunk/PalettedContainer$Data;I)"
                + "Lnet/minecraft/world/level/chunk/PalettedContainer$Data;"
        )
    )
    private PalettedContainer.Data<T> maple$freshData(PalettedContainer<T> container, PalettedContainer.Data<T> oldData, int targetBits) {
        return this.createOrReuseData(null, targetBits);
    }

    @Override
    public void maple$share(SharedSectionData shared) {
        PalettedContainer.Data<T> current = this.data;
        if (current.palette() instanceof SingleValuePalette<T> palette) {
            this.data = shared.canonical(palette.valueFor(0), current);
        }
    }
}
