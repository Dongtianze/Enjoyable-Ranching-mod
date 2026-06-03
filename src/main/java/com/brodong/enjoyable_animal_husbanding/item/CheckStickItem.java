package com.brodong.enjoyable_animal_husbanding.item;

import com.brodong.enjoyable_animal_husbanding.accessor.Gender;
import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 检查棒道具，右键点击动物时在聊天栏显示其性别。
 * <p>
 * 使用方式：手持检查棒右键点击任意动物，即可查看该动物的性别
 * （雄性 / 雌性 / 未指定）。若目标不是动物，则提示无法检查。
 * <p>
 * 合成方式：木棍 + 钻石（无序合成）。
 */
public class CheckStickItem extends Item {

    public CheckStickItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键点击生物时触发，在服务端读取该生物的性别并发送消息给玩家。
     * <p>
     * 仅在目标实现了 {@link GenderAccessor} 接口时有效，
     * 即任何通过 {@link com.brodong.enjoyable_animal_husbanding.mixin.AnimalMixin}
     * 注入了性别的动物实体。
     *
     * @param stack  手持的物品栈
     * @param player 使用物品的玩家
     * @param target 被右键点击的生物
     * @param hand   使用物品的手（主手/副手）
     * @return 交互结果，处理成功返回 {@link InteractionResult#sidedSuccess}
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof GenderAccessor accessor) {
            if (!player.level().isClientSide) {
                Gender gender = accessor.getGender();
                Component genderText = Component.translatable("item.enjoyable_animal_husbanding.check_stick.gender." + gender.name().toLowerCase());
                player.displayClientMessage(
                        Component.translatable("item.enjoyable_animal_husbanding.check_stick.check",
                                target.getDisplayName(), genderText),
                        false);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
