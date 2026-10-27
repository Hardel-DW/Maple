package fr.hardel.maple.optimisation.section;

import net.minecraft.util.ThreadingDetector;

/** Lithium's no_locking: the check only catches a misbehaving mod, and costs a lock per write. One instance serves every container. */
public final class NoCheckThreadingDetector extends ThreadingDetector {
    public NoCheckThreadingDetector() {
        super("PalettedContainer");
    }

    @Override
    public void checkAndLock() {
    }

    @Override
    public void checkAndUnlock() {
    }
}
