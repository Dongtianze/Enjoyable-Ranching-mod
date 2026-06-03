package com.brodong.enjoyable_animal_husbanding.mixin;

import com.brodong.enjoyable_animal_husbanding.accessor.Gender;
import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 动物性别系统的核心 Mixin，注入到 {@link Animal} 类中。
 * <p>
 * 主要功能：
 * <ol>
 *   <li>为每个 Animal 实体添加 {@link Gender} 属性（雄性 / 雌性 / 无）</li>
 *   <li>在实体构造时随机分配性别（50% 概率）</li>
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
     * 当前动物的性别，默认为 {@link Gender#None}。
     * 在构造函数注入中会被随机赋值为 Male 或 Female。
     */
    @Unique
    private Gender gender = Gender.None;

    /**
     * 日志记录器，用于输出繁殖拦截等调试信息。
     */
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 在 {@link Animal} 构造函数执行完毕后注入，随机为实体分配性别。
     *
     * @param type  实体类型
     * @param level 所在世界
     * @param ci    Mixin 回调信息
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$initGender(EntityType<? extends Animal> type, Level level, CallbackInfo ci) {
        this.gender = RANDOM.nextBoolean() ? Gender.Male : Gender.Female;
    }

    @Override
    @Unique
    public Gender getGender() {
        return this.gender;
    }

    @Override
    @Unique
    public void setGender(Gender gender) {
        this.gender = gender;
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
        tag.putString("enjoyable_animal_husbanding:gender", this.gender.name());
    }

    /**
     * 在实体从 NBT 加载后注入，从 CompoundTag 中读取性别。
     * <p>
     * 若 NBT 中不存在性别数据（如旧版本存档）或数据格式异常，
     * 则将性别设为 {@link Gender#None}，由构造函数注入重新随机分配。
     *
     * @param tag 实体的 NBT 数据标签
     * @param ci  Mixin 回调信息
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void enjoyable_animal_husbanding$loadGender(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("enjoyable_animal_husbanding:gender")) {
            try {
                this.gender = Gender.valueOf(tag.getString("enjoyable_animal_husbanding:gender"));
            } catch (IllegalArgumentException e) {
                this.gender = Gender.None;
            }
        }
    }
}
