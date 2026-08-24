package fr.hardel.mapple.optimisation.section;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

public final class AirSectionCache {
    private final PalettedContainerFactory factory;
    private final Map<Holder<Biome>, SharedAirSection> sections = new ConcurrentHashMap<>();

    public AirSectionCache(PalettedContainerFactory factory) {
        this.factory = factory;
    }

    public void compact(LevelChunkSection[] sections) {
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() && section.getBiomes().bitsPerEntry() == 0 && !(section instanceof SharedAirSection)) {
                sections[index] = this.sections.computeIfAbsent(section.getNoiseBiome(0, 0, 0), this::create);
            }
        }
    }

    private SharedAirSection create(Holder<Biome> biome) {
        return new SharedAirSection(this.factory.createForBlockStates(), new PalettedContainer<>(biome, this.factory.biomeStrategy()));
    }
}
