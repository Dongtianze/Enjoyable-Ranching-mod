package com.brodong.enjoyable_animal_husbanding;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组配置文件，基于 Forge Config API 管理可配置项。
 * 配置文件路径：config/enjoyable_animal_husbanding-common.toml
 */
@Mod.EventBusSubscriber(modid = Enjoyable_animal_husbanding.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** 是否在动物头顶显示性别指示器符号（♂ / ♀） */
    private static final ForgeConfigSpec.BooleanValue SHOW_GENDER_INDICATOR =
            BUILDER.comment("Whether to show gender indicator symbol above animals")
                    .define("showGenderIndicator", true);

    /** 构建完成的配置规范实例 */
    static final ForgeConfigSpec SPEC = BUILDER.build();

    /** 运行时缓存：是否显示性别指示器 */
    public static boolean showGenderIndicator;

    /**
     * 配置文件加载或重载时触发，将配置值同步到运行时静态字段
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        showGenderIndicator = SHOW_GENDER_INDICATOR.get();
    }
}
