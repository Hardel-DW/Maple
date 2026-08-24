package fr.hardel.mapple.optimisation.light;

import java.util.Arrays;
import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;

public final class SparseLayer {
    private static final int SLAB_BYTES = DataLayer.LAYER_SIZE;
    private static final int SLAB_SHIFT = 8;
    private static final int NIBBLE_MASK = 15;

    private SparseLayer() {
    }

    public record Packed(int slabs, int defaultValue, byte @Nullable [] data) {
    }

    public static Packed compress(byte[] full) {
        int defaultValue = full[0] & NIBBLE_MASK;
        if (isFilled(full, 0, full.length, defaultValue)) {
            return new Packed(0, defaultValue, null);
        }

        int slabs = 0;
        for (int slab = 0; slab < DataLayer.LAYER_COUNT; slab++) {
            if (!isFilled(full, slab * SLAB_BYTES, (slab + 1) * SLAB_BYTES, 0)) {
                slabs |= 1 << slab;
            }
        }

        byte[] data = new byte[Integer.bitCount(slabs) * SLAB_BYTES];
        int position = 0;
        for (int slab = 0; slab < DataLayer.LAYER_COUNT; slab++) {
            if ((slabs & 1 << slab) != 0) {
                System.arraycopy(full, slab * SLAB_BYTES, data, position, SLAB_BYTES);
                position += SLAB_BYTES;
            }
        }

        return new Packed(slabs, 0, data);
    }

    public static int get(byte[] data, int slabs, int defaultValue, int index) {
        int bit = 1 << (index >> SLAB_SHIFT);
        if ((slabs & bit) == 0) {
            return defaultValue;
        }

        return data[offset(slabs, bit) + ((index & 255) >> 1)] >> ((index & 1) << 2) & NIBBLE_MASK;
    }

    public static void set(byte[] data, int slabs, int index, int value) {
        int position = offset(slabs, 1 << (index >> SLAB_SHIFT)) + ((index & 255) >> 1);
        int shift = (index & 1) << 2;
        data[position] = (byte) (data[position] & ~(NIBBLE_MASK << shift) | value << shift);
    }

    public static byte[] withSlab(byte @Nullable [] data, int slabs, int slab, int defaultValue) {
        int before = offset(slabs, 1 << slab);
        int length = data == null ? 0 : data.length;
        byte[] grown = new byte[length + SLAB_BYTES];
        if (data != null) {
            System.arraycopy(data, 0, grown, 0, before);
            System.arraycopy(data, before, grown, before + SLAB_BYTES, length - before);
        }

        Arrays.fill(grown, before, before + SLAB_BYTES, packed(defaultValue));
        return grown;
    }

    public static byte[] expand(byte @Nullable [] data, int slabs, int defaultValue) {
        byte[] full = new byte[DataLayer.SIZE];
        if (defaultValue != 0) {
            Arrays.fill(full, packed(defaultValue));
        }

        int position = 0;
        for (int slab = 0; slab < DataLayer.LAYER_COUNT; slab++) {
            if ((slabs & 1 << slab) != 0) {
                System.arraycopy(data, position, full, slab * SLAB_BYTES, SLAB_BYTES);
                position += SLAB_BYTES;
            }
        }

        return full;
    }

    private static int offset(int slabs, int bit) {
        return Integer.bitCount(slabs & (bit - 1)) * SLAB_BYTES;
    }

    private static byte packed(int value) {
        return (byte) (value | value << 4);
    }

    private static boolean isFilled(byte[] bytes, int from, int to, int value) {
        byte expected = packed(value);
        for (int i = from; i < to; i++) {
            if (bytes[i] != expected) {
                return false;
            }
        }

        return true;
    }
}
