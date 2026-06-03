package com.brodong.enjoyable_animal_husbanding.client.render;

import com.brodong.enjoyable_animal_husbanding.accessor.Gender;
import com.brodong.enjoyable_animal_husbanding.accessor.GenderAccessor;
import com.brodong.enjoyable_animal_husbanding.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * 性别指示器渲染层，在动物头顶渲染 ♂ / ♀ 符号。
 * <p>
 * 通过 {@code EntityRenderersEvent.AddLayers} 事件注册到所有
 * {@link LivingEntity} 实体渲染器上。当实体实现了 {@link GenderAccessor}
 * 接口且性别不为 {@link Gender#None} 时，会在其头顶上方绘制对应的性别符号：
 * <ul>
 *   <li>雄性（Male）  → 蓝色 ♂</li>
 *   <li>雌性（Female）→ 粉色 ♀</li>
 * </ul>
 * <p>
 * 幼年实体和隐身实体不显示指示器。
 *
 * @param <T> 生物实体类型
 * @param <M> 实体模型类型
 */
public class GenderRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public GenderRenderLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || entity.isBaby()) return;
        if (!Config.showGenderIndicator) return;
        if (!(entity instanceof GenderAccessor accessor)) return;

        Gender gender = accessor.getGender();
        if (gender == Gender.None) return;

        String symbol = gender == Gender.Male ? "\u2642" : "\u2640";
        int color = gender == Gender.Male ? 0x5599FF : 0xFF77AA;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = poseStack.last().pose();
        Font font = Minecraft.getInstance().font;
        float x = (float) (-font.width(symbol) / 2);
        font.drawInBatch(symbol, x, 0.0F, color, false,
                matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);

        poseStack.popPose();
    }
}
