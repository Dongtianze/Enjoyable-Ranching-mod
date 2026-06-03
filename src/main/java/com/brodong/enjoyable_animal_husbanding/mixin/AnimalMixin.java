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
 * 动物性别系统的核心 Mixin，注入到 {@link Animal} 类中。
 * <p>
 * 通过 {@link SynchedEntityData}（实体数据同步系统）管理性别，
 * 确保服务端与客户端数据一致。主要功能：
 * <ol>
 *   <li>为每个 Animal 实体添加 {@link Gender} 属性（雄性 / 雌性）</li>
 *   <li>在实体构造时在服务端随机分配性别，客户端通过数据同步自动获得</li>
 *   <li>拦截原版 {@code canMate()} 方法，阻止同性别动物繁殖</li>
 *   <li>通过 NBT 持久化性别数据，确保重新加载世界后性别不变</li>
 * </ol>
 *
 * @see GenderAccessor
 * @see BreedGoalMixin
 */
@Mixin(Animal.class)
public abstract class AnimalMixin implements GenderAccessor {

    /**
     * 用于随机生成性别的共享随机数实例。
     */
    @Unique
    private static final Random RANDOM = new Random();

    /**
     * 日志记录器，用于输出繁殖拦截等调试信息。
     */
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 性别数据同步参数，向客户端同步性别字符串。
     * <p>
     * 使用 {@link EntityDataSerializers#STRING} 类型，
     * 存储 {@link Gender#name()} 的字符串值。
     * 通过 {@link SynchedEntityData} 自动在服务端和客户端之间同步。
     */
    @Unique
    private static final EntityDataAccessor<String> GENDER_DATA =
            SynchedEntityData.defineId(Animal.class, EntityDataSerializers.STRING);

    /**
     * 影子字段，引用原版 {@link net.minecraft.world.entity.Entity} 中的
     * {@code entityData} 字段，用于注册和读写同步数据。
     */
    @Shadow
    protected SynchedEntityData entityData;

    /**
     * 在 {@link net.minecraft.world.entity.Entity#defineSynchedData()} 末尾注入，
     * 将性别数据参数注册到实体的数据管理器。
     * <p>
     * 此方法在实体构造期间被调用，早于 {@code <init>} 注入点，
     * 因此后续对 {@link #GENDER_DATA} 的读写操作是安全的。
     *
     * @param ci Mixin 回调信息
     */
    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$defineGenderData(CallbackInfo ci) {
        this.entityData.define(GENDER_DATA, Gender.None.name());
    }

    /**
     * 在 {@link Animal} 构造函数执行完毕后注入，仅在服务端随机分配性别。
     * <p>
     * 客户端实体的性别通过 {@link SynchedEntityData} 从服务端同步获得，
     * 因此客户端不做随机赋值，避免每次进入世界时性别变化。
     *
     * @param type  实体类型
     * @param level 所在世界
     * @param ci    Mixin 回调信息
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$initGender(EntityType<? extends Animal> type, Level level, CallbackInfo ci) {
        if (!level.isClientSide) {
            this.setGenderDirect(RANDOM.nextBoolean() ? Gender.Male : Gender.Female);
        }
    }

    /**
     * 通过实体数据管理器获取当前性别。
     * <p>
     * 若存储的字符串无法解析为有效的 {@link Gender} 枚举值
     * （如数据损坏或旧版本存档），则安全回退为 {@link Gender#None}。
     *
     * @return 当前动物的性别
     */
    @Override
    @Unique
    public Gender getGender() {
        try {
            return Gender.valueOf(this.entityData.get(GENDER_DATA));
        } catch (IllegalArgumentException e) {
            return Gender.None;
        }
    }

    /**
     * 通过实体数据管理器设置性别，此操作会自动同步到客户端。
     *
     * @param gender 目标性别
     */
    @Override
    @Unique
    public void setGender(Gender gender) {
        this.setGenderDirect(gender);
    }

    /**
     * 直接写入性别数据，防止 {@link #setGender(Gender)} 被子类覆写时
     * 导致内部同步机制失效。
     *
     * @param gender 目标性别
     */
    @Unique
    private void setGenderDirect(Gender gender) {
        this.entityData.set(GENDER_DATA, gender.name());
    }

    /**
     * 在 {@link Animal#canMate(Animal)} 执行前拦截，检查双方性别。
     * <p>
     * 若双方性别相同，则取消原版繁殖逻辑并返回 {@code false}，
     * 从而阻止同性别动物之间繁殖。性别不同时不作干预，交由原版逻辑继续判断。
     *
     * @param mate 被检测的繁殖对象
     * @param cir  可取消的回调，用于设置返回值
     */
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void enjoyable_animal_husbanding$checkGenderBeforeMate(Animal mate, CallbackInfoReturnable<Boolean> cir) {
        if (mate instanceof GenderAccessor partnerAccessor) {
            Gender myGender = this.getGender();
            Gender partnerGender = partnerAccessor.getGender();

            if (myGender == partnerGender) {
                LOGGER.debug("Breeding blocked: same gender ({}).", myGender);
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * 在实体保存到 NBT 后注入，将性别写入 CompoundTag。
     * <p>
     * 使用 {@code enjoyable_animal_husbanding:gender} 作为键名，
     * 以命名空间前缀避免与其他模组的数据冲突。
     *
     * @param tag 实体的 NBT 数据标签
     * @param ci  Mixin 回调信息
     */
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$saveGender(CompoundTag tag, CallbackInfo ci) {
        tag.putString("enjoyable_animal_husbanding:gender", this.getGender().name());
    }

    /**
     * 在实体从 NBT 加载后注入，从 CompoundTag 中读取性别并写入同步数据管理器。
     * <p>
     * 由于使用 {@link SynchedEntityData} 存储性别，读取后需通过
     * {@link #setGenderDirect(Gender)} 写入，确保数据不仅在服务端恢复，
     * 还能在后续实体追踪建立后自动同步到客户端。
     * <p>
     * 若 NBT 中不存在性别数据（如旧版本存档或新生成的实体）或数据格式异常，
     * 则不做处理，保留构造时分配的随机值。
     *
     * @param tag 实体的 NBT 数据标签
     * @param ci  Mixin 回调信息
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$loadGender(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("enjoyable_animal_husbanding:gender")) {
            try {
                Gender gender = Gender.valueOf(tag.getString("enjoyable_animal_husbanding:gender"));
                this.setGenderDirect(gender);
            } catch (IllegalArgumentException e) {
                this.entityData.set(GENDER_DATA, Gender.None.name());
            }
        }
    }
}
