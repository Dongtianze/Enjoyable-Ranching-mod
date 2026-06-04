package com.brodong.enjoyable_ranching.item;

import com.brodong.enjoyable_ranching.Gender;
import com.brodong.enjoyable_ranching.GenderHelper;
import com.brodong.enjoyable_ranching.SatietyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 检查棒道具，右键点击动物时在聊天栏显示其性别与饱食度；
 * 右键玩家时显示其原版饥饿值。
 * 合成方式：木棍 + 钻石（无序合成）。
 */
public class CheckStickItem extends Item {

    public CheckStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        if (target instanceof Animal animal) {
            // 动物：显示性别 + 饱食度
            Gender gender = GenderHelper.getGender(animal);
            Component genderText = Component.translatable(
                    "item.enjoyable_ranching.check_stick.gender." + gender.name().toLowerCase());

            int satiety = SatietyHelper.getSatiety(animal);
            Component satietyText = Component.literal(satiety + "/" + SatietyHelper.MAX_SATIETY);

            player.displayClientMessage(
                    Component.translatable("item.enjoyable_ranching.check_stick.animal",
                            target.getDisplayName(), genderText, satietyText),
                    false);
            return InteractionResult.SUCCESS;
        }

        if (target instanceof Player targetPlayer) {
            // 玩家：显示原版饥饿值
            int foodLevel = targetPlayer.getFoodData().getFoodLevel();
            Component foodText = Component.literal(foodLevel + "/20");

            player.displayClientMessage(
                    Component.translatable("item.enjoyable_ranching.check_stick.player",
                            target.getDisplayName(), foodText),
                    false);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
