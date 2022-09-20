package fr.dynamx.utils.client;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.contentpack.type.vehicle.SteeringWheelInfo;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * Provides some useful methods for rendering dynamx objects/debug
 *
 * @see ClientDynamXUtils
 */
public class DynamXRenderUtils {
    private static final Sphere sphere = new Sphere();

    public static void drawBoundingBox(Vector3f halfExtent, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(-halfExtent.x, -halfExtent.y, -halfExtent.z, halfExtent.x, halfExtent.y, halfExtent.z, red, green, blue, alpha);
    }

    public static void drawBoundingBox(Vector3f min, Vector3f max, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z, red, green, blue, alpha);
    }

    public static void drawBoundingBox(AxisAlignedBB aabb, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, red, green, blue, alpha);
    }

    public static void renderCar(ModularVehicleInfo<?> car, byte textureId) {
        Vector3fPool.openPool();
        GlQuaternionPool.openPool();
        /* Rendering the chassis */
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(car.getModel());
        GlStateManager.scale(car.getScaleModifier().x, car.getScaleModifier().y, car.getScaleModifier().z);
        vehicleModel.renderDefaultParts(textureId);//.renderGroups("Chassis", textureId);
        GlStateManager.scale(1 / car.getScaleModifier().x, 1 / car.getScaleModifier().y, 1 / car.getScaleModifier().z);

        /* Rendering the steering wheel */
        SteeringWheelInfo info = car.getSubPropertyByType(SteeringWheelInfo.class);
        if (info != null) {
            ObjObjectRenderer steeringWheel = vehicleModel.getObjObjectRenderer(info.getPartName());
            if (steeringWheel != null) {
                GlStateManager.pushMatrix();
                Vector3f center = info.getSteeringWheelPosition();
                //Translation to the steering wheel rotation point (and render pos)
                GlStateManager.translate(center.x, center.y, center.z);

                //Apply steering wheel base rotation
                if (info.getSteeringWheelBaseRotation() != null)
                    GlStateManager.rotate(GlQuaternionPool.get(info.getSteeringWheelBaseRotation()));
                //Scale it
                GlStateManager.scale(car.getScaleModifier().x, car.getScaleModifier().y, car.getScaleModifier().z);
                //Render it
                vehicleModel.renderGroup(steeringWheel, textureId);
                GlStateManager.popMatrix();
            }
        }

        /* Rendering light sources */
        if (!car.getLightSources().isEmpty()) {
            for (PartLightSource.CompoundLight source : car.getLightSources()) {
                GlStateManager.scale(car.getScaleModifier().x, car.getScaleModifier().y, car.getScaleModifier().z);
                vehicleModel.renderGroups(source.getPartName(), (byte) 0);
                GlStateManager.scale(1 / car.getScaleModifier().x, 1 / car.getScaleModifier().y, 1 / car.getScaleModifier().z);
            }
        }

        /* Rendering the wheels */
        car.getPartsByType(PartWheel.class).forEach(partWheel -> {
            if (partWheel.getDefaultWheelInfo().isModelValid()) {
                GlStateManager.pushMatrix();
                {
                    /* Translation to the wheel position */
                    GlStateManager.translate(partWheel.getPosition().x, partWheel.getPosition().y, partWheel.getPosition().z);
                    if (partWheel.isRight()) {
                        /* Wheel rotation (Right-Side)*/
                        GlStateManager.rotate(180, 0, 1, 0);
                    }
                    /*Rendering the wheels */
                    PartWheelInfo info1 = partWheel.getDefaultWheelInfo();
                    ObjModelRenderer model = DynamXContext.getObjModelRegistry().getModel(info1.getModel());
                    //System.out.println("Model is "+model+" "+info1.getTextures()+ " "+car.getTextures().get(textureId));
                    GlStateManager.scale(car.getScaleModifier().x, car.getScaleModifier().y, car.getScaleModifier().z);
                    if (car.getTextures().containsKey(textureId))
                        model.renderModel(info1.getIdForTexture(car.getTextures().get(textureId).getName()));
                    else
                        model.renderModel((byte) 0);
                }
                GlStateManager.popMatrix();
            }
        });

        /* Rendering doors */
        for (PartDoor source : car.getPartsByType(PartDoor.class)) {
            GlStateManager.pushMatrix();
            {
                Vector3f pos = Vector3fPool.get(source.getCarAttachPoint());
                pos.subtractLocal(source.getDoorAttachPoint().x, source.getDoorAttachPoint().y, source.getDoorAttachPoint().z);
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.scale(car.getScaleModifier().x, car.getScaleModifier().y, car.getScaleModifier().z);
                vehicleModel.renderGroups(source.getPartName(), textureId);
            }
            GlStateManager.popMatrix();
        }
        GlQuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    public static void drawSphere(Vector3f translation, float radius, int resolution, @Nullable Color sphereColor) {
        if (sphereColor != null)
            GlStateManager.color(sphereColor.getRed(), sphereColor.getGreen(), sphereColor.getBlue(), sphereColor.getAlpha());
        GlStateManager.translate(translation.x, translation.y, translation.z);
        sphere.draw(radius, resolution, resolution);
        GlStateManager.translate(-translation.x, -translation.y, -translation.z);
        if (sphereColor != null)
            GlStateManager.color(1, 1, 1, 1);
    }

    public static void glTranslate(Vector3f translation) {
        GlStateManager.translate(translation.x, translation.y, translation.z);
    }

    public static void drawConvexHull(List<Vector3f> vectorBuffer) {
        Vector3fPool.openPool();
        GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GlStateManager.glBegin(GL11.GL_TRIANGLES);
        int index = 0;
        int size = vectorBuffer.size();
        for (int i = 0; i < size; i++) {
            int i1 = index++;
            int i2 = index++;
            int i3 = index++;

            if (i1 < size && i2 < size && i3 < size) {

                Vector3f v1 = vectorBuffer.get(i1);
                Vector3f v2 = vectorBuffer.get(i2);
                Vector3f v3 = vectorBuffer.get(i3);
                Vector3f normal = Vector3fPool.get(v3.subtract(v1)).multLocal(v2.subtract(v1));
                normal = normal.normalize();
                GlStateManager.glNormal3f(normal.x, normal.y, normal.z);
                GlStateManager.glVertex3f(v1.x, v1.y, v1.z);
                GlStateManager.glVertex3f(v2.x, v2.y, v2.z);
                GlStateManager.glVertex3f(v3.x, v3.y, v3.z);
            }

        }
        GlStateManager.glEnd();
        GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        Vector3fPool.closePool();
    }

    public static void drawNameplate(FontRenderer fontRendererIn, String str, float x, float y, float z, Quaternion rotation, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(rotation);
        GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) (isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        int i = fontRendererIn.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(-i - 1, -1 + verticalShift, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos(-i - 1, 8 + verticalShift, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos(i + 1, 8 + verticalShift, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos(i + 1, -1 + verticalShift, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        GlStateManager.depthMask(true);
        fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, verticalShift, -1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static int genVertexArrays() {
        return Minecraft.IS_RUNNING_ON_MAC ? APPLEVertexArrayObject.glGenVertexArraysAPPLE() : GL30.glGenVertexArrays();
    }

    public static void bindVertexArray(int vaoID) {
        if (Minecraft.IS_RUNNING_ON_MAC) {
            APPLEVertexArrayObject.glBindVertexArrayAPPLE(vaoID);
        } else {
            GL30.glBindVertexArray(vaoID);
        }
    }
}
