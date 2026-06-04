package com.brodong.enjoyable_animal_husbanding;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性别数据工具类，统一管理性别的存取与客户端缓存。
 * 服务端通过 Entity#getPersistentData() 存储，客户端通过同步缓存读取。
 */
public class GenderHelper {

    /** 客户端性别缓存：从网络包同步，用于渲染时快速查询 */
    private static final Map<UUID, Gender> CLIENT_CACHE = new ConcurrentHashMap<>();

    /** PersistentData 中的 key */
    private static final String KEY = "enjoyable_animal_husbanding:gender";

    /**
     * 设置实体的性别（仅在服务端调用有效）
     */
    public static void setGender(Entity entity, Gender gender) {
        // 写入实体的持久化数据，随存档保存和加载
        entity.getPersistentData().putString(KEY, gender.name());
    }

    /**
     * 读取实体的性别。
     * 服务端从 PersistentData 读取，客户端从同步缓存读取。
     */
    public static Gender getGender(Entity entity) {
        if (entity.level().isClientSide) {
            // 客户端：查同步缓存，未收到同步数据则返回 None
            return CLIENT_CACHE.getOrDefault(entity.getUUID(), Gender.None);
        }
        // 服务端：从持久化数据中解析性别
        String raw = entity.getPersistentData().getString(KEY);
        if (raw.isEmpty()) {
            return Gender.None;
        }
        try {
            return Gender.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Gender.None;
        }
    }

    /**
     * 在客户端缓存性别数据（由网络包处理器调用）
     */
    public static void updateClientCache(UUID uuid, Gender gender) {
        CLIENT_CACHE.put(uuid, gender);
    }

    /**
     * 实体被移除时清理客户端缓存
     */
    public static void removeFromClientCache(UUID uuid) {
        CLIENT_CACHE.remove(uuid);
    }
}
