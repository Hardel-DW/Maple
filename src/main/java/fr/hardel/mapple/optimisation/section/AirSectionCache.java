package fr.hardel.mapple.optimisation.section;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

public final class AirSectionCache {
    private final PalettedContainerFactory factory;
    private final Map<Holder<Biome>, SharedAirSection> sections = new ConcurrentHashMap<>();

    public AirSectionCache(PalettedContainerFactory factory) {
        this.factory = factory;
    }

    /** What a fresh vanilla section holds: air, and the default biome. Every slot of a proto chunk starts there. */
    public SharedAirSection empty() {
        return shared(this.factory.defaultBiome());
    }

    public void compact(LevelChunkSection[] sections) {
        for (int index = 0; index < sections.length; index++) {
            compact(sections, index);
        }
    }

    public void compact(LevelChunkSection[] sections, int index) {
        LevelChunkSection section = sections[index];
        if (section.hasOnlyAir() && section.getBiomes().bitsPerEntry() == 0 && !(section instanceof SharedAirSection)) {
            sections[index] = shared(section.getNoiseBiome(0, 0, 0));
        }
    }

    public SharedAirSection shared(Holder<Biome> biome) {
        return this.sections.computeIfAbsent(biome, this::create);
    }

    private SharedAirSection create(Holder<Biome> biome) {
        return new SharedAirSection(this.factory, biome);
    }
}
