package fr.hardel.mapple.optimisation.cold;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.FluidState;

/** The slot of a cold chunk. Any access thaws the chunk and goes to the section that took the slot, a write through copy-on-write first. */
final class ColdSection extends LevelChunkSection {
    private final ColdChunk owner;
    private final int index;

    ColdSection(ColdChunk owner, int index, LevelChunkSection empty) {
        super(empty.getStates(), empty.getBiomes());
        this.owner = owner;
        this.index = index;
    }

    private LevelChunkSection section() {
        return this.owner.mapple$section(this.index);
    }

    private LevelChunkSection writable() {
        return this.owner.mapple$writableSection(this.index);
    }

    @Override
    public BlockState getBlockState(int sectionX, int sectionY, int sectionZ) {
        return section().getBlockState(sectionX, sectionY, sectionZ);
    }

    @Override
    public FluidState getFluidState(int sectionX, int sectionY, int sectionZ) {
        return section().getFluidState(sectionX, sectionY, sectionZ);
    }

    @Override
    public void acquire() {
        section().acquire();
    }

    @Override
    public void release() {
        section().release();
    }

    @Override
    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state, boolean checkThreading) {
        return writable().setBlockState(sectionX, sectionY, sectionZ, state, checkThreading);
    }

    @Override
    public boolean hasOnlyAir() {
        return section().hasOnlyAir();
    }

    @Override
    public boolean hasFluid() {
        return section().hasFluid();
    }

    @Override
    public boolean isRandomlyTickingBlocks() {
        return section().isRandomlyTickingBlocks();
    }

    @Override
    public boolean isRandomlyTickingFluids() {
        return section().isRandomlyTickingFluids();
    }

    @Override
    public PalettedContainer<BlockState> getStates() {
        return section().getStates();
    }

    @Override
    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        return section().getBiomes();
    }

    @Override
    public void read(FriendlyByteBuf buffer) {
        writable().read(buffer);
    }

    @Override
    public void readBiomes(FriendlyByteBuf buffer) {
        writable().readBiomes(buffer);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        section().write(buffer);
    }

    @Override
    public int getSerializedSize() {
        return section().getSerializedSize();
    }

    @Override
    public boolean maybeHas(Predicate<BlockState> predicate) {
        return section().maybeHas(predicate);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return section().getNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int quartMinX, int quartMinY, int quartMinZ) {
        writable().fillBiomesFromNoise(biomeResolver, sampler, quartMinX, quartMinY, quartMinZ);
    }

    @Override
    public LevelChunkSection copy() {
        return section().copy();
    }
}
