package fr.hardel.mapple.optimisation.section;

import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public final class SharedAirSection extends SharedSection {
    SharedAirSection(PalettedContainer<BlockState> states, PalettedContainer<Holder<Biome>> biomes) {
        super(states, biomes);
    }

    public static LevelChunkSection writable(LevelChunkSection[] sections, int index) {
        LevelChunkSection section = sections[index];
        if (section instanceof SharedAirSection) {
            section = section.copy();
            sections[index] = section;
        }

        return section;
    }

    @Override
    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state, boolean checkThreading) {
        throw immutable();
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int quartMinX, int quartMinY, int quartMinZ) {
        throw immutable();
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        throw immutable();
    }

    @Override
    public void readBiomes(FriendlyByteBuf buffer) {
        throw immutable();
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("A shared air section must be made writable before any write");
    }
}
