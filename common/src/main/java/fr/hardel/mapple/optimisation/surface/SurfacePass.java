package fr.hardel.mapple.optimisation.surface;

import fr.hardel.mapple.optimisation.terrain.BlockKind;
import fr.hardel.mapple.optimisation.terrain.SectionBuilders;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
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
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

/**
 * Vanilla's surface pass with its loop as is, the rules running on the default block only. The column reads and writes the sections through builders
 * rebuilt at the end, the biome lookup is zoomed per chunk and the rules are compiled with the bounds of the blocks each one can fire on.
 */
public final class SurfacePass {
    private static final EnumSet<Heightmap.Types> WORLDGEN_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);

    private final SurfaceSystem system;
    private final ProtoChunk chunk;
    private final boolean useLegacyRandom;
    private final SectionBuilders sections;
    private final SurfaceColumn column;
    private final BiomeZoom biomes;
    private final SurfaceRules.Context context;
    private final CompiledRules rules;
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    public SurfacePass(SurfaceSystem system, RandomState randomState, BiomeManager biomeManager, Registry<Biome> biomeRegistry, boolean useLegacyRandom,
        WorldGenerationContext generationContext, ProtoChunk chunk, NoiseChunk noiseChunk, SurfaceRules.RuleSource ruleSource) {
        this.system = system;
        this.chunk = chunk;
        this.useLegacyRandom = useLegacyRandom;
        LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
        this.sections = new SectionBuilders(chunk);
        this.column = new SurfaceColumn(chunk, this.sections, heightAccessor.getMinY(), heightAccessor.getMaxY());
        this.biomes = new BiomeZoom(biomeManager, chunk);
        this.context = new SurfaceRules.Context(system, randomState, chunk, noiseChunk, this.biomes::get, biomeRegistry, generationContext);
        this.rules = new RuleCompiler(this.context, randomState).compile(ruleSource);
    }

    /** A proto chunk before light, whose writes update the worldgen heightmaps it already holds, zoomed by vanilla's biome manager. */
    public static boolean applies(ProtoChunk chunk, BiomeManager biomeManager) {
        ChunkStatus status = chunk.getPersistedStatus();
        return chunk.getClass() == ProtoChunk.class && biomeManager.getClass() == BiomeManager.class && !status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)
            && status.heightmapsAfter().equals(WORLDGEN_HEIGHTMAPS) && chunk.hasPrimedHeightmap(Heightmap.Types.OCEAN_FLOOR_WG)
            && chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    }

    public void run() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                surface(x, z);
            }
        }

        this.sections.build();
    }

    private void surface(int x, int z) {
        int blockX = this.chunk.getPos().getMinBlockX() + x;
        int blockZ = this.chunk.getPos().getMinBlockZ() + z;
        int startingHeight = this.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
        this.column.moveTo(blockX, blockZ);
        Holder<Biome> surfaceBiome = this.biomes.get(this.blockPos.set(blockX, this.useLegacyRandom ? 0 : startingHeight, blockZ));
        if (surfaceBiome.is(Biomes.ERODED_BADLANDS)) {
            this.system.erodedBadlandsExtension(this.column, blockX, blockZ, startingHeight, this.chunk);
        }

        int height = this.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
        this.context.updateXZ(blockX, blockZ);
        this.rules.moveToColumn();
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
                this.context.updateY(stoneAboveDepth, y - nextCeilingStoneY + 1, waterHeight, blockX, y, blockZ);
                if (this.column.getBlock(y) == this.system.defaultBlock) {
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
}
