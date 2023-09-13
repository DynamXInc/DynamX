package fr.dynamx.utils.client;

import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.mesh.shapes.ArrowMesh;
import fr.dynamx.client.renders.mesh.shapes.GridGLMesh;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
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
import net.minecraft.world.World;
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

    public static GridGLMesh gridMesh;
    public static ArrowMesh arrowMeshX;
    public static ArrowMesh arrowMeshY;
    public static ArrowMesh arrowMeshZ;
    public static IcosphereGLMesh icosphereMesh;
    public static OctasphereMesh sphereMesh;

    public static IcosphereGLMesh icosphereMeshToRender;


    private static final Sphere sphere = new Sphere();

    public static void initGlMeshes(){
        gridMesh = new GridGLMesh(10,10,0.2f);
        arrowMeshX = ArrowMesh.getMesh(0);
        arrowMeshY = ArrowMesh.getMesh(1);
        arrowMeshZ = ArrowMesh.getMesh(2);
        icosphereMesh = new IcosphereGLMesh(3, true);
        icosphereMesh.generateUvs(UvsOption.Spherical,
                new Vector4f(0f, 1f, 0f, 0f), new Vector4f(0f, 0f, 1f, 0f));
        icosphereMeshToRender = new IcosphereGLMesh(3, true);
        sphereMesh = new OctasphereMesh(3, true);
    }

    public static void drawBoundingBox(Vector3f halfExtent, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(-halfExtent.x, -halfExtent.y, -halfExtent.z, halfExtent.x, halfExtent.y, halfExtent.z, red, green, blue, alpha);
    }

    public static void drawBoundingBox(Vector3f min, Vector3f max, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z, red, green, blue, alpha);
    }

    public static void drawBoundingBox(AxisAlignedBB aabb, float red, float green, float blue, float alpha) {
        RenderGlobal.drawBoundingBox(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, red, green, blue, alpha);
    }

    private static RenderBaseVehicle<?> renderBaseVehicle;

    public static void renderCar(ModularVehicleInfo car, byte textureId) {
        if (renderBaseVehicle == null)
            renderBaseVehicle = new RenderBaseVehicle<>(ClientEventHandler.MC.getRenderManager());
        Vector3fPool.openPool();
        GlQuaternionPool.openPool();
        renderBaseVehicle.renderMain(null, car, textureId, 1);
        renderBaseVehicle.renderParts(null, car, textureId, 1);
        GlQuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    public static void drawSphere(Vector3f translation, float radius, @Nullable Color sphereColor) {
        if (sphereColor != null)
            GlStateManager.color(sphereColor.getRed()/255f, sphereColor.getGreen()/255f, sphereColor.getBlue()/255f, sphereColor.getAlpha()/255f);
        GlStateManager.translate(translation.x, translation.y, translation.z);
        GlStateManager.scale(radius, radius, radius);
        sphereMesh.render();
        GlStateManager.scale(1/radius, 1/radius, 1/radius);
        GlStateManager.translate(-translation.x, -translation.y, -translation.z);
        if (sphereColor != null)
            GlStateManager.color(1, 1, 1, 1);
    }

    public static void glTranslate(Vector3f translation) {
        GlStateManager.translate(translation.x, translation.y, translation.z);
    }

    public static void drawConvexHull(List<Vector3f> vectorBuffer, boolean wireframe) {
        Vector3fPool.openPool();
        GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, wireframe ? GL11.GL_LINE : GL11.GL_FILL);
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

    public static void drawNameplate(FontRenderer fontRendererIn, String str, float x, float y, float z, PhysicsEntity<?> entity, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-entity.rotationYaw, 0, 1, 0);
        GlStateManager.rotate(-entity.rotationPitch, 1, 0, 0);
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

    public static void spawnParticles(ParticleEmitterInfo.IParticleEmitterContainer particleEmitterInfo, World world, Vector3f initialPos, Vector3f initialRot) {
        particleEmitterInfo.getParticleEmitters()
                .forEach(emitterInfo -> {
                    Vector3f rotatedPoint = DynamXGeometry.getRotatedPoint(emitterInfo.position, initialRot.x, initialRot.y, initialRot.z);
                    world.spawnParticle(emitterInfo.particleType,
                            initialPos.x + rotatedPoint.x,
                            initialPos.y + rotatedPoint.y,
                            initialPos.z + rotatedPoint.z,
                            emitterInfo.velocity.x,
                            emitterInfo.velocity.y,
                            emitterInfo.velocity.z);
                });
    }

    public static void spawnParticles(ParticleEmitterInfo.IParticleEmitterContainer particleEmitterInfo, World world, Vector3f initialPos, com.jme3.math.Quaternion initialRot) {
        particleEmitterInfo.getParticleEmitters()
                .forEach(emitterInfo -> {
                    Vector3f rotatedPoint = DynamXGeometry.rotateVectorByQuaternion(emitterInfo.position, initialRot);
                    world.spawnParticle(emitterInfo.particleType,
                            initialPos.x + rotatedPoint.x,
                            initialPos.y + rotatedPoint.y,
                            initialPos.z + rotatedPoint.z,
                            emitterInfo.velocity.x,
                            emitterInfo.velocity.y,
                            emitterInfo.velocity.z);
                });
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

    public static void checkForOglError() {
        int errorCode = GL11.glGetError();
        if (errorCode != GL11.GL_NO_ERROR) {
            DynamXMain.log.warn("errorCode = " + errorCode);
        }
    }
}
