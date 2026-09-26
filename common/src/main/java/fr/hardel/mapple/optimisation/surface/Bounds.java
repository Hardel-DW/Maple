package fr.hardel.mapple.optimisation.surface;

import java.util.Arrays;

/**
 * A box over the variables of a stone block: its y, its stone depth above and its stone depth below. Each axis has a lower then an upper limit, both
 * inclusive. The box of a rule holds every block it may fire on, a box may hold more than that but never less.
 */
final class Bounds {
    static final int Y = 0;
    static final int ABOVE = 2;
    static final int BELOW = 4;
    static final Bounds ALL = new Bounds(new Limit[] { Limit.LOWEST, Limit.HIGHEST, Limit.LOWEST, Limit.HIGHEST, Limit.LOWEST, Limit.HIGHEST });
    static final Bounds NONE = new Bounds(new Limit[] { Limit.HIGHEST, Limit.LOWEST, Limit.HIGHEST, Limit.LOWEST, Limit.HIGHEST, Limit.LOWEST });

    private final Limit[] limits;

    private Bounds(Limit[] limits) {
        this.limits = limits;
    }

    static Bounds atLeast(int axis, Limit limit) {
        return ALL.with(axis, limit);
    }

    static Bounds atMost(int axis, Limit limit) {
        return ALL.with(axis + 1, limit);
    }

    Limit limit(int index) {
        return this.limits[index];
    }

    Bounds meet(Bounds other) {
        Limit[] result = new Limit[6];
        for (int axis = 0; axis < 6; axis += 2) {
            result[axis] = Limit.max(this.limits[axis], other.limits[axis], this.limits[axis]);
            result[axis + 1] = Limit.min(this.limits[axis + 1], other.limits[axis + 1], this.limits[axis + 1]);
        }

        return new Bounds(result);
    }

    Bounds hull(Bounds other) {
        Limit[] result = new Limit[6];
        for (int axis = 0; axis < 6; axis += 2) {
            result[axis] = Limit.min(this.limits[axis], other.limits[axis], Limit.LOWEST);
            result[axis + 1] = Limit.max(this.limits[axis + 1], other.limits[axis + 1], Limit.HIGHEST);
        }

        return new Bounds(result);
    }

    /**
     * The blocks outside this box. Only a box limited on one side has a box as complement: otherwise the outer complement opens to every block and the
     * inner one closes to none, both still true of the blocks they claim.
     */
    Bounds complement(boolean outer) {
        if (isNone()) {
            return ALL;
        }

        int finite = -1;
        for (int index = 0; index < 6; index++) {
            if (this.limits[index].isInfinite()) {
                continue;
            }

            if (finite >= 0) {
                return outer ? ALL : NONE;
            }

            finite = index;
        }

        if (finite < 0) {
            return NONE;
        }

        boolean lower = finite % 2 == 0;
        return ALL.with(lower ? finite + 1 : finite - 1, this.limits[finite].shift(lower ? -1 : 1));
    }

    boolean isAll() {
        return this.equals(ALL);
    }

    /** Empty whatever the column: some axis has constant limits the wrong way around. */
    boolean isNone() {
        for (int axis = 0; axis < 6; axis += 2) {
            Limit lower = this.limits[axis];
            Limit upper = this.limits[axis + 1];
            if (lower.isConstant() && upper.isConstant() && lower.offset() > upper.offset()) {
                return true;
            }
        }

        return false;
    }

    boolean readsColumn() {
        for (Limit limit : this.limits) {
            if (!limit.isConstant()) {
                return true;
            }
        }

        return false;
    }

    private Bounds with(int index, Limit limit) {
        Limit[] result = this.limits.clone();
        result[index] = limit;
        return new Bounds(result);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Bounds bounds && Arrays.equals(this.limits, bounds.limits);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.limits);
    }
}
