package com.brodong.enjoyable_ranching.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 饲槽方块 — 动物集中喂食点。
 *
 * <h3>外观</h3>
 * 模型复用原版堆肥桶（composter），通过 {@link #LEVEL} 属性（0-8）切换 9 种 variant。
 *
 * <h3>玩家交互</h3>
 * 手持胡萝卜 / 小麦种子 / 小麦右键 → 填入一格饲料（最多 8 格）。
 * 同一饲槽只能放入同一种饲料，不可混装。
 *
 * <h3>动物取食</h3>
 * 动物在自然进食（吃草/啄食）之前，优先搜索 16 格内装有对应饲料的饲槽。
 * 取食成功则消耗一格饲料并回满饱食度。
 *
 * <h3>物种-饲料映射</h3>
 * <table>
 *   <tr><td>牛、绵羊</td><td>小麦</td></tr>
 *   <tr><td>猪</td><td>胡萝卜</td></tr>
 *   <tr><td>鸡</td><td>小麦种子</td></tr>
 * </table>
 *
 * <h3>位置缓存</h3>
 * 为 O(1) 查找，在方块放置/破坏时同步维护全局 {@link #TROUGH_POSITIONS} 集合。
 * 动物 tick 时直接遍历此集合，无需逐块扫描。
 *
 * @see FeedingTroughBlockEntity 实体数据存储
 */
public class FeedingTroughBlock extends BaseEntityBlock {

    /** 方块状态属性：填充等级 0-8，驱动 9 种模型 variant 切换 */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);

    /**
     * 物种到饲料的映射表。
     * 使用 {@code Class<?>} 作为 key，利用 {@code getClass() == class} 做精确匹配（不含子类）。
     */
    private static final Map<Class<?>, Item> ANIMAL_FEED = Map.of(
            Cow.class, Items.WHEAT,
            Sheep.class, Items.WHEAT,
            Pig.class, Items.CARROT,
            Chicken.class, Items.WHEAT_SEEDS
    );

    /** 玩家可用以填入饲槽的有效物品集合 */
    private static final Set<Item> VALID_FEED = Set.of(Items.CARROT, Items.WHEAT_SEEDS, Items.WHEAT);

    /**
     * 全局饲槽坐标缓存。
     * <p>
     * 使用 ConcurrentHashMap 包装的 Set，在方块放置时添加、破坏时移除。
     * 动物在 {@code GrazingHandler.tryEatFromTrough()} 中遍历此缓存查找附近饲槽，
     * 相比逐块扫描性能提升约两个数量级。
     */
    public static final Set<BlockPos> TROUGH_POSITIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public FeedingTroughBlock(Properties properties) {
        super(properties);
        // 注册方块状态定义，设置 level 属性的默认值为 0（空饲槽）
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 向方块状态定义注册 LEVEL 属性，使其可被 blockstate JSON 引用
        builder.add(LEVEL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 使用 MODEL 渲染模式，由 blockstate JSON 指定模型
        return RenderShape.MODEL;
    }

    // ==================== 方块实体生命周期 ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 创建对应的方块实体实例，数据（fillLevel / feedItem）存储在 BE 的 NBT 中
        return new FeedingTroughBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 饲槽不需要每 tick 更新，返回 null
        return null;
    }

    // ==================== 方块放置/破坏 → 维护全局位置缓存 ====================

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 仅在服务端维护缓存，因为动物取食逻辑只在服务端执行
        if (!level.isClientSide()) {
            // 存储不可变副本，避免外部修改影响缓存
            TROUGH_POSITIONS.add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // 仅当方块被真正移除（非被替换为同种方块）时清理
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                // 从全局缓存中移除该位置
                TROUGH_POSITIONS.remove(pos);
            }
            // 触发 super.onRemove 以释放方块实体等资源
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // ==================== 玩家右键填入饲料 ====================

    /**
     * 玩家右键交互入口。
     * <h4>执行流程（服务端）：</h4>
     * <ol>
     *   <li>检查手持物品是否为有效饲料（胡萝卜/小麦种子/小麦）→ 否则 PASS</li>
     *   <li>获取方块实体，检查 fillLevel 是否已满 8 → 满则 CONSUME（播放动画但不消耗物品）</li>
     *   <li>检查槽内是否已有不同品种饲料 → 有则 PASS</li>
     *   <li>调用 {@link FeedingTroughBlockEntity#addFeed} 写入饲料，更新 fillLevel</li>
     *   <li>更新方块状态中的 LEVEL 属性以触发模型切换</li>
     *   <li>生存模式下消耗手持物品 1 个</li>
     * </ol>
     * <h4>客户端：</h4>
     * 立即返回 SUCCESS，播放右键动画。方块状态由服务端同步。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 1. 校验手持物品
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !VALID_FEED.contains(held.getItem())) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity be) {
            // 2. 已满 → 阻止填入，消耗交互但不消耗物品
            if (be.getFillLevel() >= 8) return InteractionResult.CONSUME;
            // 3. 已有不同品种饲料 → 拒绝混装
            if (be.getFeedItem() != null && be.getFeedItem() != held.getItem()) return InteractionResult.PASS;

            // 4-6. 服务端处理：写入饲料 + 更新模型 + 消耗物品
            if (!level.isClientSide()) {
                be.addFeed(held.getItem());
                // 同步更新方块状态 LEVEL，驱动模型切换到对应 variant
                level.setBlock(pos, state.setValue(LEVEL, be.getFillLevel()), 3);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            // 客户端返回 SUCCESS 播放动画，服务端返回 CONSUME
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    // ==================== 动物取食（静态 API，由 GrazingHandler 调用） ====================

    /**
     * 动物从指定位置的饲槽取食。
     * <h4>执行流程：</h4>
     * <ol>
     *   <li>查 {@link #ANIMAL_FEED} 表获取该物种所需饲料品种 → 无则返回 false</li>
     *   <li>获取方块实体，检查饲料品种是否匹配且 fillLevel > 0</li>
     *   <li>调用 {@link FeedingTroughBlockEntity#consumeFeed} 消耗一级</li>
     *   <li>同步更新方块状态 LEVEL</li>
     *   <li>返回 true 表示取食成功</li>
     * </ol>
     *
     * @param level   方块所在世界
     * @param pos     饲槽方块坐标
     * @param animal  取食的动物
     * @return 是否取食成功
     */
    public static boolean consumeFeed(Level level, BlockPos pos, Animal animal) {
        // 查找该物种需要的饲料品种
        Item expected = ANIMAL_FEED.get(animal.getClass());
        // 不在食谱中的动物不可取食
        if (expected == null) return false;

        if (level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity be) {
            // 校验饲料品种匹配且有库存
            if (be.getFeedItem() == expected && be.getFillLevel() > 0) {
                // 消耗一级饲料，内部自动处理 fillLevel 归零时清空品种
                be.consumeFeed();
                // 同步方块状态，驱动模型变化
                BlockState state = level.getBlockState(pos).setValue(LEVEL, be.getFillLevel());
                level.setBlock(pos, state, 3);
                return true;
            }
        }
        return false;
    }
}
