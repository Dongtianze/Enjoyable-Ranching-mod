package com.brodong.enjoyable_animal_husbanding;

import net.minecraft.world.entity.Entity;

/**
 * 动物饱食度工具类，通过 {@link Entity#getPersistentData()} 存储和读取饱食度。
 * <p>
 * 饱食度范围 [0, 20]，初始值为 20。值越大表示动物越饱。
 * 饱食度为 0 时动物会持续扣血，超过 15 时会缓慢回血。
 */
public class SatietyHelper {

    /** PersistentData 中的键名，以命名空间前缀避免冲突 */
    private static final String KEY = "enjoyable_animal_husbanding:satiety";

    /** 饱食度最大值 */
    public static final int MAX_SATIETY = 20;

    /** 饱食度最小值（饥饿） */
    public static final int MIN_SATIETY = 0;

    /**
     * 读取实体的当前饱食度。
     * <p>
     * 若 PersistentData 中尚无记录（新生成的实体），默认返回 20（满腹）。
     *
     * @param entity 目标实体
     * @return 饱食度 [0, 20]
     */
    public static int getSatiety(Entity entity) {
        if (!entity.getPersistentData().contains(KEY)) {
            return MAX_SATIETY;
        }
        return entity.getPersistentData().getInt(KEY);
    }

    /**
     * 设置实体的饱食度，值会被限制在 [{@link #MIN_SATIETY}, {@link #MAX_SATIETY}] 范围内。
     *
     * @param entity 目标实体
     * @param value  目标饱食度
     */
    public static void setSatiety(Entity entity, int value) {
        int clamped = Math.max(MIN_SATIETY, Math.min(MAX_SATIETY, value));
        entity.getPersistentData().putInt(KEY, clamped);
    }

    /**
     * 将实体饱食度恢复至最大值（喂食时调用）。
     *
     * @param entity 目标实体
     */
    public static void fillSatiety(Entity entity) {
        setSatiety(entity, MAX_SATIETY);
    }
}
