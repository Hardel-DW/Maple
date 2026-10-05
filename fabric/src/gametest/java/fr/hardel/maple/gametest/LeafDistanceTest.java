package fr.hardel.maple.gametest;

import fr.hardel.maple.optimisation.LeafDistance;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LeafDistanceTest {
    private static final BlockPos LEAVES = new BlockPos(1, 1, 1);
    private static final BlockPos STONE = new BlockPos(2, 1, 1);

    @GameTest
    public void everyStateHoldsTheDistanceVanillaComputes(GameTestHelper helper) {
        List<BlockState> wrong = BuiltInRegistries.BLOCK.stream().flatMap(block -> block.getStateDefinition().getPossibleStates().stream())
            .filter(state -> ((LeafDistance) state).maple$leafDistance() != vanilla(state)).toList();

        helper.assertTrue(wrong.isEmpty(), "%d states hold a distance vanilla does not compute, the first is %s".formatted(wrong.size(), wrong.stream().findFirst()));
        helper.succeed();
    }

    @GameTest
    public void aLeafTickFollowsTheTagsOfItsNeighbour(GameTestHelper helper) {
        Holder.Reference<Block> stone = Blocks.STONE.builtInRegistryHolder();
        List<TagKey<Block>> original = stone.tags().toList();
        helper.setBlock(STONE, Blocks.STONE);
        helper.setBlock(LEAVES, Blocks.OAK_LEAVES);

        bind(stone, Stream.concat(original.stream(), Stream.of(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)).toList());
        int tagged = vanilla(Blocks.STONE.defaultBlockState());
        helper.getBlockState(LEAVES).tick(helper.getLevel(), helper.absolutePos(LEAVES), helper.getLevel().getRandom());
        int ticked = helper.getBlockState(LEAVES).getValue(LeavesBlock.DISTANCE);
        bind(stone, original);
        int untagged = ((LeafDistance) Blocks.STONE.defaultBlockState()).maple$leafDistance();

        helper.assertValueEqual(tagged, 0, "vanilla distance of tagged stone");
        helper.assertValueEqual(ticked, 1, "distance of a leaf next to tagged stone after its tick");
        helper.assertValueEqual(untagged, vanilla(Blocks.STONE.defaultBlockState()), "distance of stone once its tags are restored");
        helper.succeed();
    }

    private static int vanilla(BlockState state) {
        return LeavesBlock.getOptionalDistanceAt(state).orElse(LeavesBlock.DECAY_DISTANCE);
    }

    private static void bind(Holder.Reference<Block> holder, Collection<TagKey<Block>> tags) {
        try {
            Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
            bindTags.setAccessible(true);
            bindTags.invoke(holder, tags);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Cannot bind the tags of %s".formatted(holder), exception);
        }
    }
}
