package com.brodong.enjoyable_animal_husbanding;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模组配置文件，基于 Forge 的 Config API 管理可配置项。
 * <p>
 * 通过 {@code @Mod.EventBusSubscriber} 注解自动注册到 MOD 事件总线上，
 * 配置文件会在模组加载时自动读取，配置变更时通过 {@link #onLoad(ModConfigEvent)} 同步。
 */
@Mod.EventBusSubscriber(modid = Enjoyable_animal_husbanding.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    /** Forge 配置构建器，所有配置项均由此构建。 */
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** 是否在通用初始化阶段打印草方块信息（调试用）。 */
    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    /** 一个示例魔法数字，范围 [0, Integer.MAX_VALUE]，默认 42。 */
    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    /** 魔法数字的介绍文本前缀。 */
    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    /**
     * 需要在日志中输出的物品列表，以资源位置字符串形式存储。
     * 默认包含 {@code minecraft:iron_ingot}。
     */
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    /** 构建完成的配置规范实例，供 Forge 配置文件系统使用。 */
    static final ForgeConfigSpec SPEC = BUILDER.build();

    /** 运行时缓存的配置值：是否打印草方块信息。 */
    public static boolean logDirtBlock;
    /** 运行时缓存的配置值：魔法数字。 */
    public static int magicNumber;
    /** 运行时缓存的配置值：魔法数字介绍文本。 */
    public static String magicNumberIntroduction;
    /** 运行时缓存的配置值：解析后的物品集合。 */
    public static Set<Item> items;

    /**
     * 验证配置中的物品名称是否为有效的资源位置。
     *
     * @param obj 待验证的对象，期望为物品资源位置字符串
     * @return 若该物品已注册则返回 {@code true}
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    /**
     * 配置文件加载或重载时触发，将配置值同步到运行时静态字段。
     *
     * @param event 配置变更事件，涵盖加载和重载的场景
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // 将字符串列表转换为实际物品的集合
        items = ITEM_STRINGS.get().stream().map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))).collect(Collectors.toSet());
    }
}
