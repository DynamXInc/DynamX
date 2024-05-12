package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A type of root node, corresponding to a block
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public class BlockNode<A extends BlockObject<?>> extends AbstractItemNode<BaseRenderContext.BlockRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneNode<BaseRenderContext.BlockRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    @Getter
    private Matrix4f transform = new Matrix4f();

    public float[] transformData = new float[16];
    @Nullable
    public float[] transformDataGuizmo;

    public float[] translation = new float[3];
    public float[] prevTranslation = new float[3];

    public float[] rotation = new float[3];
    public float[] prevRotation = new float[3];

    public float[] scale = new float[3];
    public float[] prevScale = new float[3];

    public BaseRenderContext.BlockRenderContext context;

    @Override
    public void render(BaseRenderContext.BlockRenderContext context, A packInfo) {
        this.context = context;
        if (transformDataGuizmo == null) {
            transformDataGuizmo = new float[16];
            Arrays.fill(transformDataGuizmo, 0);
            transformDataGuizmo[6] = 1;
            transformDataGuizmo[7] = 1;
            transformDataGuizmo[8] = 1;
        }
        if (context.getTileEntity() != null && context.getTileEntity().getBlockType() instanceof DynamXBlock) { //the instanceof fixes a crash
            transform.identity();


            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            TEDynamXBlock te = context.getTileEntity();
            applyTransform(te, context.getRenderPosition());

            //Rendering the model
            DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
            if (model instanceof GltfModelRenderer) {
                te.getAnimator().update((GltfModelRenderer) model, context.getPartialTicks());

                te.getAnimator().setModelAnimations(((GltfModelRenderer) model).animations);
            }
            // Scale of the block object info scale modifier
            transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));

            transformDataGuizmo[6] += (scale[0] - prevScale[0]);
            transformDataGuizmo[7] += (scale[1] - prevScale[1]);
            transformDataGuizmo[8] += (scale[2] - prevScale[2]);

            if (!Float.isNaN(transformDataGuizmo[6]) && !Float.isNaN(transformDataGuizmo[7]) && !Float.isNaN(transformDataGuizmo[8])) {
                transform.scale(transformDataGuizmo[6], transformDataGuizmo[7], transformDataGuizmo[8]);
            }

            GlStateManager.pushMatrix();


            if (Keyboard.isKeyDown(Keyboard.KEY_G)) {
                for (int i = 0; i < 16; i++) {
                    transformDataGuizmo[i] = 0;
                    transformData[i] = 0;
                }
                transformDataGuizmo[6] = 1;
                transformDataGuizmo[7] = 1;
                transformDataGuizmo[8] = 1;
            }
            transform.get(transformData);

            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));

            model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            GlStateManager.popMatrix();
            //Render the linked children
            transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            linkedChildren.forEach(c -> c.render(context, packInfo));

            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            DynamXRenderUtils.popGlAllAttribBits();

            System.arraycopy(translation, 0, prevTranslation, 0, translation.length);
            System.arraycopy(rotation, 0, prevRotation, 0, rotation.length);
            System.arraycopy(scale, 0, prevScale, 0, scale.length);
        }
    }

    public void applyTransform(TEDynamXBlock te, Vector3f renderPos) {
        // Translate to block render pos and add the config translate value
        transform.translate((renderPos.x + 0.5f + te.getRelativeTranslation().x),
                (renderPos.y + 0.5f + te.getRelativeTranslation().y),
                (renderPos.z + 0.5f + te.getRelativeTranslation().z));

        transformDataGuizmo[0] += (translation[0] - prevTranslation[0]);
        transformDataGuizmo[1] += (translation[1] - prevTranslation[1]);
        transformDataGuizmo[2] += (translation[2] - prevTranslation[2]);
        transform.translate(transformDataGuizmo[0], transformDataGuizmo[1], transformDataGuizmo[2]);


        // Rotate to the config rotation value
        transform.rotate(DynamXUtils.toQuaternion(te.getCollidableRotation()));

        transformDataGuizmo[3] += (rotation[0] - prevRotation[0]);
        transformDataGuizmo[4] += (rotation[1] - prevRotation[1]);
        transformDataGuizmo[5] += (rotation[2] - prevRotation[2]);
        transform.rotate(transformDataGuizmo[3] * DynamXMath.TO_RADIAN, -1, 0, 0);
        transform.rotate(transformDataGuizmo[4] * DynamXMath.TO_RADIAN, 0, 1, 0);
        transform.rotate(transformDataGuizmo[5] * DynamXMath.TO_RADIAN, 0, 0, -1);

        // Translate of the block object info translation
        if (te.getRelativeScale().x > 0 && te.getRelativeScale().y > 0 && te.getRelativeScale().z > 0) {
            transform.translate(-0.5f, 0.5f, -0.5f);
            // Scale to the config scale value
            transform.scale((te.getRelativeScale().x != 0 ? te.getRelativeScale().x : 1),
                    (te.getRelativeScale().y != 0 ? te.getRelativeScale().y : 1),
                    (te.getRelativeScale().z != 0 ? te.getRelativeScale().z : 1));
            transform.translate(0.5f, 0.5f, 0.5f);
        }
        transform.translate(DynamXUtils.toVector3f(te.getPackInfo().getTranslation()));
    }

    @Override
    public void renderDebug(BaseRenderContext.BlockRenderContext context, A packInfo) {
        if (!(context instanceof BaseRenderContext.BlockRenderContext))
            return;
        TEDynamXBlock te = context.getTileEntity();
        if (te == null)
            return;
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        GlStateManager.pushMatrix();
        applyTransform(te, context.getRenderPosition());
        if (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()) {
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.5D, -1.5D, -0.5D);
            for (IShapeInfo partShape : te.getUnrotatedCollisionBoxes()) {
                RenderGlobal.drawBoundingBox(
                        (partShape.getPosition().x - partShape.getSize().x),
                        (partShape.getPosition().y - partShape.getSize().y),
                        (partShape.getPosition().z - partShape.getSize().z),
                        (partShape.getPosition().x + partShape.getSize().x),
                        (partShape.getPosition().y + partShape.getSize().y),
                        (partShape.getPosition().z + partShape.getSize().z),
                        0, 1, 1, 1);
            }
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
        }
        MutableBoundingBox box = new MutableBoundingBox();
        if (DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0D, -1.5D, 0D);
            for (PartStorage storage : (List<PartStorage>) packInfo.getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        1, 0.7f, 0, 1);
            }
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
        }
        linkedChildren.forEach(c -> c.renderDebug((BaseRenderContext.BlockRenderContext) context, packInfo));
        GlStateManager.popMatrix();
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    public void translate(float[] translation) {
        this.translation[0] += translation[0];
        this.translation[1] += translation[1];
        this.translation[2] += translation[2];
    }

    public void rotate(float[] rotation) {
        this.rotation[0] += rotation[0];
        this.rotation[1] += rotation[1];
        this.rotation[2] += rotation[2];
    }

    public void scale(float[] scale) {
        this.scale[0] += scale[0];
        this.scale[1] += scale[1];
        this.scale[2] += scale[2];
    }

    public void translate(Vector3f translation) {
        this.transformDataGuizmo[0] = translation.x;
        this.transformDataGuizmo[1] = translation.y;
        this.transformDataGuizmo[2] = translation.z;
    }

    public void rotate(Vector3f rotation) {
        this.transformDataGuizmo[3] = rotation.x;
        this.transformDataGuizmo[4] = rotation.y;
        this.transformDataGuizmo[5] = rotation.z;
    }

    public void scale(Vector3f scale) {
        this.transformDataGuizmo[6] = scale.x;
        this.transformDataGuizmo[7] = scale.y;
        this.transformDataGuizmo[8] = scale.z;
    }

    @Override
    public SceneNode<BaseRenderContext.BlockRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.BlockRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
