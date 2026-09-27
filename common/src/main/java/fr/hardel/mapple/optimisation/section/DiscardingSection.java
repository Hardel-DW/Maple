package fr.hardel.mapple.optimisation.section;

import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/** The slot of an imposter: it reads as a fresh section, air and the default biome, and drops every write, as vanilla drops the imposter's own sections. */
public final class DiscardingSection extends LevelChunkSection {
    DiscardingSection(PalettedContainerFactory factory) {
        super(factory);
    }

    @Override
    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state, boolean checkThreading) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int quartMinX, int quartMinY, int quartMinZ) {
    }
}
