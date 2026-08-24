package fr.hardel.mapple.optimisation.light;

import fr.hardel.mapple.optimisation.persistent.PersistentLongMap;
import net.minecraft.world.level.chunk.DataLayer;

public interface LayerMapAccess {
    PersistentLongMap<DataLayer> mapple$layers();

    void mapple$share(PersistentLongMap<DataLayer> layers);
}
