package fr.hardel.mapple.mixin.feature;

import fr.hardel.mapple.optimisation.feature.OreProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The probe square only depends on the origin and the size: when no column of it reaches the ore, vanilla returns false after its three draws, which
 * the placement's next attempts depend on. Placed outside generation, by a command, the ore keeps vanilla's probe.
 */
@Mixin(OreFeature.class)
public abstract class OreFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void mapple$skipUnreachable(FeaturePlaceContext<OreConfiguration> context, CallbackInfoReturnable<Boolean> callback) {
        if (!(context.level() instanceof WorldGenRegion region)) {
            return;
        }

        int size = context.config().size;
        float spreadXY = size / 8.0F;
        int maxRadius = Mth.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F);
        int reach = Mth.ceil(spreadXY) + maxRadius;
        BlockPos origin = context.origin();
        if (OreProbe.reaches(region, origin.getX() - reach, origin.getZ() - reach, 2 * reach, origin.getY() - 2 - maxRadius)) {
            return;
        }

        RandomSource random = context.random();
        random.nextFloat();
        random.nextInt(3);
        random.nextInt(3);
        callback.setReturnValue(false);
    }
}
