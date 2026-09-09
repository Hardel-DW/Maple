package fr.hardel.mapple.mixin.pathfinding;

import fr.hardel.mapple.optimisation.pathfinding.PathfindingScratch;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathFinder.class)
public abstract class PathFinderMixin {
    private static final String FIND_PATH = "findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;";

    @Shadow
    @Final
    @Mutable
    private Node[] neighbors;

    @Shadow
    @Final
    @Mutable
    private BinaryHeap openSet;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$dropOwnScratch(CallbackInfo callback) {
        this.mapple$detach();
    }

    @Inject(method = FIND_PATH, at = @At("HEAD"))
    private void mapple$attachScratch(CallbackInfoReturnable<?> callback) {
        PathfindingScratch scratch = PathfindingScratch.current();
        this.neighbors = scratch.neighbors;
        this.openSet = scratch.openSet;
    }

    @Inject(method = FIND_PATH, at = @At("RETURN"))
    private void mapple$detachScratch(CallbackInfoReturnable<?> callback) {
        this.mapple$detach();
    }

    @Unique
    private void mapple$detach() {
        this.neighbors = null;
        this.openSet = null;
    }
}
