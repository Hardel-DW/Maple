package fr.hardel.mapple.optimisation.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PersistentLongMapTest {
    @Test
    void matchesAHashMapUnderRandomOperations() {
        Random random = new Random(1234567890L);
        Map<Long, String> reference = new HashMap<>();
        PersistentLongMap<String> map = PersistentLongMap.empty();
        for (int step = 0; step < 200_000; step++) {
            long key = (random.nextInt(4096) - 2048L) << 20 | random.nextInt(24);
            if (random.nextInt(3) == 0) {
                reference.remove(key);
                map = map.without(key);
            } else {
                String value = "v" + step;
                reference.put(key, value);
                map = map.with(key, value);
            }

            assertEquals(reference.size(), map.size());
            assertEquals(reference.get(key), map.get(key));
        }

        for (Map.Entry<Long, String> entry : reference.entrySet()) {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }

        Set<String> values = new HashSet<>();
        map.forEach(values::add);
        assertEquals(new HashSet<>(reference.values()), values);
    }

    @Test
    void olderVersionsAreUntouched() {
        PersistentLongMap<String> empty = PersistentLongMap.empty();
        PersistentLongMap<String> one = empty.with(7L, "a");
        PersistentLongMap<String> two = one.with(7L, "b").with(-7L, "c");
        assertNull(empty.get(7L));
        assertEquals("a", one.get(7L));
        assertNull(one.get(-7L));
        assertEquals("b", two.get(7L));
        assertEquals("c", two.get(-7L));
        assertSame(two, two.without(99L));
        assertEquals(0, two.without(7L).without(-7L).size());
    }
}
