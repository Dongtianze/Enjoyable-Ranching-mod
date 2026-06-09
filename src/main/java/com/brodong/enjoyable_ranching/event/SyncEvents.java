package com.brodong.enjoyable_ranching.event;

import com.brodong.enjoyable_ranching.EnjoyableRanching;
import com.brodong.enjoyable_ranching.Gender;
import com.brodong.enjoyable_ranching.GenderHelper;
import com.brodong.enjoyable_ranching.network.GenderSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 性别数据同步事件：玩家追踪实体时推送性别到客户端。
 */
@Mod.EventBusSubscriber(modid = EnjoyableRanching.MODID)
public class SyncEvents {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (!(target instanceof Animal)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Gender gender = GenderHelper.getGender(target);
        EnjoyableRanching.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new GenderSyncPacket(target.getId(), gender));
    }
}
