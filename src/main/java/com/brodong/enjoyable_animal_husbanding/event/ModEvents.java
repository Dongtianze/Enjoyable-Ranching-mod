package com.brodong.enjoyable_animal_husbanding.event;

import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
import com.brodong.enjoyable_animal_husbanding.Enjoyable_animal_husbanding;
import com.brodong.enjoyable_animal_husbanding.SatietyHelper;
import com.brodong.enjoyable_animal_husbanding.network.GenderSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

/**
 * 模组事件处理器，负责：
 * <ol>
 *   <li>动物生成时随机分配性别并初始化饱食度</li>
 *   <li>玩家追踪实体时同步性别到客户端</li>
 *   <li>同性别繁殖事件拦截</li>
 *   <li>拦截原版喂食→inLove，改为喂食→饱食度</li>
 *   <li>饱食度 >15 时随机进入 inLove（保留原版冷却）</li>
 *   <li>每 tick 更新饱食度、饥饿扣血、饱腹回血</li>
 *   <li>鸡下蛋替代原版机制</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Enjoyable_animal_husbanding.MODID)
public class ModEvents {

    private static final Random RANDOM = new Random();

    /** 饥饿扣血间隔（tick） */
    private static final int STARVE_DAMAGE_INTERVAL = 20;
    /** 饱食回血间隔（tick） */
    private static final int SATIETY_HEAL_INTERVAL = 60;
    /** 饱食度下降概率 */
    private static final int SATIETY_DECAY_PROBABILITY = 1200;
    /** 高饱食度下进入 inLove 的概率：1/64 每 tick */
    private static final int INLOVE_CHANCE = 64;

    // ==================== 实体生命周期 ====================

    /**
     * 实体加入世界时，分配性别、饱食度、自定义计时器。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;

        if (GenderHelper.getGender(animal) == Gender.None) {
            GenderHelper.setGender(animal, RANDOM.nextBoolean() ? Gender.Male : Gender.Female);
        }
        if (!animal.getPersistentData().contains("enjoyable_animal_husbanding:satiety")) {
            SatietyHelper.fillSatiety(animal);
        }
        animal.getPersistentData().putInt("enjoyable_animal_husbanding:last_damage_tick", animal.tickCount);
        animal.getPersistentData().putInt("enjoyable_animal_husbanding:last_heal_tick", animal.tickCount);

        if (animal instanceof Chicken chicken
                && !chicken.getPersistentData().contains("enjoyable_animal_husbanding:egg_timer")) {
            chicken.getPersistentData().putInt("enjoyable_animal_husbanding:egg_timer",
                    chicken.getRandom().nextInt(1200) + 1200);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            GenderHelper.removeFromClientCache(event.getEntity().getUUID());
        }
    }

    // ==================== 网络同步 ====================

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (!(target instanceof Animal)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Gender gender = GenderHelper.getGender(target);
        Enjoyable_animal_husbanding.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new GenderSyncPacket(target.getId(), gender));
    }

    // ==================== 繁殖拦截 ====================

    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        if (!(parentA instanceof Animal) || !(parentB instanceof Animal)) return;

        // 同性别拦截
        if (GenderHelper.getGender(parentA) == GenderHelper.getGender(parentB)) {
            event.setCanceled(true);
            return;
        }

        // 鸡不产生幼崽，仅掉落经验值
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

    // ==================== 喂食拦截：喂食→饱食度，不再触发 inLove ====================

    /**
     * 拦截玩家右键喂食动物，取消原版 setInLove，改为填充饱食度。
     * <p>
     * 成年动物：消耗食物 → 饱食度回满，不进入交配状态<br>
     * 幼年动物：保留原版成长加速行为
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Animal animal)) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.isEmpty() || !animal.isFood(held)) return;

        if (animal.getAge() < 0) {
            // 幼年动物：保留原版成长加速，额外填充饱食度
            SatietyHelper.fillSatiety(animal);
            return;
        }

        // 成年动物：取消原版 inLove，消耗食物填充饱食度
        event.setCanceled(true);
        SatietyHelper.fillSatiety(animal);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        animal.playSound(animal.getEatingSound(held), 1.0F, 1.0F);
    }

    // ==================== 每 tick 主循环 ====================

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;

        // 鸡下蛋逻辑
        if (animal instanceof Chicken chicken && !chicken.isBaby()) {
            handleChickenEggLaying(chicken);
        }

        CompoundTag data = animal.getPersistentData();
        int satiety = SatietyHelper.getSatiety(animal);
        int tickCount = animal.tickCount;

        // ===== 饱食度 >15 时随机进入 inLove（保留原版 canFallInLove 冷却） =====
        if (animal.getAge() == 0 && animal.canFallInLove() && satiety > 15) {
            if (RANDOM.nextInt(INLOVE_CHANCE) == 0) {
                animal.setInLove(null);
            }
        }

        // ===== 羊吃草检测：羊毛从剃光状态重新长出 → 饱食度回满 =====
        if (animal instanceof Sheep sheep) {
            boolean wasSheared = data.getBoolean("enjoyable_animal_husbanding:was_sheared");
            boolean isSheared = sheep.isSheared();
            if (wasSheared && !isSheared) {
                SatietyHelper.fillSatiety(animal);
                satiety = SatietyHelper.MAX_SATIETY;
            }
            data.putBoolean("enjoyable_animal_husbanding:was_sheared", isSheared);
        }

        // ===== 饱食度自然衰减 =====
        if (satiety > 0 && RANDOM.nextInt(SATIETY_DECAY_PROBABILITY) == 0) {
            SatietyHelper.setSatiety(animal, satiety - 1);
            satiety--;
        }

        // ===== 饥饿扣血 =====
        if (satiety == 0) {
            int lastDamage = data.getInt("enjoyable_animal_husbanding:last_damage_tick");
            if (tickCount - lastDamage >= STARVE_DAMAGE_INTERVAL) {
                animal.hurt(animal.damageSources().starve(), 1.0F);
                data.putInt("enjoyable_animal_husbanding:last_damage_tick", tickCount);
            }
        }

        // ===== 饱食回血 =====
        if (satiety > 15 && animal.getHealth() < animal.getMaxHealth()) {
            int lastHeal = data.getInt("enjoyable_animal_husbanding:last_heal_tick");
            if (tickCount - lastHeal >= SATIETY_HEAL_INTERVAL) {
                animal.heal(1.0F);
                data.putInt("enjoyable_animal_husbanding:last_heal_tick", tickCount);
            }
        }
    }

    // ==================== 鸡下蛋 ====================

    private static void handleChickenEggLaying(Chicken chicken) {
        if (chicken.eggTime < 999999) {
            chicken.eggTime = 999999;
        }

        CompoundTag data = chicken.getPersistentData();
        int timer = data.getInt("enjoyable_animal_husbanding:egg_timer") - 1;

        if (timer <= 0) {
            data.putInt("enjoyable_animal_husbanding:egg_timer",
                    chicken.getRandom().nextInt(6000) + 6000);

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
                                Enjoyable_animal_husbanding.CHICKEN_EGG.get().defaultBlockState(), 3);
                    }
                } else {
                    int count = chicken.getRandom().nextInt(2) + 1;
                    chicken.spawnAtLocation(new ItemStack(Items.FEATHER, count));
                }
            }
        } else {
            data.putInt("enjoyable_animal_husbanding:egg_timer", timer);
        }
    }
}
