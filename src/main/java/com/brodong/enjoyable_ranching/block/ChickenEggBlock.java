package com.brodong.enjoyable_ranching.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 鸡蛋方块，机制参考海龟蛋但孵化时间更短，使用海龟蛋模型。
 * <p>
 * <ul>
 *   <li>最多叠加 4 个蛋，有 3 级裂纹（0/1/2 → 未裂/轻微裂纹/严重裂纹）</li>
 *   <li>每 tick 有 1/100 概率升级裂纹（海龟蛋为 1/500），加速孵化</li>
 *   <li>孵化后生成 1 只幼年鸡</li>
 *   <li>玩家右键直接收获 1~3 个原版鸡蛋并移除方块</li>
 * </ul>
 */
public class ChickenEggBlock extends TurtleEggBlock {

    /** 裂纹升级概率：1/100 每 tick（海龟蛋为 1/500） */
    private static final int HATCH_CHANCE = 100;

    public ChickenEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int hatch = state.getValue(HATCH);
        int eggs = state.getValue(EGGS);

        // 裂纹已达最大 → 孵化
        if (hatch == 2) {
            hatch(level, pos, state);
            return;
        }

        // 下方为固体方块才能孵化（比海龟蛋的沙子限定更宽松）
        if (!level.getBlockState(pos.below()).isSolid()) return;

        // 加速孵化（1/100 概率升级裂纹）
        if (random.nextInt(HATCH_CHANCE) == 0) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS,
                    0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, hatch + 1), 2);
        }
    }

    /**
     * 孵化：移除方块并生成一只幼年鸡。
     */
    private void hatch(ServerLevel level, BlockPos pos, BlockState state) {
        level.destroyBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        Chicken chicken = EntityType.CHICKEN.create(level);
        if (chicken != null) {
            chicken.setBaby(true);
            chicken.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(chicken);
        }
    }

    /**
     * 玩家右键：收获 1~3 个原版鸡蛋并移除该方块。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            int count = level.random.nextInt(3) + 1;
            popResource(level, pos, new ItemStack(Items.EGG, count));
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
