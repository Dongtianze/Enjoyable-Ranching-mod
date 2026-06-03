package com.brodong.enjoyable_animal_husbanding.mixin;

import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 繁殖 AI 目标的 Mixin，注入到 {@link BreedGoal} 类中。
 * <p>
 * 在原版中，BreedGoal 的 {@code canUse()} 方法会查找附近处于发情状态的
 * 同类动物作为繁殖对象，但不检查性别。此 Mixin 在伴侣查找完成后进行
 * 性别过滤：若双方性别相同，则清除伴侣引用并返回 {@code false}，
 * 防止 AI 尝试与同性动物繁殖。
 * <p>
 * 这与 {@link AnimalMixin} 中的 {@code canMate()} 拦截形成双重保障：
 * <ul>
 *   <li>BreedGoalMixin：在 AI 层面阻止配对，避免同性别动物互相靠近</li>
 *   <li>AnimalMixin：在繁殖判定层面兜底，即使某种方式绕过了 AI 也依然受限</li>
 * </ul>
 *
 * @see AnimalMixin
 * @see GenderAccessor
 */
@Mixin(BreedGoal.class)
public abstract class BreedGoalMixin extends Goal {

    /**
     * 执行此 AI 目标的动物实体（原版 BreedGoal 中的 {@code animal} 字段）。
     */
    @Shadow
    @Final
    protected Animal animal;

    /**
     * 原版 AI 找到的目标伴侣（原版 BreedGoal 中的 {@code partner} 字段）。
     * 可能为 {@code null} 表示尚未找到合适的伴侣。
     */
    @Shadow
    @Nullable
    protected Animal partner;

    /**
     * 在 {@link BreedGoal#canUse()} 返回前注入，对已找到的伴侣进行性别检查。
     * <p>
     * 若原版逻辑找到了伴侣（返回 {@code true} 且 partner 非 null），
     * 则比较自身和伴侣的性别。性别相同则清空伴侣并将返回值设为 {@code false}，
     * 阻止 AI 进入繁殖流程。
     *
     * @param cir 可取消的回调，用于获取和修改返回值
     */
    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void enjoyable_animal_husbanding$filterSameGenderPartner(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this.partner != null) {
            GenderAccessor selfAccessor = (GenderAccessor) this.animal;
            GenderAccessor partnerAccessor = (GenderAccessor) this.partner;
            if (selfAccessor.getGender() == partnerAccessor.getGender()) {
                this.partner = null;
                cir.setReturnValue(false);
            }
        }
    }
}
