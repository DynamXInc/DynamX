package fr.dynamx.client.renders;

import com.jme3.bullet.joints.Anchor;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.shaders.DxShader;
import fr.dynamx.client.shaders.ShaderManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.SoftbodyEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBufUtil;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nullable;
import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class RenderSoftbody<T extends SoftbodyEntity> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderSoftbody(RenderManager manager) {
        super(manager);
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(context.getRenderPosition().x, context.getRenderPosition().y, context.getRenderPosition().z);
        FacesMesh facesMesh = DynamXContext.getSoftbodyEntityMesh().get(entity);

        PhysicsSoftBody collisionObject = entity.physicsHandler.getCollisionObject();

        Vector3f physicsLocation = Vector3fPool.get();

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("dynamxmod", "textures/white.png"));

        collisionObject.getPhysicsLocation(physicsLocation);
        DynamXRenderUtils.glTranslate(physicsLocation.negateLocal());

        ShaderManager.sceneGrid.useShader();
        facesMesh.render(1);
        facesMesh.update(context.getPartialTicks());

        DxShader.stopShader();




        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();

       /*         IntBuffer faces = collisionObject.copyFaces(null);
        FloatBuffer nodeLocations = collisionObject.copyLocations(null);
        int numFaces = collisionObject.countFaces();
        Color col = new Color(0, 155, 0);
        for (int i = 0; i < numFaces; i++) {
            int vi1 = faces.get(3 * i);
            int vi2 = faces.get(3 * i + 1);
            int vi3 = faces.get(3 * i + 2);
            Vector3f nodePos1 = new Vector3f();
            Vector3f nodePos2 = new Vector3f();
            Vector3f nodePos3 = new Vector3f();
            MyBuffer.get(nodeLocations, 3 * vi1, nodePos1);
            MyBuffer.get(nodeLocations, 3 * vi2, nodePos2);
            MyBuffer.get(nodeLocations, 3 * vi3, nodePos3);
            Vector3f[] x = new Vector3f[]{nodePos1, nodePos2, nodePos3};
            Vector3f c = x[0].add(x[1]).add(x[2]).divideLocal(3);

            DynamXRenderUtils.glTranslate(c);
            GlStateManager.scale(0.01,0.01,0.01);
            GlStateManager.color(1,0,0,1);
            DynamXRenderUtils.icosphereMeshToRender.render();
            GlStateManager.color(1,1,1,1);
            GlStateManager.scale(1/0.01,1/0.01,1/0.01);
            DynamXRenderUtils.glTranslate(c.negateLocal());

        }*/


        GlStateManager.enableLighting();
        GlStateManager.enableDepth();

    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {

    }

    @Nullable
    @Override
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {

        return context.setModelParams(entity, null, (byte)0);
    }

}
