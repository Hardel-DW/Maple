package fr.hardel.mapple.mixin.section;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.hardel.mapple.optimisation.section.AirSectionCacheHolder;
import fr.hardel.mapple.optimisation.section.AirSectionCompaction;
import fr.hardel.mapple.optimisation.section.DiscardingSection;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An imposter never reads its own sections, a proto chunk of a level starts with its shared empty one, and uniform air goes back to the shared section of its biome. */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements AirSectionCompaction {
    @Shadow
    protected ChunkSkyLightSources skyLightSources;

    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    @Shadow
    public abstract LevelChunkSection[] getSections();

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;replaceMissingSections("
                + "Lnet/minecraft/world/level/chunk/PalettedContainerFactory;[Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"
        )
    )
    private void mapple$shareMissingSections(PalettedContainerFactory containerFactory, LevelChunkSection[] sections, Operation<Void> original,
        @Local(argsOnly = true) LevelHeightAccessor heightAccessor) {
        if ((Object) this instanceof ImposterProtoChunk) {
            Arrays.fill(sections, DiscardingSection.INSTANCE);
            this.skyLightSources = null;
            return;
        }

        if (!((Object) this instanceof ProtoChunk) || !(heightAccessor instanceof AirSectionCacheHolder level)) {
            original.call(containerFactory, sections);
            return;
        }

        SharedAirSection empty = level.mapple$airSections().empty();
        for (int index = 0; index < sections.length; index++) {
            if (sections[index] == null) {
                sections[index] = empty;
            }
        }
    }

    @Inject(method = "collectBiomesInPalette", at = @At("HEAD"), cancellable = true)
    private void mapple$collectWrappedBiomes(Set<Holder<Biome>> output, CallbackInfo callback) {
        if ((Object) this instanceof ImposterProtoChunk imposter) {
            imposter.getWrapped().collectBiomesInPalette(output);
            callback.cancel();
        }
    }

    @Redirect(
        method = "fillBiomesFromNoise",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection mapple$writableSection(ChunkAccess chunk, int index) {
        return SharedAirSection.writable(chunk.getSections(), index);
    }

    @Inject(method = "fillBiomesFromNoise", at = @At("RETURN"))
    private void mapple$compactFilledSections(CallbackInfo callback) {
        mapple$compactAirSections();
    }

    /** A chunk built outside a level, a mod's template or preview, has no shared sections to go back to. */
    @Override
    public void mapple$compactAirSections() {
        if (this.levelHeightAccessor instanceof AirSectionCacheHolder level) {
            level.mapple$airSections().compact(getSections());
        }
    }
}
