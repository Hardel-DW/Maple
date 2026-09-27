package fr.hardel.mapple.optimisation.terrain;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jspecify.annotations.Nullable;

/**
 * The sections of a proto chunk its worker rewrites alone, each decoded into a builder on first use. Until build the sections keep their old content:
 * every read and write of the pass goes through here.
 */
public final class SectionBuilders {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ChunkAccess chunk;
    private final SectionBuilder[] builders;
    private final int minSectionY;

    public SectionBuilders(ChunkAccess chunk) {
        this.chunk = chunk;
        this.builders = new SectionBuilder[chunk.getSectionsCount()];
        this.minSectionY = chunk.getMinSectionY();
    }

    /** x and z inside the chunk, blockY inside the build height. */
    public BlockState get(int x, int blockY, int z) {
        SectionBuilder builder = reader(blockY);
        return builder == null ? AIR : builder.get(builder.index(x, SectionPos.sectionRelative(blockY), z));
    }

    public byte kind(int x, int blockY, int z) {
        SectionBuilder builder = reader(blockY);
        return builder == null ? BlockKind.AIR : builder.kind(builder.index(x, SectionPos.sectionRelative(blockY), z));
    }

    /** Tells whether the block changed. */
    public boolean set(int x, int blockY, int z, BlockState state) {
        SectionBuilder builder = at(blockY);
        return builder.set(builder.index(x, SectionPos.sectionRelative(blockY), z), state);
    }

    /** The builder of the section holding blockY, for a pass writing a run of blocks into it. */
    SectionBuilder at(int blockY) {
        return builder(index(blockY));
    }

    public void build() {
        for (SectionBuilder builder : this.builders) {
            if (builder != null) {
                builder.build();
            }
        }
    }

    /** From the minimum read once: the chunk's own lookup walks its height accessor on every block. */
    private int index(int blockY) {
        return SectionPos.blockToSectionCoord(blockY) - this.minSectionY;
    }

    /** None for a section of air never written: it reads as air without being decoded. */
    private @Nullable SectionBuilder reader(int blockY) {
        int index = index(blockY);
        if (this.builders[index] == null && this.chunk.getSection(index).hasOnlyAir()) {
            return null;
        }

        return builder(index);
    }

    private SectionBuilder builder(int index) {
        SectionBuilder builder = this.builders[index];
        if (builder == null) {
            builder = new SectionBuilder(this.chunk.getSection(index));
            this.builders[index] = builder;
        }

        return builder;
    }
}
