package com.brodong.enjoyable_animal_husbanding.client.render;

import com.brodong.enjoyable_animal_husbanding.Gender;
import com.brodong.enjoyable_animal_husbanding.GenderHelper;
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
 * 通过 EntityRenderersEvent.AddLayers 事件注册到所有生物实体渲染器上。
 */
public class GenderRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public GenderRenderLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        // ====== 步骤一：前置条件检查 ======

        // 隐身实体不显示指示器
        if (entity.isInvisible()) return;

        // 幼年实体不显示指示器
        if (entity.isBaby()) return;

        // 配置中已关闭性别显示
        if (!Config.showGenderIndicator) return;

        // ====== 步骤二：获取性别信息 ======

        // 通过 GenderHelper 读取性别（服务端读 PersistentData，客户端读同步缓存）
        Gender gender = GenderHelper.getGender(entity);

        // 性别未指定则跳过
        if (gender == Gender.None) return;

        // 根据性别选择 Unicode 符号：♂（U+2642）或 ♀（U+2640）
        String symbol = gender == Gender.Male ? "\u2642" : "\u2640";

        // 根据性别选择颜色：雄性蓝色 0x5599FF，雌性粉色 0xFF77AA
        int color = gender == Gender.Male ? 0x5599FF : 0xFF77AA;

        // ====== 步骤三：坐标变换 —— 定位到实体头顶 ======

        // 保存当前渲染矩阵
        poseStack.pushPose();

        // 将绘制原点沿 Y 轴平移到实体碰撞箱顶部上方
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);

        // ====== 步骤四：摄像机朝向 —— 使符号始终面向玩家 ======

        // 获取当前摄像机朝向的四元数并应用到矩阵
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // ====== 步骤五：缩放 ======

        // 缩小符号到合适大小，负号用于水平镜像反转
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        // ====== 步骤六：字体绘制 ======

        // 获取当前变换矩阵
        Matrix4f matrix4f = poseStack.last().pose();

        // 获取 Minecraft 内置字体渲染器
        Font font = Minecraft.getInstance().font;

        // 计算水平居中偏移
        float x = (float) (-font.width(symbol) / 2);

        // 调用字体渲染器在 3D 空间绘制文字（穿透模式，即使被方块遮挡也能看到）
        font.drawInBatch(symbol, x, 0.0F, color, false,
                matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);

        // ====== 步骤七：恢复矩阵状态 ======

        // 弹出矩阵，避免影响后续渲染
        poseStack.popPose();
    }
}
