package fr.hardel.maple.optimisation.surface;

import fr.hardel.maple.optimisation.terrain.BlockKind;
import fr.hardel.maple.optimisation.terrain.SectionBuilders;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's surface pass with its loop as is. The column reads and writes the sections through builders rebuilt at the end, the biome lookup is zoomed
 * per chunk and the rules are compiled with the bounds of the blocks each one can fire on.
 */
public final class SurfacePass {
    private static final EnumSet<Heightmap.Types> WORLDGEN_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);

    private final MaterialSystem system;
    private final ProtoChunk chunk;
    private final int minY;
    private final int maxY;
    private final SectionBuilders sections;
    private final SurfaceColumn column;
    private final BiomeZoom biomes;
    private final MaterialRuleContext context;
    private final SurfaceRules rules;
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    public SurfacePass(MaterialSystem system, RandomState randomState, BiomeManager biomeManager, WorldGenerationContext generationContext, ProtoChunk chunk,
        NoiseChunk noiseChunk, MaterialRule ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes) {
        this.system = system;
        this.chunk = chunk;
        LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
        this.minY = heightAccessor.getMinY();
        this.maxY = heightAccessor.getMaxY();
        this.sections = new SectionBuilders(chunk);
        this.column = new SurfaceColumn(chunk, this.sections, this.minY, this.maxY);
        this.biomes = new BiomeZoom(biomeManager, chunk);
        DensityVolume volume = narrowedVolume(chunk, noiseChunk.volume());
        this.context = new MaterialRuleContext(system, randomState, volume, noiseChunk.cachingSamplers(), this.biomes::get, generationContext, possibleBiomes);
        this.rules = new RuleCompiler(this.context, randomState, noiseChunk.cachingSamplers(), volume, chunk.getMinY()).compile(ruleSource);
    }

    /** A proto chunk before light, whose writes update the worldgen heightmaps it already holds, zoomed by vanilla's biome manager. */
    public static boolean applies(ProtoChunk chunk, BiomeManager biomeManager) {
        ChunkStatus status = chunk.getPersistedStatus();
        return chunk.getClass() == ProtoChunk.class && biomeManager.getClass() == BiomeManager.class && !status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)
            && status.heightmapsAfter().equals(WORLDGEN_HEIGHTMAPS) && chunk.hasPrimedHeightmap(Heightmap.Types.OCEAN_FLOOR_WG)
            && chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    }

    public void run() {
        try (this.rules) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    surface(x, z);
                }
            }
        }

        this.sections.build();
    }

    private void surface(int x, int z) {
        int blockX = this.chunk.getPos().getMinBlockX() + x;
        int blockZ = this.chunk.getPos().getMinBlockZ() + z;
        int startingHeight = this.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
        this.column.moveTo(blockX, blockZ);
        Holder<Biome> surfaceBiome = this.biomes.get(this.blockPos.set(blockX, startingHeight, blockZ));
        if (surfaceBiome.is(Biomes.ERODED_BADLANDS)) {
            this.system.erodedBadlandsExtension(this.column, blockX, blockZ, startingHeight, this.chunk);
        }

        int height = this.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
        this.context.updateXZ(blockX, blockZ, MaterialSystem.getSurfaceGradientX(this.chunk, x, z), MaterialSystem.getSurfaceGradientZ(this.chunk, x, z));
        this.rules.moveTo(blockX, blockZ, height);
        applyRules(blockX, blockZ, height);
        if (surfaceBiome.is(Biomes.FROZEN_OCEAN) || surfaceBiome.is(Biomes.DEEP_FROZEN_OCEAN)) {
            this.system.frozenOceanExtension(this.context.getMinSurfaceLevel(), surfaceBiome.value(), this.column, this.blockPos, blockX, blockZ, startingHeight);
        }
    }

    private void applyRules(int blockX, int blockZ, int height) {
        int stoneAboveDepth = 0;
        int waterHeight = Integer.MIN_VALUE;
        int nextCeilingStoneY = Integer.MAX_VALUE;
        int endY = this.chunk.getMinY();
        for (int y = height; y >= endY; y--) {
            byte kind = this.column.kind(y);
            if (kind == BlockKind.AIR) {
                stoneAboveDepth = 0;
                waterHeight = Integer.MIN_VALUE;
            } else if (kind == BlockKind.FLUID) {
                if (waterHeight == Integer.MIN_VALUE) {
                    waterHeight = y + 1;
                }
            } else {
                if (nextCeilingStoneY >= y) {
                    nextCeilingStoneY = nextCeilingStone(y, endY);
                }

                stoneAboveDepth++;
                this.context.updateY(stoneAboveDepth, y - nextCeilingStoneY + 1, waterHeight, y);
                if (y >= this.minY && y <= this.maxY) {
                    BlockState state = this.rules.tryApply(blockX, y, blockZ);
                    if (state != null) {
                        this.column.setBlock(y, state);
                    }
                }
            }
        }
    }

    private int nextCeilingStone(int y, int endY) {
        for (int lookaheadY = y - 1; lookaheadY >= endY - 1; lookaheadY--) {
            if (this.column.kind(lookaheadY) != BlockKind.SOLID) {
                return lookaheadY + 1;
            }
        }

        return DimensionType.WAY_BELOW_MIN_Y;
    }

    private static DensityVolume narrowedVolume(ProtoChunk chunk, DensityVolume fullVolume) {
        int maxBlockY = chunk.getSectionYFromSectionIndex(chunk.getHighestFilledSectionIndex()) * 16 + 15;
        return new DensityVolume(fullVolume.sizeX(), Math.max(maxBlockY - fullVolume.minBlockY() + 1, 1), fullVolume.sizeZ(), fullVolume.minBlockX(),
            fullVolume.minBlockY(), fullVolume.minBlockZ());
    }
}
