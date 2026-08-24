package fr.hardel.mapple.optimisation.persistent;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

public final class PersistentHolderMap<V> extends Long2ObjectLinkedOpenHashMap<V> {
    private PersistentLongMap<V> entries;

    public PersistentHolderMap() {
        this(PersistentLongMap.empty());
    }

    private PersistentHolderMap(PersistentLongMap<V> entries) {
        super(0);
        this.entries = entries;
    }

    @Override
    public @Nullable V get(long key) {
        return this.entries.get(key);
    }

    @Override
    public boolean containsKey(long key) {
        return this.entries.get(key) != null;
    }

    @Override
    public @Nullable V put(long key, V value) {
        V previous = this.entries.get(key);
        this.entries = this.entries.with(key, value);
        return previous;
    }

    @Override
    public @Nullable V remove(long key) {
        V previous = this.entries.get(key);
        this.entries = this.entries.without(key);
        return previous;
    }

    @Override
    public int size() {
        return this.entries.size();
    }

    @Override
    public boolean isEmpty() {
        return this.entries.size() == 0;
    }

    @Override
    public ObjectCollection<V> values() {
        return new Values();
    }

    @Override
    public Long2ObjectSortedMap.FastSortedEntrySet<V> long2ObjectEntrySet() {
        return this.materialize().long2ObjectEntrySet();
    }

    @Override
    public LongSortedSet keySet() {
        return this.materialize().keySet();
    }

    @Override
    public PersistentHolderMap<V> clone() {
        return new PersistentHolderMap<>(this.entries);
    }

    private Long2ObjectLinkedOpenHashMap<V> materialize() {
        Long2ObjectLinkedOpenHashMap<V> copy = new Long2ObjectLinkedOpenHashMap<>(this.entries.size());
        for (PersistentLongMap.Entry<V> entry : this.entries) {
            copy.put(entry.key(), entry.value());
        }

        return copy;
    }

    private final class Values extends AbstractObjectCollection<V> {
        @Override
        public ObjectIterator<V> iterator() {
            Iterator<PersistentLongMap.Entry<V>> entries = PersistentHolderMap.this.entries.iterator();
            return new ObjectIterator<>() {
                @Override
                public boolean hasNext() {
                    return entries.hasNext();
                }

                @Override
                public V next() {
                    return entries.next().value();
                }
            };
        }

        @Override
        public int size() {
            return PersistentHolderMap.this.entries.size();
        }
    }
}
