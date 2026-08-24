package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class EntityProbe implements MetricProbe {
    private final MinecraftServer server;

    public EntityProbe(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void collect(MetricSink sink) {
        for (ServerLevel level : this.server.getAllLevels()) {
            collect(level, sink);
        }
    }

    private static void collect(ServerLevel level, MetricSink sink) {
        String scope = MetricProbe.scopeOf(level);
        long loaded = 0L;
        long players = 0L;
        long living = 0L;
        long items = 0L;
        for (Entity entity : level.getEntities().getAll()) {
            loaded++;
            if (entity instanceof Player) {
                players++;
            } else if (entity instanceof LivingEntity) {
                living++;
            } else if (entity instanceof ItemEntity) {
                items++;
            }
        }

        sink.put(scope, "entity.loaded", loaded);
        sink.put(scope, "entity.players", players);
        sink.put(scope, "entity.living", living);
        sink.put(scope, "entity.items", items);
        sink.put(scope, "entity.other", loaded - players - living - items);
    }
}
