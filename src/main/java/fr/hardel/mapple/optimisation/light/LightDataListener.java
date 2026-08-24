package fr.hardel.mapple.optimisation.light;

import it.unimi.dsi.fastutil.longs.LongSet;

public interface LightDataListener {
    void mapple$onLightDataChanged(LongSet chunkKeys);
}
