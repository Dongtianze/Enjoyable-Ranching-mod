package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.EnjoyableRanching;
import com.brodong.enjoyable_ranching.Gender;
import com.brodong.enjoyable_ranching.GenderHelper;
import com.brodong.enjoyable_ranching.SatietyHelper;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 实体生命周期事件：加入/离开世界时的初始化与清理。
 */
@Mod.EventBusSubscriber(modid = EnjoyableRanching.MODID)
public class EntityEvents {

    private static final Random RANDOM = new Random();

    /** 实体加入世界时分配性别、饱食度、鸡下蛋计时器 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;

        if (GenderHelper.getGender(animal) == Gender.None) {
            GenderHelper.setGender(animal, RANDOM.nextBoolean() ? Gender.Male : Gender.Female);
        }
        if (!animal.getPersistentData().contains("enjoyable_ranching:satiety")) {
            SatietyHelper.fillSatiety(animal);
        }
        animal.getPersistentData().putInt("enjoyable_ranching:last_damage_tick", animal.tickCount);
        animal.getPersistentData().putInt("enjoyable_ranching:last_heal_tick", animal.tickCount);

        if (animal instanceof Chicken chicken
                && !chicken.getPersistentData().contains("enjoyable_ranching:egg_timer")) {
            chicken.getPersistentData().putInt("enjoyable_ranching:egg_timer",
                    chicken.getRandom().nextInt(1200) + 1200);
        }
    }

    /** 实体离开世界时清理客户端性别缓存 */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            GenderHelper.removeFromClientCache(event.getEntity().getUUID());
        }
    }
}
