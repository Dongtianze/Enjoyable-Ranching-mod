package com.brodong.enjoyable_animal_husbanding.network;

import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 性别同步数据包，将服务端分配的性别同步到客户端缓存。
 */
public class GenderSyncPacket {

    private final int entityId;
    private final Gender gender;

    public GenderSyncPacket(int entityId, Gender gender) {
        this.entityId = entityId;
        this.gender = gender;
    }

    /** 编码：写入实体 ID 和性别枚举序号 */
    public static void encode(GenderSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeEnum(packet.gender);
    }

    /** 解码：读取实体 ID 和性别枚举 */
    public static GenderSyncPacket decode(FriendlyByteBuf buf) {
        return new GenderSyncPacket(buf.readInt(), buf.readEnum(Gender.class));
    }

    /** 客户端处理器：根据实体 ID 查找 UUID 并更新缓存 */
    public static void handle(GenderSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 通过客户端世界中的实体 ID 反查实体 UUID
            if (net.minecraft.client.Minecraft.getInstance().level != null) {
                var entity = net.minecraft.client.Minecraft.getInstance().level.getEntity(packet.entityId);
                if (entity != null) {
                    GenderHelper.updateClientCache(entity.getUUID(), packet.gender);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
