package com.brodong.enjoyable_animal_husbanding.client.event;

import com.brodong.enjoyable_animal_husbanding.Config;
import com.brodong.enjoyable_animal_husbanding.Enjoyable_animal_husbanding;
import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 客户端渲染事件处理器，负责在动物头顶绘制性别符号。
 * <p>
 * 通过 {@link RenderLivingEvent.Post} 事件进行渲染，此时 PoseStack 处于
 * 与命名牌完全相同的位置上下文（实体世界坐标，无模型变换），直接复用
 * 原版命名牌的摄像机朝向 + 缩放 + 字体绘制管线，确保符号始终正对玩家。
 */
@Mod.EventBusSubscriber(modid = Enjoyable_animal_husbanding.MODID, value = Dist.CLIENT)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // 仅对动物生效
        if (!(entity instanceof Animal)) return;
        // 隐身实体不显示
        if (entity.isInvisible()) return;
        // 幼年实体不显示
        if (entity.isBaby()) return;
        // 配置关闭则不显示
        if (!Config.showGenderIndicator) return;

        // 通过 GenderHelper 获取性别（客户端读缓存，服务端读 PersistentData）
        Gender gender = GenderHelper.getGender(entity);
        if (gender == Gender.None) return;

        // 性别符号：♂（U+2642）或 ♀（U+2640）
        String symbol = gender == Gender.Male ? "\u2642" : "\u2640";
        // 颜色：雄性蓝色 0x5599FF，雌性粉色 0xFF77AA
        int color = gender == Gender.Male ? 0x5599FF : 0xFF77AA;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        // 复刻原版 EntityRenderer.renderNameTag() 的渲染管线
        poseStack.pushPose();

        // ① 定位到实体头部上方（与命名牌同样的偏移逻辑）
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);

        // ② 摄像机朝向四元数 —— 使符号始终面向玩家（原版命名牌同款方案）
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // ③ 缩放到合适大小并镜像（与原版命名牌一致的缩放值）
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        // ④ 获取变换矩阵并计算文字居中偏移
        Matrix4f matrix4f = poseStack.last().pose();
        Font font = Minecraft.getInstance().font;
        float x = (float) (-font.width(symbol) / 2);

        // ⑤ 绘制文字（穿透模式，即使被方块遮挡也可见）
        font.drawInBatch(symbol, x, 0.0F, color, false,
                matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);

        poseStack.popPose();
    }
}
