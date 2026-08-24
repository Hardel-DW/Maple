package fr.hardel.mapple.optimisation.pathfinding;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;

public final class PathfindingScratch {
    private static final ThreadLocal<PathfindingScratch> CURRENT = ThreadLocal.withInitial(PathfindingScratch::new);

    public final Node[] neighbors = new Node[32];
    public final BinaryHeap openSet = new BinaryHeap();
    public final Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap<>();
    public final Long2ObjectMap<PathType> pathTypes = new Long2ObjectOpenHashMap<>();
    public final Object2BooleanMap<AABB> collisions = new Object2BooleanOpenHashMap<>();

    private PathfindingScratch() {
    }

    public static PathfindingScratch current() {
        return CURRENT.get();
    }
}
