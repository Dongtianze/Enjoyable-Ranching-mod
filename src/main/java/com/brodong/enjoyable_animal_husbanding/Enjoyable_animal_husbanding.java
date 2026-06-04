package com.brodong.enjoyable_animal_husbanding;

import com.brodong.enjoyable_animal_husbanding.block.ChickenEggBlock;
import com.brodong.enjoyable_animal_husbanding.item.CheckStickItem;
import com.brodong.enjoyable_animal_husbanding.network.GenderSyncPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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

@Mod(Enjoyable_animal_husbanding.MODID)
public class Enjoyable_animal_husbanding {

    public static final String MODID = "enjoyable_animal_husbanding";

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

    public Enjoyable_animal_husbanding() {
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

        // 注册自身到 Forge 事件总线，监听服务端事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册创造模式标签页事件
        modEventBus.addListener(this::addCreative);

        // 注册配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Enjoyable Animal Husbanding mod loaded.");
    }

    /** 将模组物品添加到创造模式标签页 */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHECK_STICK);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CHICKEN_EGG_ITEM);
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
