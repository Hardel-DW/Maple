package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.persistent.PersistentTopSections;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkyLightSectionStorage.class)
public abstract class SkyLightSectionStorageMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;"))
    private static Long2IntOpenHashMap mapple$persistentTopSections() {
        return new PersistentTopSections();
    }
}
