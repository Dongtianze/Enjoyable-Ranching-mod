package com.brodong.enjoyable_ranching.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 饲槽方块实体，使用 PersistentData（基于 BlockEntity 的 NBT）存储饲料类型和填充等级 0-8。
 *
 * <h3>数据存储</h3>
 * <ul>
 *   <li>{@code fillLevel} — 当前填充等级 (0=空, 8=满)</li>
 *   <li>{@code feedItem} — 当前存储的饲料品种 (小麦 / 胡萝卜 / 小麦种子)，空饲槽为 {@code null}</li>
 * </ul>
 *
 * <h3>类型注入（避免循环引用）</h3>
 * {@link BlockEntityType} 的注册需要引用本类的构造器，
 * 但 {@code BlockEntity(BEType, pos, state)} 的 super 调用又需要 {@link BlockEntityType}。
 * <br>解决方案：构造函数仅接受 (pos, state)，类型通过静态字段 {@link #TYPE} 在注册完成后由
 * {@link #initType(BlockEntityType)} 延迟注入。
 *
 * <h3>客户端同步</h3>
 * 通过 {@link #getUpdatePacket()} / {@link #getUpdateTag()} 将方块状态变更通知客户端，
 * 使饲槽模型（复用堆肥桶模型）能根据 fillLevel 切换显示层级。
 *
 * @see FeedingTroughBlock
 */
public class FeedingTroughBlockEntity extends BlockEntity {

    /**
     * 本实体的 BlockEntityType 引用。
     * 由 {@link com.brodong.enjoyable_ranching.EnjoyableRanching} 在注册完成后通过
     * {@link #initType(BlockEntityType)} 注入，解决注册时的循环引用问题。
     */
    @Nullable
    private static BlockEntityType<FeedingTroughBlockEntity> TYPE;

    /**
     * 由 {@code EnjoyableRanching} 在 {@link BlockEntityType} 构建完成后立即调用，
     * 将类型引用注入本类，使得后续构造函数可调用 {@code super(TYPE, pos, state)}。
     *
     * @param type 已构建完成的 BlockEntityType 实例
     */
    public static void initType(BlockEntityType<FeedingTroughBlockEntity> type) {
        TYPE = type;
    }

    /** 当前填充等级，范围 0-8。0 表示空，8 表示满。 */
    private int fillLevel;

    /**
     * 饲槽中当前存储的饲料品种。
     * {@code null} 表示槽内无饲料（fillLevel == 0 时始终为 null）。
     * 一旦首次填入，后续只能填入同品种，直到槽被清空。
     */
    @Nullable
    private Item feedItem;

    /**
     * 方块实体构造器。
     * <p>
     * 调用链：{@code BlockEntityType.Builder.of(FeedingTroughBlockEntity::new, ...)} 存储此构造器的工厂引用，
     * 当世界中实际创建饲槽方块时调用本构造器创建 BE 实例。
     * 类型由静态字段 {@link #TYPE} 提供，在注册阶段已通过 {@link #initType} 注入。
     *
     * @param pos   方块坐标
     * @param state 方块状态
     */
    public FeedingTroughBlockEntity(BlockPos pos, BlockState state) {
        // TYPE 已在 EnjoyableRanching 的 DeferredRegister supplier 中注入，此处不为 null
        super(TYPE, pos, state);
        this.fillLevel = 0;
        this.feedItem = null;
    }

    /** @return 当前填充等级 (0-8) */
    public int getFillLevel() {
        return fillLevel;
    }

    /**
     * 设置填充等级并标记数据已变更。
     * 标记后 {@link #getUpdatePacket()} 会将新状态写入同步包发送至客户端。
     */
    public void setFillLevel(int fillLevel) {
        this.fillLevel = fillLevel;
        setChanged();
    }

    /** @return 当前存储的饲料品种，空槽时返回 {@code null} */
    @Nullable
    public Item getFeedItem() {
        return feedItem;
    }

    public void setFeedItem(@Nullable Item feedItem) {
        this.feedItem = feedItem;
        setChanged();
    }

    /**
     * 玩家右键饲槽时调用，向槽中添加一份饲料。
     * <h4>执行流程：</h4>
     * <ol>
     *   <li>检查 fillLevel 是否已达上限 8 → 满则拒绝</li>
     *   <li>检查槽内是否已有不同品种的饲料 → 有则拒绝（同一槽不可混装）</li>
     *   <li>若槽为空（feedItem == null），则记录本次的饲料品种</li>
     *   <li>fillLevel +1，标记数据变更</li>
     * </ol>
     *
     * @param item 待添加的饲料物品（已由调用方校验为有效饲料类型）
     * @return 是否添加成功
     */
    public boolean addFeed(Item item) {
        // 达到最大容量 8，拒绝填入
        if (fillLevel >= 8) return false;
        // 已存有其他品种饲料，拒绝混装
        if (feedItem != null && feedItem != item) return false;
        // 首次填入：记录饲料品种；后续填入：品种相同，累加等级
        feedItem = item;
        fillLevel++;
        setChanged();
        return true;
    }

    /**
     * 动物从饲槽取食时调用，消耗一级饲料。
     * <h4>执行流程：</h4>
     * <ol>
     *   <li>检查是否有饲料可取（fillLevel <= 0 → 返回 null）</li>
     *   <li>记录当前饲料品种用于返回</li>
     *   <li>fillLevel -1</li>
     *   <li>若消耗后 fillLevel 归零，清空 feedItem（槽变空）</li>
     *   <li>标记数据变更以触发客户端同步</li>
     * </ol>
     *
     * @return 被消耗的饲料物品，空槽时返回 {@code null}
     */
    @Nullable
    public Item consumeFeed() {
        // 无饲料可取
        if (fillLevel <= 0) return null;
        // 记录被消耗的品种（槽变空前不会变）
        Item consumed = feedItem;
        fillLevel--;
        // 消耗后槽变空，清空品种标记
        if (fillLevel == 0) {
            feedItem = null;
        }
        setChanged();
        return consumed;
    }

    /**
     * 将实体数据写入 NBT，随区块一起保存到磁盘。
     * <h4>写入内容：</h4>
     * <ul>
     *   <li>{@code fillLevel} — 当前填充等级</li>
     *   <li>{@code feed} — 饲料品种的资源位置字符串（如 {@code minecraft:wheat}），null 时不写入</li>
     * </ul>
     */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 写入填充等级
        tag.putInt("fillLevel", fillLevel);
        if (feedItem != null) {
            // 将 Item 实例转化为注册表键名存储，保证跨存档兼容
            tag.putString("feed", ForgeRegistries.ITEMS.getKey(feedItem).toString());
        }
    }

    /**
     * 从 NBT 读取实体数据，在区块加载时调用。
     * <h4>读取流程：</h4>
     * <ol>
     *   <li>读取 fillLevel 整数值</li>
     *   <li>若存在 feed 键 → 解析资源位置字符串 → 从注册表获取对应 Item</li>
     *   <li>若不存在 feed 键 → feedItem 保持 {@code null}</li>
     * </ol>
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 恢复填充等级
        fillLevel = tag.getInt("fillLevel");
        if (tag.contains("feed")) {
            // 根据保存的资源位置字符串还原 Item 实例
            feedItem = ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(tag.getString("feed")));
        }
    }

    /**
     * 获取用于客户端初始同步的 NBT 标签。
     * 在区块发送给客户端时调用，确保客户端能正确显示饲槽的填充层级。
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        // 复用 saveAdditional 写入完整数据，避免重复代码
        saveAdditional(tag);
        return tag;
    }

    /**
     * 获取数据变更时的增量同步包。
     * 在 {@link #setChanged()} 调用后，服务端通过此包将变更通知所有追踪此方块的客户端。
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // create(this) 会自动调用 getUpdateTag() 打包数据
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
