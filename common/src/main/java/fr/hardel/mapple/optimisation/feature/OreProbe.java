package fr.hardel.mapple.optimisation.feature;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Whether some column of an ore's probe square has its ocean floor at or above the ore's bottom, the test vanilla runs column by column. A chunk whose
 * highest floor stays below answers for all its columns; only the others are probed, and the answer is the same.
 */
public final class OreProbe {
    private OreProbe() {
    }

    public static boolean reaches(WorldGenRegion region, int xStart, int zStart, int size, int yStart) {
        int xEnd = xStart + size;
        int zEnd = zStart + size;
        for (int chunkX = SectionPos.blockToSectionCoord(xStart); chunkX <= SectionPos.blockToSectionCoord(xEnd); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(zStart); chunkZ <= SectionPos.blockToSectionCoord(zEnd); chunkZ++) {
                if (((OceanFloorPeak) region.getChunk(chunkX, chunkZ)).mapple$oceanFloorPeak() >= yStart
                    && columnReaches(region, Math.max(xStart, SectionPos.sectionToBlockCoord(chunkX)), Math.min(xEnd, SectionPos.sectionToBlockCoord(chunkX, 15)),
                    Math.max(zStart, SectionPos.sectionToBlockCoord(chunkZ)), Math.min(zEnd, SectionPos.sectionToBlockCoord(chunkZ, 15)), yStart)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean columnReaches(WorldGenRegion region, int xMin, int xMax, int zMin, int zMax, int yStart) {
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                if (yStart <= region.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)) {
                    return true;
                }
            }
        }

        return false;
    }
}
