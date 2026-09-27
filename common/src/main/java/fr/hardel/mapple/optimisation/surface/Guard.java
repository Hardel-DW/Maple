package fr.hardel.mapple.optimisation.surface;

/** Bounds as plain ints for the test of each block, resolved again at each column when a limit reads the column. */
final class Guard {
    private final Bounds bounds;
    private int minY;
    private int maxY;
    private int minAbove;
    private int maxAbove;
    private int minBelow;
    private int maxBelow;

    Guard(Bounds bounds) {
        this.bounds = bounds;
    }

    void resolve() {
        this.minY = this.bounds.limit(Bounds.Y).value();
        this.maxY = this.bounds.limit(Bounds.Y + 1).value();
        this.minAbove = this.bounds.limit(Bounds.ABOVE).value();
        this.maxAbove = this.bounds.limit(Bounds.ABOVE + 1).value();
        this.minBelow = this.bounds.limit(Bounds.BELOW).value();
        this.maxBelow = this.bounds.limit(Bounds.BELOW + 1).value();
    }

    boolean contains(int y, int above, int below) {
        return y >= this.minY && y <= this.maxY && above >= this.minAbove && above <= this.maxAbove && below >= this.minBelow && below <= this.maxBelow;
    }
}
