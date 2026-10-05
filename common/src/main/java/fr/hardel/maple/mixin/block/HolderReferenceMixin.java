package fr.hardel.maple.mixin.block;

import fr.hardel.maple.optimisation.LeafDistance;
import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Holder.Reference.class)
public abstract class HolderReferenceMixin<T> {
    @Shadow
    private @Nullable T value;

    @Inject(method = "bindTags", at = @At("RETURN"))
    private void maple$refreshLeafDistances(Collection<?> tags, CallbackInfo callback) {
        if (this.value instanceof Block block && block.builtInRegistryHolder() == (Object) this) {
            block.getStateDefinition().getPossibleStates().forEach(state -> ((LeafDistance) state).maple$refreshLeafDistance());
        }
    }
}
