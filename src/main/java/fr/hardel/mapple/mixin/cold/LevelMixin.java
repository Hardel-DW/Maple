package fr.hardel.mapple.mixin.cold;

import fr.hardel.mapple.optimisation.cold.ColdStorage;
import fr.hardel.mapple.optimisation.cold.ColdStorageHolder;
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
public abstract class LevelMixin implements ColdStorageHolder {
    @Shadow
    @Final
    private PalettedContainerFactory palettedContainerFactory;

    @Unique
    private ColdStorage mapple$coldStorage;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$createColdStorage(CallbackInfo callback) {
        this.mapple$coldStorage = new ColdStorage(this.palettedContainerFactory, (AirSectionCacheHolder) this);
    }

    @Override
    public ColdStorage mapple$coldStorage() {
        return this.mapple$coldStorage;
    }
}
