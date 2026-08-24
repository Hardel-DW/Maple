package fr.hardel.mapple.optimisation.section;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

abstract class SharedSection extends LevelChunkSection {
    SharedSection(PalettedContainer<BlockState> states, PalettedContainerRO<Holder<Biome>> biomes) {
        super(states, biomes);
    }

    @Override
    public final void acquire() {
    }

    @Override
    public final void release() {
    }
}
