package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.EnjoyableRanching;
import com.brodong.enjoyable_ranching.SatietyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 每 tick 主循环：饱食度衰减、饥饿扣血、饱腹回血、inLove 随机触发。
 * <p>
 * 调用 {@link GrazingHandler} 和 {@link ChickenEggHandler} 处理动物特殊行为。
 */
@Mod.EventBusSubscriber(modid = EnjoyableRanching.MODID)
public class LivingTickHandler {

    private static final Random RANDOM = new Random();

    static final int STARVE_DAMAGE_INTERVAL = 20;
    static final int SATIETY_HEAL_INTERVAL = 60;
    static final int SATIETY_DECAY_PROBABILITY = 1200;
    static final int INLOVE_CHANCE = 64;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;

        // 鸡下蛋
        if (animal instanceof Chicken chicken && !chicken.isBaby()) {
            ChickenEggHandler.handleChickenEggLaying(chicken);
        }

        CompoundTag data = animal.getPersistentData();
        int satiety = SatietyHelper.getSatiety(animal);
        int tickCount = animal.tickCount;

        // 饱食度 >15 → 随机进入 inLove
        if (animal.getAge() == 0 && animal.canFallInLove() && satiety > 15) {
            if (RANDOM.nextInt(INLOVE_CHANCE) == 0 && GrazingHandler.hasCompatibleMate(animal)) {
                animal.setInLove(null);
            }
        }

        // 各动物进食行为
        if (animal instanceof Sheep sheep && !sheep.isBaby()) {
            satiety = GrazingHandler.handleSheepGrazing(sheep, satiety, data);
        }
        if (animal instanceof Cow cow && !cow.isBaby()) {
            GrazingHandler.handleCowGrazing(cow, satiety);
        }
        if (animal instanceof Chicken chicken && !chicken.isBaby()) {
            GrazingHandler.handleChickenGrazing(chicken, satiety);
        }

        // 饱食度自然衰减
        if (satiety > 0 && RANDOM.nextInt(SATIETY_DECAY_PROBABILITY) == 0) {
            SatietyHelper.setSatiety(animal, satiety - 1);
            satiety--;
        }

        // 饥饿扣血
        if (satiety == 0) {
            int lastDamage = data.getInt("enjoyable_ranching:last_damage_tick");
            if (tickCount - lastDamage >= STARVE_DAMAGE_INTERVAL) {
                animal.hurt(animal.damageSources().starve(), 1.0F);
                data.putInt("enjoyable_ranching:last_damage_tick", tickCount);
            }
        }

        // 饱腹回血
        if (satiety > 15 && animal.getHealth() < animal.getMaxHealth()) {
            int lastHeal = data.getInt("enjoyable_ranching:last_heal_tick");
            if (tickCount - lastHeal >= SATIETY_HEAL_INTERVAL) {
                animal.heal(1.0F);
                data.putInt("enjoyable_ranching:last_heal_tick", tickCount);
            }
        }
    }
}
