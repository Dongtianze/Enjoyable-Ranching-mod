package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.EnjoyableRanching;
import com.brodong.enjoyable_ranching.Gender;
import com.brodong.enjoyable_ranching.GenderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 鸡下蛋逻辑：替代原版鸡蛋物品掉落。
 * <p>
 * 锁定原版 eggTime，使用独立 PersistentData 计时器。
 * 雌性 1/8 鸡蛋方块 + 7/8 羽毛，雄性 100% 羽毛。
 */
public class ChickenEggHandler {

    /** 永久阻止原版蛋机制的值 */
    private static final int EGG_TIME_LOCK = 999999;
    /** 下蛋间隔（tick）基础值 */
    private static final int EGG_TIMER_BASE = 6000;

    static void handleChickenEggLaying(Chicken chicken) {
        if (chicken.eggTime < EGG_TIME_LOCK) {
            chicken.eggTime = EGG_TIME_LOCK;
        }

        CompoundTag data = chicken.getPersistentData();
        int timer = data.getInt("enjoyable_ranching:egg_timer") - 1;

        if (timer <= 0) {
            data.putInt("enjoyable_ranching:egg_timer",
                    chicken.getRandom().nextInt(EGG_TIMER_BASE) + EGG_TIMER_BASE);

            Gender gender = GenderHelper.getGender(chicken);
            if (gender == Gender.Male) {
                int count = chicken.getRandom().nextInt(2) + 1;
                chicken.spawnAtLocation(new ItemStack(Items.FEATHER, count));
            } else {
                if (chicken.getRandom().nextInt(8) == 0) {
                    BlockPos pos = chicken.blockPosition();
                    if (chicken.level().getBlockState(pos.below()).isSolid()
                            && chicken.level().getBlockState(pos).canBeReplaced()) {
                        chicken.level().setBlock(pos,
                                EnjoyableRanching.CHICKEN_EGG.get().defaultBlockState(), 3);
                    }
                } else {
                    int count = chicken.getRandom().nextInt(2) + 1;
                    chicken.spawnAtLocation(new ItemStack(Items.FEATHER, count));
                }
            }
        } else {
            data.putInt("enjoyable_ranching:egg_timer", timer);
        }
    }
}
