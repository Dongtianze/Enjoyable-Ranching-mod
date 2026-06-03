package com.brodong.enjoyable_animal_husbanding.mixin;

import com.brodong.enjoyable_animal_husbanding.accessor.Gender;
import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 动物性别系统的核心 Mixin，注入到 Animal 类中。
 * 通过 SynchedEntityData 管理性别，确保服务端与客户端数据一致。
 */
@Mixin(Animal.class)
public abstract class AnimalMixin implements GenderAccessor {

    /** 共享随机数，用于性别随机分配 */
    @Unique
    private static final Random RANDOM = new Random();

    /** 日志记录器，输出繁殖拦截等调试信息 */
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 性别数据同步键，向客户端同步性别字符串。
     * 使用 STRING 序列化，存储 Gender.name() 的值。
     */
    @Unique
    private static final EntityDataAccessor<String> GENDER_DATA =
            SynchedEntityData.defineId(Animal.class, EntityDataSerializers.STRING);

    /** 引用原版 Entity 中的 entityData 字段 */
    @Shadow
    protected SynchedEntityData entityData;

    /**
     * 注册性别数据参数到实体数据管理器，在实体构造早期执行。
     */
    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$defineGenderData(CallbackInfo ci) {
        // 将性别数据键注册到 SynchedEntityData，默认值为 None
        this.entityData.define(GENDER_DATA, Gender.None.name());
    }

    /**
     * 实体构造完成后，仅在服务端随机分配性别。
     * 客户端通过 SynchedEntityData 自动同步，无需本地随机。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$initGender(EntityType<? extends Animal> type, Level level, CallbackInfo ci) {
        // 服务端才做随机分配，客户端性别由数据同步系统获得
        if (!level.isClientSide) {
            // 50% 概率分配雄性/雌性
            this.setGenderDirect(RANDOM.nextBoolean() ? Gender.Male : Gender.Female);
        }
    }

    /**
     * 获取当前性别，解析异常时安全回退为 None。
     */
    @Override
    @Unique
    public Gender getGender() {
        try {
            // 从同步数据中读取并解析性别枚举
            return Gender.valueOf(this.entityData.get(GENDER_DATA));
        } catch (IllegalArgumentException e) {
            // 数据损坏或旧存档时回退
            return Gender.None;
        }
    }

    /**
     * 设置性别，此操作通过 SynchedEntityData 自动同步到客户端。
     */
    @Override
    @Unique
    public void setGender(Gender gender) {
        this.setGenderDirect(gender);
    }

    /**
     * 直接写入性别数据，防止子类覆写导致内部同步机制失效。
     */
    @Unique
    private void setGenderDirect(Gender gender) {
        this.entityData.set(GENDER_DATA, gender.name());
    }

    /**
     * 在 canMate() 执行前拦截，阻止同性别动物繁殖。
     */
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void enjoyable_animal_husbanding$checkGenderBeforeMate(Animal mate, CallbackInfoReturnable<Boolean> cir) {
        // 对方也实现了 GenderAccessor 接口才进行性别比对
        if (mate instanceof GenderAccessor partnerAccessor) {
            // 获取双方性别
            Gender myGender = this.getGender();
            Gender partnerGender = partnerAccessor.getGender();

            // 性别相同则取消繁殖
            if (myGender == partnerGender) {
                LOGGER.debug("Breeding blocked: same gender ({}).", myGender);
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * 保存性别到 NBT，使用命名空间前缀避免与其他模组冲突。
     */
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$saveGender(CompoundTag tag, CallbackInfo ci) {
        // 将性别枚举名称写入 NBT
        tag.putString("enjoyable_animal_husbanding:gender", this.getGender().name());
    }

    /**
     * 从 NBT 加载性别数据并写入同步数据管理器。
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$loadGender(CompoundTag tag, CallbackInfo ci) {
        // 检查 NBT 中是否存在该模组的性别数据
        if (tag.contains("enjoyable_animal_husbanding:gender")) {
            try {
                // 解析性别枚举并写入同步数据
                Gender gender = Gender.valueOf(tag.getString("enjoyable_animal_husbanding:gender"));
                this.setGenderDirect(gender);
            } catch (IllegalArgumentException e) {
                // 数据异常时重置为 None
                this.entityData.set(GENDER_DATA, Gender.None.name());
            }
        }
    }
}
