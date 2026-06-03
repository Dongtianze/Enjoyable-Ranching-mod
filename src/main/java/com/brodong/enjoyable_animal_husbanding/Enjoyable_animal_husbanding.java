package com.brodong.enjoyable_animal_husbanding;

import com.brodong.enjoyable_animal_husbanding.item.CheckStickItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Enjoyable Animal Husbanding 模组的主入口类。
 * <p>
 * 该模组为 Minecraft 1.20.1 中的动物添加性别系统：
 * <ul>
 *   <li>每只动物在生成时随机获得雄性（Male）或雌性（Female）性别</li>
 *   <li>性别数据随实体 NBT 持久化，加载存档后保持不变</li>
 *   <li>只有异性动物之间才能进行繁殖</li>
 *   <li>繁殖 AI 会在寻找伴侣时自动过滤同性别目标</li>
 * </ul>
 * <p>
 * 核心实现参见：
 * <ul>
 *   <li>{@link com.brodong.enjoyable_animal_husbanding.accessor.Gender}    性别枚举</li>
 *   <li>{@link com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor} 性别访问接口</li>
 *   <li>{@link com.brodong.enjoyable_animal_husbanding.mixin.AnimalMixin}   注入 Animal 实体的核心 Mixin</li>
 *   <li>{@link com.brodong.enjoyable_animal_husbanding.mixin.BreedGoalMixin} 注入繁殖 AI 的 Mixin</li>
 * </ul>
 *
 * @see Config
 */
@Mod(Enjoyable_animal_husbanding.MODID)
public class Enjoyable_animal_husbanding {

    /** 模组 ID，需与 {@code mods.toml} 和 {@code mixins.json} 中的命名空间保持一致。 */
    public static final String MODID = "enjoyable_animal_husbanding";

    /** 模组日志记录器，基于 SLF4J。 */
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 方块的延迟注册器，注册在 {@code enjoyable_animal_husbanding} 命名空间下。 */
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    /** 物品的延迟注册器，注册在 {@code enjoyable_animal_husbanding} 命名空间下。 */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /** 检查棒：右键动物可在聊天栏查看性别，合成配方为木棍 + 钻石。 */
    public static final RegistryObject<Item> CHECK_STICK = ITEMS.register("check_stick",
            () -> new CheckStickItem(new Item.Properties().stacksTo(1)));

    // 若将来需要添加自定义物品/方块，可在此处取消注释并注册：
    //
    // public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
    //         () -> new Item(new Item.Properties().food(
    //                 new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));

    /**
     * 模组构造函数，在模组加载时由 Forge 调用。
     * <p>
     * 在此完成：
     * <ol>
     *   <li>注册通用初始化事件监听器</li>
     *   <li>注册方块和物品的延迟注册器</li>
     *   <li>注册 Forge 游戏事件总线（服务端事件等）</li>
     *   <li>注册创造模式标签页内容事件</li>
     *   <li>注册模组配置文件</li>
     * </ol>
     */
    public Enjoyable_animal_husbanding() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册通用初始化事件（mod 加载完成后触发）
        modEventBus.addListener(this::commonSetup);

        // 将方块和物品注册器绑定到 mod 事件总线
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        // 注册自身到 Forge 事件总线，接收服务端等游戏事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册创造模式标签页内容事件
        modEventBus.addListener(this::addCreative);

        // 注册配置文件（类型为 COMMON，位于 config/ 目录下）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * 通用初始化回调，在 {@link FMLCommonSetupEvent} 触发时执行。
     * <p>
     * 输出调试日志并读取配置文件中的值。
     *
     * @param event 通用初始化事件
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    /**
     * 创造模式标签页内容事件回调，用于向指定标签页添加物品。
     * <p>
     * 当前未注册自定义物品，因此方法体为空。若将来注册了物品，
     * 可在此处将物品添加到对应的创造模式标签页中。
     *
     * @param event 创造模式标签页内容事件
     */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHECK_STICK);
        }
    }

    /**
     * 服务端启动事件处理器。
     * <p>
     * 通过 {@link SubscribeEvent} 注解自动被 Forge 事件总线发现和调用。
     *
     * @param event 服务端启动事件
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    /**
     * 客户端专用事件的内部订阅类。
     * <p>
     * 通过 {@code @Mod.EventBusSubscriber} 将此类中的所有
     * {@code @SubscribeEvent} 静态方法自动注册到 MOD 事件总线上，
     * 且仅在客户端（{@link Dist#CLIENT}）生效。
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        /**
         * 客户端初始化回调，在 {@link FMLClientSetupEvent} 触发时执行。
         *
         * @param event 客户端初始化事件
         */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
