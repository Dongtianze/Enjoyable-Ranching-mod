package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.EnjoyableRanching;
import com.brodong.enjoyable_ranching.GenderHelper;
import com.brodong.enjoyable_ranching.SatietyHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 繁殖与进食事件：
 * <ul>
 *   <li>同性别/鸡配对拦截</li>
 *   <li>喂食→饱食度（取消原版 inLove）</li>
 *   <li>捕猎奖励（狼/狐/猫/豹猫）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EnjoyableRanching.MODID)
public class BreedingEvents {

    /** 同性别拦截 + 鸡配对仅产经验 */
    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        if (!(parentA instanceof Animal) || !(parentB instanceof Animal)) return;

        if (GenderHelper.getGender(parentA) == GenderHelper.getGender(parentB)) {
            event.setCanceled(true);
            return;
        }

        if (parentA instanceof Chicken && parentB instanceof Chicken) {
            event.setCanceled(true);
            if (parentA.level() instanceof ServerLevel level
                    && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                level.addFreshEntity(new ExperienceOrb(level,
                        parentA.getX(), parentA.getY(), parentA.getZ(),
                        parentA.getRandom().nextInt(7) + 1));
            }
        }
    }

    /** 喂食→饱食度，取消 inLove */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Animal animal)) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.isEmpty() || !animal.isFood(held)) return;

        if (animal.getAge() < 0) {
            SatietyHelper.fillSatiety(animal);
            return;
        }

        event.setCanceled(true);
        SatietyHelper.fillSatiety(animal);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        animal.playSound(animal.getEatingSound(held), 1.0F, 1.0F);
    }

    /** 捕猎奖励：狼/狐/猫/豹猫猎杀时饱食度回满 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        var attacker = event.getSource().getEntity();
        if (attacker instanceof Wolf || attacker instanceof Fox
                || attacker instanceof Cat || attacker instanceof net.minecraft.world.entity.animal.Ocelot) {
            SatietyHelper.fillSatiety((Animal) attacker);
        }
    }
}
