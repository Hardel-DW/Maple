package fr.hardel.maple.optimisation.surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * BiomeManager.getBiome over one chunk: each quart is fetched once, and the zoom is skipped when the 8 corners hold the same biome, since vanilla returns
 * one of them. Otherwise the zoom runs as vanilla's, on the same fiddled distances.
 */
final class BiomeZoom {
    private final BiomeManager manager;
    private final int minQuartX;
    private final int minQuartY;
    private final int minQuartZ;
    private final int sizeY;
    private final List<Holder<Biome>> quarts;

    BiomeZoom(BiomeManager manager, ChunkAccess chunk) {
        this.manager = manager;
        this.minQuartX = QuartPos.fromBlock(chunk.getPos().getMinBlockX() - 2);
        this.minQuartY = QuartPos.fromBlock(chunk.getMinY() - 2);
        this.minQuartZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ() - 2);
        this.sizeY = QuartPos.fromBlock(chunk.getMaxY() - 1) - this.minQuartY + 2;
        this.quarts = new ArrayList<>(Collections.nCopies(6 * 6 * this.sizeY, null));
    }

    Holder<Biome> get(BlockPos pos) {
        int absX = pos.getX() - 2;
        int absY = pos.getY() - 2;
        int absZ = pos.getZ() - 2;
        int parentX = absX >> 2;
        int parentY = absY >> 2;
        int parentZ = absZ >> 2;
        if (!contains(parentX, parentY, parentZ)) {
            return this.manager.getBiome(pos);
        }

        Holder<Biome> first = quart(parentX, parentY, parentZ);
        if (cornersHold(first, parentX, parentY, parentZ)) {
            return first;
        }

        double fractX = (absX & 3) / 4.0;
        double fractY = (absY & 3) / 4.0;
        double fractZ = (absZ & 3) / 4.0;
        int minI = 0;
        double minFiddledDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            boolean xEven = (i & 4) == 0;
            boolean yEven = (i & 2) == 0;
            boolean zEven = (i & 1) == 0;
            double next = BiomeManager.getFiddledDistance(this.manager.biomeZoomSeed, xEven ? parentX : parentX + 1, yEven ? parentY : parentY + 1,
                zEven ? parentZ : parentZ + 1, xEven ? fractX : fractX - 1.0, yEven ? fractY : fractY - 1.0, zEven ? fractZ : fractZ - 1.0);
            if (minFiddledDistance > next) {
                minI = i;
                minFiddledDistance = next;
            }
        }

        return quart((minI & 4) == 0 ? parentX : parentX + 1, (minI & 2) == 0 ? parentY : parentY + 1, (minI & 1) == 0 ? parentZ : parentZ + 1);
    }

    private boolean contains(int parentX, int parentY, int parentZ) {
        int x = parentX - this.minQuartX;
        int y = parentY - this.minQuartY;
        int z = parentZ - this.minQuartZ;
        return x >= 0 && x < 5 && y >= 0 && y < this.sizeY - 1 && z >= 0 && z < 5;
    }

    private boolean cornersHold(Holder<Biome> biome, int parentX, int parentY, int parentZ) {
        for (int i = 1; i < 8; i++) {
            if (quart(parentX + (i >> 2 & 1), parentY + (i >> 1 & 1), parentZ + (i & 1)) != biome) {
                return false;
            }
        }

        return true;
    }

    private Holder<Biome> quart(int quartX, int quartY, int quartZ) {
        int index = ((quartZ - this.minQuartZ) * 6 + quartX - this.minQuartX) * this.sizeY + quartY - this.minQuartY;
        Holder<Biome> biome = this.quarts.get(index);
        if (biome == null) {
            biome = this.manager.noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
            this.quarts.set(index, biome);
        }

        return biome;
    }
}
