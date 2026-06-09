package com.brodong.enjoyable_ranching;

import com.brodong.enjoyable_ranching.block.ChickenEggBlock;
import com.brodong.enjoyable_ranching.block.FeedingTroughBlock;
import com.brodong.enjoyable_ranching.block.FeedingTroughBlockEntity;
import com.brodong.enjoyable_ranching.item.CheckStickItem;
import com.brodong.enjoyable_ranching.network.GenderSyncPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(EnjoyableRanching.MODID)
public class EnjoyableRanching {

    public static final String MODID = "enjoyable_ranching";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 网络通信协议版本 */
    private static final String PROTOCOL_VERSION = "1";

    /** 模组网络通道，用于性别数据的客户端同步 */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /** 物品的延迟注册器 */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /** 检查棒：右键动物/玩家在聊天栏查看性别与饱食度 */
    public static final RegistryObject<Item> CHECK_STICK = ITEMS.register("check_stick",
            () -> new CheckStickItem(new Item.Properties().stacksTo(1)));

    /** 方块的延迟注册器 */
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    /** 鸡蛋方块：类似海龟蛋孵化，玩家右键收获原版鸡蛋 */
    public static final RegistryObject<Block> CHICKEN_EGG = BLOCKS.register("chicken_egg",
            () -> new ChickenEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .randomTicks()
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY)));

    /** 鸡蛋方块的物品形式 */
    public static final RegistryObject<Item> CHICKEN_EGG_ITEM = ITEMS.register("chicken_egg",
            () -> new BlockItem(CHICKEN_EGG.get(), new Item.Properties()));

    /** 饲槽方块：玩家右键填入饲料，动物优先从中取食 */
    public static final RegistryObject<Block> FEEDING_TROUGH = BLOCKS.register("feeding_trough",
            () -> new FeedingTroughBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)      // 地图显示颜色：木质
                    .strength(0.6F)               // 硬度 0.6（与工作台相同）
                    .sound(SoundType.WOOD)         // 音效：木头
                    .noOcclusion()));              // 非完整方块，允许相邻方块渲染

    /** 饲槽方块的物品形式 */
    public static final RegistryObject<Item> FEEDING_TROUGH_ITEM = ITEMS.register("feeding_trough",
            () -> new BlockItem(FEEDING_TROUGH.get(), new Item.Properties()));

    /** 方块实体的延迟注册器 */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    /**
     * 饲槽方块实体类型注册。
     *
     * <h4>循环引用解决方案：</h4>
     * BlockEntityType.Builder 需要 FeedingTroughBlockEntity 的二参构造器 (pos, state)，
     * 但 BlockEntity 的 super() 又需要 BlockEntityType 实例。
     * 解决策略：
     * <ol>
     *   <li>在此 supplier 内先构建 BlockEntityType（此时仅存储工厂引用，不创建实例）</li>
     *   <li>立即调用 {@code FeedingTroughBlockEntity.initType(type)} 将类型注入静态字段</li>
     *   <li>游戏内创建 BE 时，FeedingTroughBlockEntity(pos, state) 的 super(TYPE, ...) 从静态字段获取类型</li>
     * </ol>
     */
    public static final RegistryObject<BlockEntityType<FeedingTroughBlockEntity>> FEEDING_TROUGH_ENTITY =
            BLOCK_ENTITIES.register("feeding_trough",
                    () -> {
                        // 1. 构建 BlockEntityType：
                        //    - 工厂方法 FeedingTroughBlockEntity::new 匹配 (BlockPos, BlockState) → BE
                        //    - FEEDING_TROUGH.get() 绑定此实体类型到饲槽方块
                        BlockEntityType<FeedingTroughBlockEntity> type =
                                BlockEntityType.Builder.of(FeedingTroughBlockEntity::new, FEEDING_TROUGH.get())
                                        .build(null);
                        // 2. 注入类型引用到 FeedingTroughBlockEntity 静态字段
                        //    此后所有 BE 实例的 super(TYPE, pos, state) 调用可正常工作
                        FeedingTroughBlockEntity.initType(type);
                        // 3. 返回构建完成的类型供注册系统使用
                        return type;
                    });

    public EnjoyableRanching() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册网络包：id=0，将性别同步包注册到网络通道
        CHANNEL.registerMessage(0, GenderSyncPacket.class,
                GenderSyncPacket::encode,
                GenderSyncPacket::decode,
                GenderSyncPacket::handle);

        // 注册通用初始化事件
        modEventBus.addListener(this::commonSetup);

        // 注册物品到事件总线
        ITEMS.register(modEventBus);

        // 注册方块到事件总线
        BLOCKS.register(modEventBus);

        // 注册方块实体到事件总线
        BLOCK_ENTITIES.register(modEventBus);

        // 注册自身到 Forge 事件总线，监听服务端事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册创造模式标签页事件
        modEventBus.addListener(this::addCreative);

        // 注册配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Enjoyable Ranching mod loaded.");
    }

    /** 将模组物品添加到创造模式标签页 */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHECK_STICK);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CHICKEN_EGG_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FEEDING_TROUGH_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    /** 客户端专用事件处理类，仅在客户端生效 */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
