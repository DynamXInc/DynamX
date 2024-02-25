package fr.dynamx.bb;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class OBBModelObject {
    public static final FloatBuffer FLOAT_BUFFER = BufferUtils.createFloatBuffer(16);
    public OBBModelScene scene;
    public ArrayList<OBBModelBox> boxes = new ArrayList<>();
    public HashMap<OBBModelBox, OBBModelBone> boneBinding = new HashMap<>();
    public ArrayList<IBoneUpdatePoseListener> boneUpdatePoseListeners = new ArrayList<>();

    public interface IBoneUpdatePoseListener {
        void onBoneUpdatePose(OBBModelBone bone);
    }

    public void updatePose() {
        scene.updatePose(this);
    }

    public void computePose() {
        scene.computePose();
        boneBinding.forEach((box, bone) -> {
            box.compute(new Matrix4f(bone.currentPose).translate(bone.origin));
        });
        return;
    }

    public void onBoneUpdatePose(OBBModelBone bone) {
        for (IBoneUpdatePoseListener boneUpdatePoseListener : boneUpdatePoseListeners) {
            boneUpdatePoseListener.onBoneUpdatePose(bone);
        }
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugBoxes() {
        boneBinding.forEach((box, bone) -> {
            GlStateManager.pushMatrix();
            FLOAT_BUFFER.rewind();

            FLOAT_BUFFER.put(bone.currentPose.m00);
            FLOAT_BUFFER.put(bone.currentPose.m01);
            FLOAT_BUFFER.put(bone.currentPose.m02);
            FLOAT_BUFFER.put(bone.currentPose.m03);
            FLOAT_BUFFER.put(bone.currentPose.m10);
            FLOAT_BUFFER.put(bone.currentPose.m11);
            FLOAT_BUFFER.put(bone.currentPose.m12);
            FLOAT_BUFFER.put(bone.currentPose.m13);
            FLOAT_BUFFER.put(bone.currentPose.m20);
            FLOAT_BUFFER.put(bone.currentPose.m21);
            FLOAT_BUFFER.put(bone.currentPose.m22);
            FLOAT_BUFFER.put(bone.currentPose.m23);
            FLOAT_BUFFER.put(bone.currentPose.m30);
            FLOAT_BUFFER.put(bone.currentPose.m31);
            FLOAT_BUFFER.put(bone.currentPose.m32);
            FLOAT_BUFFER.put(bone.currentPose.m33);

            FLOAT_BUFFER.flip();
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(FLOAT_BUFFER);
            GlStateManager.translate(bone.origin.getX(), bone.origin.getY(), bone.origin.getZ());
            box.renderDebugBox();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        });
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugAxis() {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        boxes.forEach((box) -> {
            box.axis.forEach((axis) -> {
                Tessellator tessellator = Tessellator.getInstance();
                tessellator.getBuffer().begin(3, DefaultVertexFormats.POSITION_COLOR);
                tessellator.getBuffer().pos(box.center.x, box.center.y, box.center.z).color(255, 0, 0, 255).endVertex();
                tessellator.getBuffer().pos(box.center.x + axis.x, box.center.y + axis.y, box.center.z + axis.z).color(255, 0, 0, 255).endVertex();
                tessellator.draw();
            });
            GlStateManager.glLineWidth(4.0F);
            box.axisNormal.forEach((axis) -> {
                Tessellator tessellator = Tessellator.getInstance();
                tessellator.getBuffer().begin(3, DefaultVertexFormats.POSITION_COLOR);
                tessellator.getBuffer().pos(box.center.x, box.center.y, box.center.z).color(0, 255, 0, 255).endVertex();
                tessellator.getBuffer().pos(box.center.x + axis.x, box.center.y + axis.y, box.center.z + axis.z).color(0, 255, 0, 255).endVertex();
                tessellator.draw();
            });
        });
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.enableTexture2D();
    }
}
