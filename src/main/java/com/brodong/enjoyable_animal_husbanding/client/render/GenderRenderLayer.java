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

    /**
     * 构造渲染层，绑定到指定的生物实体渲染器。
     *
     * @param renderer 实体渲染器，作为本渲染层的父级
     */
    public GenderRenderLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    /**
     * 每帧渲染时调用，在实体模型绘制完成后叠加性别指示器。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *   <li><b>前置检查</b> — 跳过隐身实体、幼年实体、配置关闭、非动物实体、无性别实体</li>
     *   <li><b>获取性别信息</b> — 通过 {@link GenderAccessor} 读取性别，选择对应符号和颜色</li>
     *   <li><b>坐标变换</b> — 将绘制原点移到实体头顶上方</li>
     *   <li><b>摄像机朝向</b> — 旋转使其始终面向玩家（告示牌效果）</li>
     *   <li><b>缩放</b> — 缩小符号到合适大小</li>
     *   <li><b>字体绘制</b> — 使用 Minecraft 内置字体渲染 Unicode 符号</li>
     *   <li><b>恢复矩阵</b> — 弹出 PoseStack，避免影响后续渲染</li>
     * </ol>
     *
     * @param poseStack       姿态矩阵栈，控制渲染位置、旋转和缩放
     * @param buffer          多重缓冲源，用于提交渲染顶点数据
     * @param packedLight     打包的光照数据（天空光 + 方块光）
     * @param entity          当前被渲染的生物实体
     * @param limbSwing       肢体摆动角度
     * @param limbSwingAmount 肢体摆动幅度
     * @param partialTick     部分刻时间（用于动画插值）
     * @param ageInTicks      实体年龄（以 tick 计）
     * @param netHeadYaw      头部水平旋转角度
     * @param headPitch       头部垂直旋转角度
     */
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        // ====== 步骤一：前置条件检查 ======

        // 隐身状态 —— 使用药水或指令隐身的实体不显示指示器
        if (entity.isInvisible()) return;

        // 幼年实体 —— 幼年动物体型较小，指示器会显得突兀，跳过
        if (entity.isBaby()) return;

        // 配置开关 —— 玩家可在配置文件中关闭性别指示器
        if (!Config.showGenderIndicator) return;

        // 类型检查 —— 通过 instanceof 模式匹配判断实体是否具有性别属性
        // GenderAccessor 接口仅在 AnimalMixin 中被实现，因此非 Animal 实体不会通过检查
        if (!(entity instanceof GenderAccessor accessor)) return;

        // ====== 步骤二：获取性别信息 ======

        // 通过接口读取当前实体的性别
        Gender gender = accessor.getGender();

        // 性别未指定 —— 如数据异常或旧存档中的动物，不显示指示器
        if (gender == Gender.None) return;

        // 根据性别选择 Unicode 符号：♂（U+2642）或 ♀（U+2640）
        String symbol = gender == Gender.Male ? "\u2642" : "\u2640";

        // 根据性别选择指示器颜色：雄性蓝色 0x5599FF，雌性粉色 0xFF77AA
        int color = gender == Gender.Male ? 0x5599FF : 0xFF77AA;

        // ====== 步骤三：坐标变换 —— 定位到实体头顶 ======

        // 保存当前渲染矩阵状态，后续操作在此快照上进行
        poseStack.pushPose();

        // 将绘制原点沿 Y 轴平移到实体碰撞箱顶部上方 0.5 格处
        // getBbHeight() 返回实体碰撞箱高度（如牛约 1.4，鸡约 0.7）
        // +0.5F 确保符号不会与实体模型重叠
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);

        // ====== 步骤四：摄像机朝向 ======

        // 使符号始终面向玩家视角，类似于告示牌或名称标签的渲染方式
        // cameraOrientation() 返回一个四元数，表示当前摄像机的旋转状态
        // mulPose() 将当前矩阵与目标旋转相乘，实现"看向玩家"的效果
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // ====== 步骤五：缩放 ======

        // 缩小符号到合适大小
        // -0.025 的负号用于水平镜像反转（配合后续字体渲染的正确方向）
        // 第三个轴（Z）保持正值 0.025，确保深度写入方向正确
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        // ====== 步骤六：字体绘制 ======

        // 从矩阵栈中获取当前变换矩阵（4x4 齐次坐标矩阵）
        // poseStack.last() 返回最顶层的矩阵条目
        // .pose() 获取该条目中的位置矩阵（包含位移、旋转、缩放信息）
        Matrix4f matrix4f = poseStack.last().pose();

        // 获取 Minecraft 内置字体渲染器实例
        Font font = Minecraft.getInstance().font;

        // 计算符号的水平居中偏移量
        // font.width() 返回该字符串在屏幕上的像素宽度
        // 除以 2 取负，使符号从中心向两侧展开，实现水平居中
        float x = (float) (-font.width(symbol) / 2);

        // 调用字体渲染器在 3D 空间绘制文字
        // 参数说明：
        //   symbol        - 要渲染的字符串（♂ 或 ♀）
        //   x, 0.0F       - 文本左上角坐标（已居中处理）
        //   color         - 文字颜色（ARGB 格式：蓝色 0x5599FF 或粉色 0xFF77AA）
        //   false         - 是否显示阴影（false = 不显示）
        //   matrix4f      - 变换矩阵（包含位置和朝向信息）
        //   buffer        - 顶点数据缓冲区
        //   SEE_THROUGH   - 显示模式（穿透显示，即使实体被方块遮挡也能看到）
        //   0             - 背景色（0 = 透明背景）
        //   packedLight   - 光照数据
        font.drawInBatch(symbol, x, 0.0F, color, false,
                matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);

        // ====== 步骤七：恢复矩阵状态 ======

        // 弹出之前保存的矩阵，将渲染状态恢复到 pushPose() 之前
        // 确保本层渲染不影响后续其他 RenderLayer 和实体模型的绘制
        poseStack.popPose();
    }
}
