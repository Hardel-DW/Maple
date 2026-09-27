package fr.hardel.mapple.optimisation.surface;

import java.util.function.IntSupplier;
import org.jspecify.annotations.Nullable;

/** One side of a range: a constant, or read from the column as offset + multiplier * source. */
record Limit(int offset, int multiplier, @Nullable IntSupplier source) {
    static final Limit LOWEST = constant(Integer.MIN_VALUE);
    static final Limit HIGHEST = constant(Integer.MAX_VALUE);

    static Limit constant(int value) {
        return new Limit(value, 0, null);
    }

    static Limit column(int offset, int multiplier, IntSupplier source) {
        return multiplier == 0 ? constant(offset) : new Limit(offset, multiplier, source);
    }

    boolean isConstant() {
        return this.source == null;
    }

    boolean isInfinite() {
        return this.equals(LOWEST) || this.equals(HIGHEST);
    }

    int value() {
        return this.source == null ? this.offset : this.offset + this.multiplier * this.source.getAsInt();
    }

    Limit shift(int delta) {
        return new Limit(this.offset + delta, this.multiplier, this.source);
    }

    /** The lower of two limits, or the fallback when they read different sources. */
    static Limit min(Limit first, Limit second, Limit fallback) {
        if (first.equals(HIGHEST) || second.equals(LOWEST)) {
            return second;
        }

        if (second.equals(HIGHEST) || first.equals(LOWEST)) {
            return first;
        }

        if (!first.comparable(second)) {
            return fallback;
        }

        return first.offset <= second.offset ? first : second;
    }

    /** The higher of two limits, or the fallback when they read different sources. */
    static Limit max(Limit first, Limit second, Limit fallback) {
        if (first.equals(LOWEST) || second.equals(HIGHEST)) {
            return second;
        }

        if (second.equals(LOWEST) || first.equals(HIGHEST)) {
            return first;
        }

        if (!first.comparable(second)) {
            return fallback;
        }

        return first.offset >= second.offset ? first : second;
    }

    private boolean comparable(Limit other) {
        return this.source == other.source && this.multiplier == other.multiplier;
    }
}
