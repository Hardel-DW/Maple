package fr.hardel.mapple.optimisation.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public final class PersistentLongMap<V> implements Iterable<PersistentLongMap.Entry<V>> {
    private static final Node EMPTY_NODE = new Node(0, new Object[0]);
    private static final PersistentLongMap<?> EMPTY = new PersistentLongMap<>(EMPTY_NODE, 0);
    private static final int BITS = 5;
    private static final int MASK = 31;
    private static final int MAX_DEPTH = Long.SIZE / BITS + 1;

    private final Node root;
    private final int size;

    private PersistentLongMap(Node root, int size) {
        this.root = root;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <V> PersistentLongMap<V> empty() {
        return (PersistentLongMap<V>) EMPTY;
    }

    public int size() {
        return this.size;
    }

    @SuppressWarnings("unchecked")
    public @Nullable V get(long key) {
        long hash = mix(key);
        Node node = this.root;
        for (int shift = 0; ; shift += BITS) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            if ((node.bitmap & bit) == 0) {
                return null;
            }

            Object slot = node.slots[Integer.bitCount(node.bitmap & (bit - 1))];
            if (slot instanceof Entry<?> entry) {
                return entry.key == key ? (V) entry.value : null;
            }

            node = (Node) slot;
        }
    }

    public PersistentLongMap<V> with(long key, V value) {
        Insertion insertion = new Insertion();
        Node root = this.root.with(new Entry<>(key, value), mix(key), 0, insertion);
        return new PersistentLongMap<>(root, this.size + (insertion.added ? 1 : 0));
    }

    public PersistentLongMap<V> without(long key) {
        Object result = this.root.without(key, mix(key), 0);
        if (result == this.root) {
            return this;
        }

        Node root = result instanceof Node node ? node : EMPTY_NODE.with((Entry<?>) result, mix(((Entry<?>) result).key), 0, new Insertion());
        return new PersistentLongMap<>(root, this.size - 1);
    }

    @Override
    public Iterator<Entry<V>> iterator() {
        return new EntryIterator<>(this.root);
    }

    private static long mix(long key) {
        key = (key ^ (key >>> 30)) * 0xBF58476D1CE4E5B9L;
        key = (key ^ (key >>> 27)) * 0x94D049BB133111EBL;
        return key ^ (key >>> 31);
    }

    public record Entry<V>(long key, V value) {
    }

    private static final class Insertion {
        private boolean added;
    }

    private static final class Node {
        private final int bitmap;
        private final Object[] slots;

        private Node(int bitmap, Object[] slots) {
            this.bitmap = bitmap;
            this.slots = slots;
        }

        private Node with(Entry<?> entry, long hash, int shift, Insertion insertion) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            int index = Integer.bitCount(this.bitmap & (bit - 1));
            if ((this.bitmap & bit) == 0) {
                Object[] slots = new Object[this.slots.length + 1];
                System.arraycopy(this.slots, 0, slots, 0, index);
                slots[index] = entry;
                System.arraycopy(this.slots, index, slots, index + 1, this.slots.length - index);
                insertion.added = true;
                return new Node(this.bitmap | bit, slots);
            }

            Object slot = this.slots[index];
            Object replacement;
            if (slot instanceof Node child) {
                replacement = child.with(entry, hash, shift + BITS, insertion);
            } else if (((Entry<?>) slot).key == entry.key) {
                replacement = entry;
            } else {
                Entry<?> existing = (Entry<?>) slot;
                replacement = EMPTY_NODE.with(existing, mix(existing.key), shift + BITS, new Insertion()).with(entry, hash, shift + BITS, insertion);
            }

            Object[] slots = this.slots.clone();
            slots[index] = replacement;
            return new Node(this.bitmap, slots);
        }

        private Object without(long key, long hash, int shift) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            if ((this.bitmap & bit) == 0) {
                return this;
            }

            int index = Integer.bitCount(this.bitmap & (bit - 1));
            Object slot = this.slots[index];
            if (slot instanceof Node child) {
                Object replacement = child.without(key, hash, shift + BITS);
                if (replacement == child) {
                    return this;
                }

                if (replacement instanceof Entry<?> && this.slots.length == 1) {
                    return replacement;
                }

                Object[] slots = this.slots.clone();
                slots[index] = replacement;
                return new Node(this.bitmap, slots);
            }

            if (((Entry<?>) slot).key != key) {
                return this;
            }

            if (this.slots.length == 2 && this.slots[1 - index] instanceof Entry<?> remaining) {
                return remaining;
            }

            Object[] slots = new Object[this.slots.length - 1];
            System.arraycopy(this.slots, 0, slots, 0, index);
            System.arraycopy(this.slots, index + 1, slots, index, slots.length - index);
            return new Node(this.bitmap & ~bit, slots);
        }
    }

    private static final class EntryIterator<V> implements Iterator<Entry<V>> {
        private final Node[] path = new Node[MAX_DEPTH];
        private final int[] positions = new int[MAX_DEPTH];
        private int depth;
        private @Nullable Entry<V> next;

        private EntryIterator(Node root) {
            this.path[0] = root;
            this.advance();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public Entry<V> next() {
            Entry<V> current = this.next;
            if (current == null) {
                throw new NoSuchElementException();
            }

            this.advance();
            return current;
        }

        @SuppressWarnings("unchecked")
        private void advance() {
            while (this.depth >= 0) {
                Node node = this.path[this.depth];
                int position = this.positions[this.depth];
                if (position == node.slots.length) {
                    this.depth--;
                    continue;
                }

                this.positions[this.depth] = position + 1;
                Object slot = node.slots[position];
                if (slot instanceof Node child) {
                    this.depth++;
                    this.path[this.depth] = child;
                    this.positions[this.depth] = 0;
                } else {
                    this.next = (Entry<V>) slot;
                    return;
                }
            }

            this.next = null;
        }
    }
}
