package com.brodong.enjoyable_animal_husbanding.event;

import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
import com.brodong.enjoyable_animal_husbanding.Enjoyable_animal_husbanding;
import com.brodong.enjoyable_animal_husbanding.network.GenderSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

/**
 * 模组事件处理器，负责：
 * 1. 动物生成时随机分配性别
 * 2. 玩家追踪实体时同步性别到客户端
 * 3. 同性别繁殖事件拦截
 */
@Mod.EventBusSubscriber(modid = Enjoyable_animal_husbanding.MODID)
public class ModEvents {

    private static final Random RANDOM = new Random();

    /**
     * 实体加入世界时，服务端为 Animal 随机分配性别并存储到持久化数据
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        // 只处理 Animal 实体
        if (!(event.getEntity() instanceof Animal animal)) return;
        // 检查是否已有性别数据（从存档加载的实体会保留性别）
        if (GenderHelper.getGender(animal) != Gender.None) return;
        // 随机分配性别并写入持久化数据
        Gender gender = RANDOM.nextBoolean() ? Gender.Male : Gender.Female;
        GenderHelper.setGender(animal, gender);
    }

    /**
     * 玩家开始追踪实体时，将性别数据同步到该玩家的客户端
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        // 只同步动物实体
        if (!(target instanceof Animal)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Gender gender = GenderHelper.getGender(target);
        // 发送同步包到该玩家
        Enjoyable_animal_husbanding.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new GenderSyncPacket(target.getId(), gender));
    }

    /**
     * 实体离开世界时，清理客户端性别缓存
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // 仅在客户端清理缓存
        if (event.getLevel().isClientSide()) {
            GenderHelper.removeFromClientCache(event.getEntity().getUUID());
        }
    }

    /**
     * 繁殖事件：拦截同性别动物的繁殖尝试
     */
    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        // 双方都必须是 Animal 且具有性别属性
        if (!(parentA instanceof Animal) || !(parentB instanceof Animal)) return;
        // 比较性别，相同则取消繁殖
        if (GenderHelper.getGender(parentA) == GenderHelper.getGender(parentB)) {
            event.setCanceled(true);
        }
    }
}
