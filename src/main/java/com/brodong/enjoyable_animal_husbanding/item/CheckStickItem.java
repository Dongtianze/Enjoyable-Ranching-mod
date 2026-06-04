package com.brodong.enjoyable_animal_husbanding.item;

import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 检查棒道具，右键点击动物时在聊天栏显示其性别。
 * 合成方式：木棍 + 钻石（无序合成）。
 */
public class CheckStickItem extends Item {

    public CheckStickItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键点击生物时触发，在服务端读取该生物的性别并发送消息给玩家。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // 只对动物实体生效
        if (!(target instanceof Animal)) return InteractionResult.PASS;
        // 仅在服务端处理，通过 PersistentData 读取性别
        if (!player.level().isClientSide) {
            // 获取目标性别
            Gender gender = GenderHelper.getGender(target);
            // 构建可翻译的性别文本（支持多语言）
            Component genderText = Component.translatable(
                    "item.enjoyable_animal_husbanding.check_stick.gender." + gender.name().toLowerCase());
            // 向玩家显示格式化消息："XXX的性别是：雌性/雄性"
            player.displayClientMessage(
                    Component.translatable("item.enjoyable_animal_husbanding.check_stick.check",
                            target.getDisplayName(), genderText),
                    false);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
