package fr.hardel.mapple.optimisation.terrain;

import java.util.ArrayList;
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

/**
 * The blocks of one section as local ids, starting from what it holds, turned at the end into one container built by the vanilla constructor.
 * The palette lists the states in the order vanilla's writes would add them, so the container is the one vanilla ends with.
 */
final class SectionBuilder {
    private final LevelChunkSection section;
    private final Strategy<BlockState> strategy;
    private final List<BlockState> entries = new ArrayList<>();
    private final short[] ids;

    SectionBuilder(LevelChunkSection section) {
        this.section = section;
        PalettedContainer<BlockState> states = section.getStates();
        this.strategy = states.strategy;
        this.ids = new short[this.strategy.entryCount()];
        if (states.bitsPerEntry() == 0) {
            this.entries.add(states.get(0, 0, 0));
            return;
        }

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    set(x, y, z, states.get(x, y, z));
                }
            }
        }
    }

    void set(int x, int y, int z, BlockState state) {
        this.ids[this.strategy.getIndex(x, y, z)] = (short) localId(state);
    }

    LevelChunkSection build() {
        Configuration configuration = this.strategy.getConfigurationForPaletteSize(this.entries.size());
        Palette<BlockState> palette = configuration.createPalette(this.strategy, this.entries);
        return new LevelChunkSection(new PalettedContainer<>(this.strategy, configuration, storage(configuration, palette), palette), this.section.getBiomes());
    }

    private BitStorage storage(Configuration configuration, Palette<BlockState> palette) {
        if (configuration.bitsInMemory() == 0) {
            return new ZeroBitStorage(this.ids.length);
        }

        int[] paletteIds = new int[this.entries.size()];
        for (int local = 0; local < paletteIds.length; local++) {
            paletteIds[local] = palette.idFor(this.entries.get(local), PaletteResize.noResizeExpected());
        }

        SimpleBitStorage storage = new SimpleBitStorage(configuration.bitsInMemory(), this.ids.length);
        for (int index = 0; index < this.ids.length; index++) {
            storage.set(index, paletteIds[this.ids[index]]);
        }

        return storage;
    }

    private int localId(BlockState state) {
        int id = this.entries.indexOf(state);
        if (id >= 0) {
            return id;
        }

        this.entries.add(state);
        return this.entries.size() - 1;
    }
}
