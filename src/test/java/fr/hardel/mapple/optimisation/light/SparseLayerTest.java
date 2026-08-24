package fr.hardel.mapple.optimisation.light;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.world.level.chunk.DataLayer;
import org.junit.jupiter.api.Test;

class SparseLayerTest {
    private static final int NIBBLES = DataLayer.SIZE * 2;

    @Test
    void uniformLayersCompressToTheirValue() {
        byte[] full = new byte[DataLayer.SIZE];
        Arrays.fill(full, (byte) 0xFF);
        SparseLayer.Packed packed = SparseLayer.compress(full);
        assertNull(packed.data());
        assertEquals(15, packed.defaultValue());
        assertEquals(0, packed.slabs());
    }

    @Test
    void matchesAFlatLayerUnderRandomWrites() {
        Random random = new Random(1234567890L);
        for (int round = 0; round < 200; round++) {
            byte[] flat = new byte[DataLayer.SIZE];
            int litSlabs = random.nextInt(1 << DataLayer.LAYER_COUNT);
            for (int index = 0; index < NIBBLES; index++) {
                if ((litSlabs & 1 << (index >> 8)) != 0) {
                    writeFlat(flat, index, random.nextInt(16));
                }
            }

            SparseLayer.Packed packed = SparseLayer.compress(flat);
            byte[] data = packed.data();
            int slabs = packed.slabs();
            int defaultValue = packed.defaultValue();
            assertArrayEquals(flat, SparseLayer.expand(data, slabs, defaultValue));

            for (int step = 0; step < 2000; step++) {
                int index = random.nextInt(NIBBLES);
                int value = random.nextInt(16);
                writeFlat(flat, index, value);
                int slab = index >> 8;
                if ((slabs & 1 << slab) == 0) {
                    if (value == defaultValue) {
                        continue;
                    }

                    data = SparseLayer.withSlab(data, slabs, slab, defaultValue);
                    slabs |= 1 << slab;
                }

                SparseLayer.set(data, slabs, index, value);
            }

            for (int index = 0; index < NIBBLES; index++) {
                assertEquals(readFlat(flat, index), SparseLayer.get(data, slabs, defaultValue, index));
            }

            assertArrayEquals(flat, SparseLayer.expand(data, slabs, defaultValue));
        }
    }

    private static void writeFlat(byte[] flat, int index, int value) {
        int position = index >> 1;
        int shift = (index & 1) << 2;
        flat[position] = (byte) (flat[position] & ~(15 << shift) | value << shift);
    }

    private static int readFlat(byte[] flat, int index) {
        return flat[index >> 1] >> ((index & 1) << 2) & 15;
    }
}
