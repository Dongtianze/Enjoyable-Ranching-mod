package com.brodong.enjoyable_animal_husbanding.mixin;

import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 繁殖 AI 的 Mixin，在原版找到伴侣后进行性别过滤。
 * 与 AnimalMixin.canMate() 形成双重保障：AI 层面阻止配对 + 繁殖底层兜底拦截。
 */
@Mixin(BreedGoal.class)
public abstract class BreedGoalMixin {

    /** 执行此 AI 的动物实体 */
    @Shadow
    @Final
    protected Animal animal;

    /** 原版 AI 找到的目标伴侣，可能为 null */
    @Shadow
    @Nullable
    protected Animal partner;

    /**
     * 在 canUse() 返回前注入，对已找到的伴侣进行性别检查。
     * 性别相同则清空伴侣并阻止 AI 进入繁殖流程。
     */
    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void enjoyable_animal_husbanding$filterSameGenderPartner(CallbackInfoReturnable<Boolean> cir) {
        // 仅在原版找到了伴侣的前提下进行性别比对
        if (cir.getReturnValue() && this.partner != null) {
            // 通过性别接口获取双方性别并比较
            GenderAccessor selfAccessor = (GenderAccessor) this.animal;
            GenderAccessor partnerAccessor = (GenderAccessor) this.partner;
            if (selfAccessor.getGender() == partnerAccessor.getGender()) {
                // 性别相同：清空伴侣引用，阻止 AI 执行
                this.partner = null;
                cir.setReturnValue(false);
            }
        }
    }
}
