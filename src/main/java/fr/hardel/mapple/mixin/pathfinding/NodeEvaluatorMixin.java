package fr.hardel.mapple.mixin.pathfinding;

import fr.hardel.mapple.optimisation.pathfinding.PathfindingScratch;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NodeEvaluator.class)
public abstract class NodeEvaluatorMixin {
    @Shadow
    @Final
    @Mutable
    protected Int2ObjectMap<Node> nodes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$dropOwnNodes(CallbackInfo callback) {
        this.nodes = null;
    }

    @Inject(method = "prepare", at = @At("HEAD"))
    private void mapple$attachNodes(PathNavigationRegion level, Mob entity, CallbackInfo callback) {
        this.nodes = PathfindingScratch.current().nodes;
    }

    @Inject(method = "done", at = @At("RETURN"))
    private void mapple$detachNodes(CallbackInfo callback) {
        this.nodes = null;
    }
}
