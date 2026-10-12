package fr.hardel.maple.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class BlenderTest {
    private static final ChunkStep BIOMES = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.BIOMES);

    @GameTest(maxTicks = 100)
    public void theBlenderAnswersWhileTheDiskIsBusy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos center = new ChunkPos(3000, 3000);
        CompletableFuture<Void> disk = new CompletableFuture<>();
        level.getChunkSource().chunkMap.write(center, () -> {
            disk.join();
            return null;
        });
        Blender blender = CompletableFuture.supplyAsync(() -> Blender.of(region(level, center, null))).completeOnTimeout(null, 2, TimeUnit.SECONDS).join();
        disk.complete(null);

        helper.assertTrue(blender != null, "the blender answers while the disk is busy");
        helper.assertTrue(blender == Blender.empty(), "no chunk around carries blending data");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void aChunkWithBlendingDataAtTheEdgeOfTheRangeStillBlends(GameTestHelper helper) {
        ChunkPos center = new ChunkPos(-3000, 3000);
        Blender blender = Blender.of(region(helper.getLevel(), center, new ChunkPos(center.x() + 7, center.z())));
        helper.assertFalse(blender.isEmpty(), "the chunk with blending data seven chunks away blends the terrain");
        helper.succeed();
    }

    private static WorldGenRegion region(ServerLevel level, ChunkPos center, @Nullable ChunkPos old) {
        StaticCache2D<GenerationChunkHolder> holders = StaticCache2D.create(center.x(), center.z(), BIOMES.getAccumulatedRadiusOf(ChunkStatus.EMPTY),
            (x, z) -> new HeldChunk(chunk(level, new ChunkPos(x, z), old)));
        return new WorldGenRegion(level, holders, BIOMES, holders.get(center.x(), center.z()).getChunkIfPresentUnchecked(ChunkStatus.EMPTY));
    }

    private static ProtoChunk chunk(ServerLevel level, ChunkPos pos, @Nullable ChunkPos old) {
        BlendingData blending = pos.equals(old) ? BlendingData.unpack(new BlendingData.Packed(level.getMinSectionY(), level.getMaxSectionY() + 1, Optional.empty())) : null;
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.palettedContainerFactory(), blending);
        chunk.setPersistedStatus(ChunkStatus.BIOMES);
        return chunk;
    }

    private static final class HeldChunk extends GenerationChunkHolder {
        private final ChunkAccess chunk;

        private HeldChunk(ChunkAccess chunk) {
            super(chunk.getPos());
            this.chunk = chunk;
        }

        @Override
        public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
            return chunk;
        }

        @Override
        protected void addSaveDependency(CompletableFuture<?> sync) {
        }

        @Override
        public int getTicketLevel() {
            return ChunkLevel.byStatus(ChunkStatus.FULL);
        }

        @Override
        public int getQueueLevel() {
            return getTicketLevel();
        }
    }
}
