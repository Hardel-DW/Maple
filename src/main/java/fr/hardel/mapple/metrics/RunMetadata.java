package fr.hardel.mapple.metrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;

public record RunMetadata(String name, String minecraftVersion, long seed, int viewDistance, int simulationDistance, int periodTicks, long heapMaxBytes, List<String> jvmArguments, List<String> garbageCollectors, Map<String, String> mods) {
    public static RunMetadata capture(MinecraftServer server, String name, int periodTicks) {
        Map<String, String> mods = new TreeMap<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            mods.put(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString());
        }

        return new RunMetadata(name, SharedConstants.getCurrentVersion().name(), server.overworld().getSeed(), server.getPlayerList().getViewDistance(),
            server.getPlayerList().getSimulationDistance(), periodTicks, ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax(),
            ManagementFactory.getRuntimeMXBean().getInputArguments(), ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getName).toList(), mods);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.name);
        json.addProperty("minecraftVersion", this.minecraftVersion);
        json.addProperty("seed", this.seed);
        json.addProperty("viewDistance", this.viewDistance);
        json.addProperty("simulationDistance", this.simulationDistance);
        json.addProperty("periodTicks", this.periodTicks);
        json.addProperty("heapMaxBytes", this.heapMaxBytes);
        json.add("jvmArguments", toArray(this.jvmArguments));
        json.add("garbageCollectors", toArray(this.garbageCollectors));

        JsonObject mods = new JsonObject();
        this.mods.forEach(mods::addProperty);
        json.add("mods", mods);
        return json;
    }

    private static JsonArray toArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
