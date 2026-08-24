package fr.hardel.mapple.optimisation.section;

import java.util.List;
import net.minecraft.world.level.chunk.Configuration;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.Strategy;

public record PackedConfiguration(int bitsInMemory, int bitsInStorage) implements Configuration {
    private static final PackedConfiguration[] BELOW_STORAGE = {null, new PackedConfiguration(1, 4), new PackedConfiguration(2, 4), new PackedConfiguration(3, 4)};

    public static PackedConfiguration belowStorage(int bits) {
        return BELOW_STORAGE[bits];
    }

    @Override
    public boolean alwaysRepack() {
        return true;
    }

    @Override
    public <T> Palette<T> createPalette(Strategy<T> strategy, List<T> paletteEntries) {
        return LinearPalette.create(this.bitsInMemory, paletteEntries);
    }
}
