package fr.hardel.mapple.optimisation.persistent;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public final class PersistentTopSections extends Long2IntOpenHashMap {
    private PersistentLongMap<Integer> sections;
    private int defaultValue;

    public PersistentTopSections() {
        this(PersistentLongMap.empty(), 0);
    }

    private PersistentTopSections(PersistentLongMap<Integer> sections, int defaultValue) {
        super(0);
        this.sections = sections;
        this.defaultValue = defaultValue;
    }

    @Override
    public int get(long key) {
        Integer value = this.sections.get(key);
        return value == null ? this.defaultValue : value;
    }

    @Override
    public boolean containsKey(long key) {
        return this.sections.get(key) != null;
    }

    @Override
    public int put(long key, int value) {
        int previous = this.get(key);
        this.sections = this.sections.with(key, value);
        return previous;
    }

    @Override
    public int remove(long key) {
        int previous = this.get(key);
        this.sections = this.sections.without(key);
        return previous;
    }

    @Override
    public int size() {
        return this.sections.size();
    }

    @Override
    public boolean isEmpty() {
        return this.sections.size() == 0;
    }

    @Override
    public void defaultReturnValue(int value) {
        this.defaultValue = value;
    }

    @Override
    public int defaultReturnValue() {
        return this.defaultValue;
    }

    @Override
    public PersistentTopSections clone() {
        return new PersistentTopSections(this.sections, this.defaultValue);
    }
}
