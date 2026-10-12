package fr.hardel.maple.optimisation.section;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/** What the chunks of one level share: one data per single value, and the write sink of every imposter. Sections stay owned by their chunk. */
public final class SharedSectionData {
    private final Map<Object, PalettedContainer.Data<?>> singleValues = new ConcurrentHashMap<>();
    private final DiscardingSection imposterSection;

    public SharedSectionData(PalettedContainerFactory factory) {
        this.imposterSection = new DiscardingSection(factory);
    }

    public DiscardingSection imposterSection() {
        return this.imposterSection;
    }

    /** A new proto chunk: a biome generator may still fill the single-value biome palette in place, so its biomes wait for the level chunk. */
    public void shareStates(LevelChunkSection[] sections) {
        for (LevelChunkSection section : sections) {
            ((SingleValueSharing) section.getStates()).maple$share(this);
        }
    }

    public void share(LevelChunkSection[] sections) {
        for (LevelChunkSection section : sections) {
            ((SingleValueSharing) section.getStates()).maple$share(this);
            ((SingleValueSharing) section.getBiomes()).maple$share(this);
        }
    }

    /** The first data seen for a value becomes the level's. Vanilla never writes into a single-value data: another value resizes into a new one. */
    @SuppressWarnings("unchecked") // the data of a value holds that value, so both have its type
    public <T> PalettedContainer.Data<T> canonical(T value, PalettedContainer.Data<T> data) {
        return (PalettedContainer.Data<T>) this.singleValues.computeIfAbsent(value, key -> data);
    }
}
