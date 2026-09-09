package fr.hardel.mapple.optimisation.cold;

import net.minecraft.world.level.chunk.LevelChunkSection;

/** A proto chunk that can hold its sections as bytes. Every method is definitive: it takes the chunk monitor and answers from the thawed state. */
public interface ColdChunk {
    void mapple$freeze();

    void mapple$thaw();

    LevelChunkSection mapple$section(int index);

    LevelChunkSection mapple$writableSection(int index);
}
