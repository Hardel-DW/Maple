package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.AirSectionCache;
import fr.hardel.mapple.optimisation.section.AirSectionCacheHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin implements AirSectionCacheHolder {
    @Shadow
    @Final
    private PalettedContainerFactory palettedContainerFactory;

    @Unique
    private AirSectionCache mapple$airSections;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$createAirSections(CallbackInfo callback) {
        this.mapple$airSections = new AirSectionCache(this.palettedContainerFactory);
    }

    @Override
    public AirSectionCache mapple$airSections() {
        return this.mapple$airSections;
    }
}
