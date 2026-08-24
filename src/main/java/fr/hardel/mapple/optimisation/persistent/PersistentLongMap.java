package fr.hardel.mapple.optimisation.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public final class PersistentLongMap<V> implements Iterable<PersistentLongMap.Entry<V>> {
    private static final int BITS = 5;
    private static final int MASK = 31;
    private static final int MAX_DEPTH = Long.SIZE / BITS + 1;

    private final Node<V> root;
    private final int size;

    private PersistentLongMap(Node<V> root, int size) {
        this.root = root;
        this.size = size;
    }

    public static <V> PersistentLongMap<V> empty() {
        return new PersistentLongMap<>(Node.empty(), 0);
    }

    public int size() {
        return this.size;
    }

    public @Nullable V get(long key) {
        long hash = mix(key);
        Node<V> node = this.root;
        for (int shift = 0; ; shift += BITS) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            if ((node.bitmap & bit) == 0) {
                return null;
            }

            switch (node.slots[Integer.bitCount(node.bitmap & (bit - 1))]) {
                case Entry<V> entry -> {
                    return entry.key == key ? entry.value : null;
                }
                case Node<V> child -> node = child;
            }
        }
    }

    public PersistentLongMap<V> with(long key, V value) {
        int added = this.get(key) == null ? 1 : 0;
        return new PersistentLongMap<>(this.root.with(new Entry<>(key, value), mix(key), 0), this.size + added);
    }

    public PersistentLongMap<V> without(long key) {
        Slot<V> result = this.root.without(key, mix(key), 0);
        if (result == this.root) {
            return this;
        }

        Node<V> root = switch (result) {
            case Node<V> node -> node;
            case Entry<V> entry -> Node.<V>empty().with(entry, mix(entry.key), 0);
        };
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

    private sealed interface Slot<V> permits Node, Entry {
        @SuppressWarnings("unchecked")
        static <V> Slot<V>[] array(int length) {
            return (Slot<V>[]) new Slot[length];
        }
    }

    public record Entry<V>(long key, V value) implements Slot<V> {
    }

    private static final class Node<V> implements Slot<V> {
        private final int bitmap;
        private final Slot<V>[] slots;

        private Node(int bitmap, Slot<V>[] slots) {
            this.bitmap = bitmap;
            this.slots = slots;
        }

        private static <V> Node<V> empty() {
            return new Node<>(0, Slot.array(0));
        }

        private Node<V> with(Entry<V> entry, long hash, int shift) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            int index = Integer.bitCount(this.bitmap & (bit - 1));
            if ((this.bitmap & bit) == 0) {
                Slot<V>[] slots = Slot.array(this.slots.length + 1);
                System.arraycopy(this.slots, 0, slots, 0, index);
                slots[index] = entry;
                System.arraycopy(this.slots, index, slots, index + 1, this.slots.length - index);
                return new Node<>(this.bitmap | bit, slots);
            }

            Slot<V> replacement = switch (this.slots[index]) {
                case Node<V> child -> child.with(entry, hash, shift + BITS);
                case Entry<V> existing -> existing.key == entry.key ? entry : Node.<V>empty().with(existing, mix(existing.key), shift + BITS).with(entry, hash, shift + BITS);
            };
            Slot<V>[] slots = this.slots.clone();
            slots[index] = replacement;
            return new Node<>(this.bitmap, slots);
        }

        private Slot<V> without(long key, long hash, int shift) {
            int bit = 1 << ((int) (hash >>> shift) & MASK);
            if ((this.bitmap & bit) == 0) {
                return this;
            }

            int index = Integer.bitCount(this.bitmap & (bit - 1));
            return switch (this.slots[index]) {
                case Node<V> child -> this.withoutInChild(child, index, key, hash, shift);
                case Entry<V> entry -> entry.key == key ? this.withoutSlot(index, bit) : this;
            };
        }

        private Slot<V> withoutInChild(Node<V> child, int index, long key, long hash, int shift) {
            Slot<V> replacement = child.without(key, hash, shift + BITS);
            if (replacement == child) {
                return this;
            }

            if (replacement instanceof Entry<V> && this.slots.length == 1) {
                return replacement;
            }

            Slot<V>[] slots = this.slots.clone();
            slots[index] = replacement;
            return new Node<>(this.bitmap, slots);
        }

        private Slot<V> withoutSlot(int index, int bit) {
            if (this.slots.length == 2 && this.slots[1 - index] instanceof Entry<V> remaining) {
                return remaining;
            }

            Slot<V>[] slots = Slot.array(this.slots.length - 1);
            System.arraycopy(this.slots, 0, slots, 0, index);
            System.arraycopy(this.slots, index + 1, slots, index, slots.length - index);
            return new Node<>(this.bitmap & ~bit, slots);
        }
    }

    private static final class EntryIterator<V> implements Iterator<Entry<V>> {
        private final Slot<V>[] path = Slot.array(MAX_DEPTH);
        private final int[] positions = new int[MAX_DEPTH];
        private int depth;
        private @Nullable Entry<V> next;

        private EntryIterator(Node<V> root) {
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

        private void advance() {
            while (this.depth >= 0) {
                Node<V> node = (Node<V>) this.path[this.depth];
                int position = this.positions[this.depth];
                if (position == node.slots.length) {
                    this.depth--;
                    continue;
                }

                this.positions[this.depth] = position + 1;
                switch (node.slots[position]) {
                    case Node<V> child -> {
                        this.depth++;
                        this.path[this.depth] = child;
                        this.positions[this.depth] = 0;
                    }
                    case Entry<V> entry -> {
                        this.next = entry;
                        return;
                    }
                }
            }

            this.next = null;
        }
    }
}
