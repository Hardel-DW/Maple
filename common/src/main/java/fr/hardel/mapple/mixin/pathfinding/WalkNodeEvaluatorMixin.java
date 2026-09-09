package fr.hardel.mapple.mixin.pathfinding;

import fr.hardel.mapple.optimisation.pathfinding.PathfindingScratch;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<PathType> pathTypesByPosCacheByMob;

    @Shadow
    @Final
    @Mutable
    private Object2BooleanMap<AABB> collisionCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mapple$dropOwnCaches(CallbackInfo callback) {
        this.mapple$detach();
    }

    @Inject(method = "prepare", at = @At("HEAD"))
    private void mapple$attachCaches(PathNavigationRegion level, Mob entity, CallbackInfo callback) {
        PathfindingScratch scratch = PathfindingScratch.current();
        scratch.pathTypes.clear();
        scratch.collisions.clear();
        this.pathTypesByPosCacheByMob = scratch.pathTypes;
        this.collisionCache = scratch.collisions;
    }

    @Inject(method = "done", at = @At("RETURN"))
    private void mapple$detachCaches(CallbackInfo callback) {
        this.mapple$detach();
    }

    @Unique
    private void mapple$detach() {
        this.pathTypesByPosCacheByMob = null;
        this.collisionCache = null;
    }
}
