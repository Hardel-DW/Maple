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
 * The blocks of one section as local ids with a count per id, starting from what it holds: the local ids are those of its palette, so new states follow
 * in the order vanilla's writes would add them. At the end a changed section takes one container built by the vanilla constructor, and its counters
 * from the counts instead of a recount.
 */
final class SectionBuilder {
    private final LevelChunkSection section;
    private final Strategy<BlockState> strategy;
    private final List<BlockState> entries = new ArrayList<>();
    private final short[] ids;
    private int[] counts = new int[4];
    private byte[] kinds = new byte[4];
    private boolean changed;
    private @Nullable BlockState lastState;
    private int lastId;

    SectionBuilder(LevelChunkSection section) {
        this.section = section;
        PalettedContainer<BlockState> states = section.getStates();
        this.strategy = states.strategy;
        this.ids = new short[this.strategy.entryCount()];
        PalettedContainer.Data<BlockState> data = states.data;
        if (data.configuration() instanceof Configuration.Global) {
            decode(data.storage(), data.palette());
            return;
        }

        for (int id = 0; id < data.palette().getSize(); id++) {
            add(data.palette().valueFor(id));
        }

        if (data.storage() instanceof ZeroBitStorage) {
            this.counts[0] = this.ids.length;
            return;
        }

        decode(data.storage(), null);
    }

    /** The index of a block of the section, from its coordinates inside it. */
    int index(int x, int y, int z) {
        return this.strategy.getIndex(x, y, z);
    }

    BlockState get(int index) {
        return this.entries.get(this.ids[index]);
    }

    byte kind(int index) {
        return this.kinds[this.ids[index]];
    }

    /** Tells whether the block changed. */
    boolean set(int index, BlockState state) {
        int id = localId(state);
        int old = this.ids[index];
        if (old == id) {
            return false;
        }

        this.counts[old]--;
        this.counts[id]++;
        this.ids[index] = (short) id;
        this.changed = true;
        return true;
    }

    void build() {
        if (!this.changed) {
            return;
        }

        Configuration configuration = this.strategy.getConfigurationForPaletteSize(this.entries.size());
        Palette<BlockState> palette = configuration.createPalette(this.strategy, this.entries);
        this.section.states = new PalettedContainer<>(this.strategy, configuration, storage(configuration, palette), palette);
        countBlocks();
    }

    /**
     * Reads every raw id, in the layout storage() packs: a global palette holds registry ids, each distinct state then gets a local id in the order it
     * first appears. Any other palette's ids are already the local ones.
     */
    private void decode(BitStorage storage, @Nullable Palette<BlockState> global) {
        if (!(storage instanceof SimpleBitStorage simple)) {
            for (int index = 0; index < this.ids.length; index++) {
                store(index, storage.get(index), global);
            }

            return;
        }

        int bits = simple.getBits();
        int valuesPerLong = 64 / bits;
        long mask = (1L << bits) - 1;
        int index = 0;
        for (long cell : simple.getRaw()) {
            for (int slot = 0; slot < valuesPerLong && index < this.ids.length; slot++, index++) {
                store(index, (int) (cell >>> slot * bits & mask), global);
            }
        }
    }

    private void store(int index, int raw, @Nullable Palette<BlockState> global) {
        int id = global == null ? raw : localId(global.valueFor(raw));
        this.ids[index] = (short) id;
        this.counts[id]++;
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
        int index = 0;
        for (int cell = 0; cell < data.length; cell++) {
            long word = 0;
            for (int slot = 0; slot < valuesPerLong && index < this.ids.length; slot++, index++) {
                word |= (long) paletteIds[this.ids[index]] << slot * bits;
            }

            data[cell] = word;
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

        return add(state);
    }

    private int add(BlockState state) {
        int id = this.entries.size();
        this.entries.add(state);
        if (id == this.counts.length) {
            this.counts = Arrays.copyOf(this.counts, id * 2);
            this.kinds = Arrays.copyOf(this.kinds, id * 2);
        }

        this.kinds[id] = BlockKind.of(state);
        return id;
    }
}
