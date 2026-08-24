package fr.hardel.mapple.optimisation.section;

import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;

public final class DiscardingSection extends SharedSection {
    public static final DiscardingSection INSTANCE = new DiscardingSection();

    private DiscardingSection() {
        super(new PalettedContainer<>(Blocks.AIR.defaultBlockState(), Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY)), null);
    }

    @Override
    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state, boolean checkThreading) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        throw unreachable();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        throw unreachable();
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int quartMinX, int quartMinY, int quartMinZ) {
        throw unreachable();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        throw unreachable();
    }

    @Override
    public int getSerializedSize() {
        throw unreachable();
    }

    private static UnsupportedOperationException unreachable() {
        return new UnsupportedOperationException("An imposter section delegates everything to the wrapped chunk");
    }
}
