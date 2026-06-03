package com.brodong.enjoyable_animal_husbanding;

import com.brodong.enjoyable_animal_husbanding.client.render.GenderRenderLayer;
import com.brodong.enjoyable_animal_husbanding.item.CheckStickItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
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

@Mod(Enjoyable_animal_husbanding.MODID)
public class Enjoyable_animal_husbanding {

    public static final String MODID = "enjoyable_animal_husbanding";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 物品的延迟注册器 */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /** 检查棒：右键动物在聊天栏查看性别，合成配方为木棍 + 钻石 */
    public static final RegistryObject<Item> CHECK_STICK = ITEMS.register("check_stick",
            () -> new CheckStickItem(new Item.Properties().stacksTo(1)));

    public Enjoyable_animal_husbanding() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册通用初始化事件
        modEventBus.addListener(this::commonSetup);

        // 注册物品到事件总线
        ITEMS.register(modEventBus);

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

    /** 将检查棒添加到工具与实用物品标签页 */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHECK_STICK);
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

        /**
         * 为所有生物实体注册性别指示渲染层，在头顶显示 ♂ / ♀ 符号
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES) {
                // 跳过玩家，只处理生物实体
                if (entityType == EntityType.PLAYER) continue;
                LivingEntityRenderer renderer = event.getRenderer((EntityType) entityType);
                if (renderer != null) {
                    renderer.addLayer(new GenderRenderLayer(renderer));
                }
            }
        }
    }
}
