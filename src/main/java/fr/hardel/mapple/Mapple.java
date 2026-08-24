package fr.hardel.mapple;

import fr.hardel.mapple.metrics.MetricsCommand;
import fr.hardel.mapple.metrics.MetricsService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class Mapple implements ModInitializer {
    public static final String MOD_ID = "mapple";

    @Override
    public void onInitialize() {
        MetricsCommand.register();
        ServerLifecycleEvents.SERVER_STARTING.register(MetricsService::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MetricsService.stop());
        ServerTickEvents.END_SERVER_TICK.register(server -> MetricsService.tick());
    }
}
