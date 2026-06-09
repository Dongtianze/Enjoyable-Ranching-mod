package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.Gender;
import com.brodong.enjoyable_ranching.GenderHelper;
import com.brodong.enjoyable_ranching.SatietyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 动物进食行为处理器：牛吃草、鸡啄植物、羊吃草。
 * <p>
 * 所有方法均为静态工具方法，由 {@link LivingTickHandler} 调用。
 */
public class GrazingHandler {

    private static final double MATE_SEARCH_RANGE = 8.0D;

    private static final int COW_GRAZE_BASE = 300;
    private static final int CHICKEN_GRAZE_BASE = 200;

    // ==================== 羊吃草 ====================

    /**
     * 羊吃草饱食度调控：
     * <ul>
     *   <li>饱食度 > 15：自动长回羊毛阻止进食</li>
     *   <li>饱食度 < 5：强制剪毛迫使进食</li>
     *   <li>羊毛从无到有 → 饱食度回满</li>
     * </ul>
     */
    static int handleSheepGrazing(Sheep sheep, int satiety, CompoundTag data) {
        boolean wasSheared = data.getBoolean("enjoyable_ranching:was_sheared");
        boolean isSheared = sheep.isSheared();

        if (wasSheared && !isSheared) {
            SatietyHelper.fillSatiety(sheep);
            satiety = SatietyHelper.MAX_SATIETY;
        }

        if (satiety > 15 && isSheared) {
            sheep.setSheared(false);
            isSheared = false;
        }
        if (satiety < 5 && !isSheared) {
            sheep.setSheared(true);
            isSheared = true;
        }

        data.putBoolean("enjoyable_ranching:was_sheared", isSheared);
        return satiety;
    }

    // ==================== 牛吃草 ====================

    /** 饱食度 > 15 不进食，< 5 概率加倍 */
    static void handleCowGrazing(Cow cow, int satiety) {
        if (satiety > 15) return;
        int chance = satiety < 5 ? COW_GRAZE_BASE / 2 : COW_GRAZE_BASE;
        if (cow.getRandom().nextInt(chance) != 0) return;

        BlockPos below = cow.blockPosition().below();
        BlockState state = cow.level().getBlockState(below);
        if (state.is(Blocks.GRASS_BLOCK)) {
            cow.level().setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
            SatietyHelper.fillSatiety(cow);
            cow.playSound(SoundEvents.COW_HURT, 1.0F, 1.0F);
        }
    }

    // ==================== 鸡啄食 ====================

    /** 饱食度 > 15 不进食，< 5 概率加倍 */
    static void handleChickenGrazing(Chicken chicken, int satiety) {
        if (satiety > 15) return;
        int chance = satiety < 5 ? CHICKEN_GRAZE_BASE / 2 : CHICKEN_GRAZE_BASE;
        if (chicken.getRandom().nextInt(chance) != 0) return;

        BlockPos pos = chicken.blockPosition();
        BlockState state = chicken.level().getBlockState(pos);
        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(BlockTags.FLOWERS)) {
            chicken.level().destroyBlock(pos, false);
            SatietyHelper.fillSatiety(chicken);
            chicken.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 1.0F);
        }
    }

    // ==================== 交配检查 ====================

    /** 检查附近是否存在异性同类 */
    static boolean hasCompatibleMate(Animal animal) {
        Gender myGender = GenderHelper.getGender(animal);
        if (myGender == Gender.None) return true;
        AABB searchBox = animal.getBoundingBox().inflate(MATE_SEARCH_RANGE, 4.0D, MATE_SEARCH_RANGE);
        for (Animal other : animal.level().getEntitiesOfClass(Animal.class, searchBox,
                a -> a != animal && a.getClass() == animal.getClass())) {
            Gender otherGender = GenderHelper.getGender(other);
            if (otherGender != Gender.None && otherGender != myGender) {
                return true;
            }
        }
        return false;
    }
}
