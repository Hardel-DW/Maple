package fr.hardel.mapple.optimisation.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Configuration;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * The blocks of one section as local ids with a count per id, starting from what it holds. At the end the section takes one container built by the
 * vanilla constructor, with the palette in the order vanilla's writes would give, and its counters from the counts instead of a recount.
 */
final class SectionBuilder {
    private final LevelChunkSection section;
    private final Strategy<BlockState> strategy;
    private final List<BlockState> entries = new ArrayList<>();
    private final short[] ids;
    private int[] counts = new int[4];
    private @Nullable BlockState lastState;
    private int lastId;

    SectionBuilder(LevelChunkSection section) {
        this.section = section;
        PalettedContainer<BlockState> states = section.getStates();
        this.strategy = states.strategy;
        this.ids = new short[this.strategy.entryCount()];
        if (states.bitsPerEntry() == 0) {
            this.entries.add(states.get(0, 0, 0));
            this.counts[0] = this.ids.length;
            return;
        }

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int id = localId(states.get(x, y, z));
                    this.ids[this.strategy.getIndex(x, y, z)] = (short) id;
                    this.counts[id]++;
                }
            }
        }
    }

    void set(int x, int y, int z, BlockState state) {
        int index = this.strategy.getIndex(x, y, z);
        int id = localId(state);
        this.counts[this.ids[index]]--;
        this.counts[id]++;
        this.ids[index] = (short) id;
    }

    void build() {
        Configuration configuration = this.strategy.getConfigurationForPaletteSize(this.entries.size());
        Palette<BlockState> palette = configuration.createPalette(this.strategy, this.entries);
        this.section.states = new PalettedContainer<>(this.strategy, configuration, storage(configuration, palette), palette);
        countBlocks();
    }

    /** Vanilla's recalcBlockCounts, over the counts of the palette. */
    private void countBlocks() {
        int nonEmpty = 0;
        int fluids = 0;
        int tickingBlocks = 0;
        int tickingFluids = 0;
        for (int id = 0; id < this.entries.size(); id++) {
            BlockState state = this.entries.get(id);
            int count = this.counts[id];
            if (count == 0 || state.isAir()) {
                continue;
            }

            nonEmpty += count;
            if (state.isRandomlyTicking()) {
                tickingBlocks += count;
            }

            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                fluids += count;
                if (fluid.isRandomlyTicking()) {
                    tickingFluids += count;
                }
            }
        }

        this.section.nonEmptyBlockCount = (short) nonEmpty;
        this.section.fluidCount = (short) fluids;
        this.section.tickingBlockCount = (short) tickingBlocks;
        this.section.tickingFluidCount = (short) tickingFluids;
    }

    /** Packed as SimpleBitStorage packs an int array: the first value of a long in its lowest bits, no value across two longs. */
    private BitStorage storage(Configuration configuration, Palette<BlockState> palette) {
        int bits = configuration.bitsInMemory();
        if (bits == 0) {
            return new ZeroBitStorage(this.ids.length);
        }

        int[] paletteIds = new int[this.entries.size()];
        for (int local = 0; local < paletteIds.length; local++) {
            paletteIds[local] = palette.idFor(this.entries.get(local), PaletteResize.noResizeExpected());
        }

        int valuesPerLong = 64 / bits;
        long[] data = new long[(this.ids.length + valuesPerLong - 1) / valuesPerLong];
        for (int index = 0; index < this.ids.length; index++) {
            data[index / valuesPerLong] |= (long) paletteIds[this.ids[index]] << (index % valuesPerLong * bits);
        }

        return new SimpleBitStorage(bits, this.ids.length, data);
    }

    /** Blocks written in a row mostly share their state: the last one answers without a search. */
    private int localId(BlockState state) {
        if (state == this.lastState) {
            return this.lastId;
        }

        this.lastState = state;
        this.lastId = id(state);
        return this.lastId;
    }

    private int id(BlockState state) {
        int id = this.entries.indexOf(state);
        if (id >= 0) {
            return id;
        }

        this.entries.add(state);
        if (this.entries.size() > this.counts.length) {
            this.counts = Arrays.copyOf(this.counts, this.counts.length * 2);
        }

        return this.entries.size() - 1;
    }
}
