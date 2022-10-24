package fr.dynamx.client.renders.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.CaterpillarInfo;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.vehicles.CaterpillarEntity;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector4f;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.lwjgl.opengl.GL11.*;

public class RenderCaterpillar<T extends CaterpillarEntity<?>> extends RenderBaseVehicle<T> {
    public RenderCaterpillar(RenderManager manager) {
        super(manager);
        VehicleDebugRenderer.addAll(this, true);
    }

    @Override
    public void renderParts(T carEntity, float partialTicks) {
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            WheelsModule module = carEntity.getModuleByType(WheelsModule.class);
            /* Rendering the steering wheel */
            GlStateManager.pushMatrix();
            {
                /* Translation to the origin */
                GlStateManager.translate(0.5, 1.1, 1);
                /* Rotation of the steering wheel */
                GlStateManager.rotate(-(module.prevVisualProperties[1] + (module.visualProperties.get()[1] - module.prevVisualProperties[1]) * partialTicks), 0.0F, 0.0F, 1.0F);
                /* Translate with the same values but negative to move it to it normal position*/
                GlStateManager.translate(-0.5, -1.1, -1);
                /* Rendering the steering wheel */
                DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel()).renderGroups("SteeringWheel", carEntity.getEntityTextureID());
            }
            GlStateManager.popMatrix();


            GlStateManager.pushMatrix();
            GlStateManager.translate(0, .6, 0);
            ModularVehicleInfo minfo = carEntity.getPackInfo();

            int index = VehicleEntityProperties.getPropertyIndex(0, VehicleEntityProperties.EnumVisualProperties.ROTATIONANGLE);
            float an = ((module.prevVisualProperties[index] + (module.visualProperties.get()[index] - module.prevVisualProperties[index]) * partialTicks)) % 360;
            float dif = an - carEntity.prevAngle;
            carEntity.trackProgress += Math.toRadians(dif) * minfo.getPartsByType(PartWheel.class).get(0).getDefaultWheelInfo().getWheelRadius();
            /* Rendering the wheels */
            CaterpillarInfo info = minfo.getSubPropertyByType(CaterpillarInfo.class);
            if (info.caterpillarLeftBuffer != null && info.caterpillarRightBuffer != null && carEntity.getPackInfo().getPartsByType(PartWheel.class).get(0).getDefaultWheelInfo().enableRendering()) {
                if (carEntity.trackProgress > info.caterpillarWidth) carEntity.trackProgress = 0;
                if (carEntity.trackProgress < -info.caterpillarWidth) carEntity.trackProgress = 0;

                Vector3f[] points = info.caterpillarLeftBuffer;
                float nbTrack = getPerimeter(points) / info.caterpillarWidth;
                float p = 0;
                for (int i = 0; i <= nbTrack; i++) {
                    GL11.glPushMatrix();
                    Vector4f a = getAdvancement(p + carEntity.trackProgress, info.caterpillarLeftBuffer);
                    GL11.glTranslated(a.x, a.y, a.z);
                    GL11.glRotated(-Math.toDegrees(a.w), 1, 0, 0);
                    DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getPartsByType(PartWheel.class).get(0).getDefaultWheelInfo().getModel()).renderModel(((IModuleContainer.IPropulsionContainer<WheelsModule>) carEntity).getPropulsion().getWheelsTextureId(0));
                    GL11.glPopMatrix();
                    p += info.caterpillarWidth;
                }
                p = 0;
                for (int i = 0; i <= nbTrack; i++) {
                    GL11.glPushMatrix();
                    Vector4f a = getAdvancement(p + carEntity.trackProgress, info.caterpillarRightBuffer);
                    GL11.glTranslated(a.x, a.y, a.z);
                    GL11.glRotated(-Math.toDegrees(a.w), 1, 0, 0);
                    DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getPartsByType(PartWheel.class).get(carEntity.getPackInfo().getPartsByType(PartWheel.class).size() - 1).getDefaultWheelInfo().getModel()).renderModel(((IModuleContainer.IPropulsionContainer<WheelsModule>) carEntity).getPropulsion().getWheelsTextureId(0));
                    GL11.glPopMatrix();
                    p += info.caterpillarWidth;
                }
            }
            GlStateManager.popMatrix();
            carEntity.prevAngle = an;

        }
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
    }


    Vector4f getAdvancement(float distance, Vector3f[] points) {
        Vector3f p1 = points[0];
        Vector3f p2 = points[0];
        float prevLength = 0;
        float length = 0;
        int i = 1;
        for (; i <= points.length; i++) {
            if (i == points.length) {
                p2 = points[0];
            } else {
                p2 = points[i];
            }
            prevLength = length;
            length += (p2.subtract(p1).length());
            if (distance <= length) break;
            p1 = p2;
        }
        float theta = (float) Math.atan2((p2.y - p1.y), (p2.z - p1.z));
        Vector3f r = p2.subtract(p1).normalize();

        float u = distance - prevLength;

        Vector3f h = r.clone().multLocal(u);
        r = p1.add(h);
        return new Vector4f(r.x, r.y, r.z, theta);
    }

    float getPerimeter(Vector3f[] points) {
        float distance = 0;
        Vector3f p1 = points[0];
        for (int i = 1; i <= points.length; i++) {
            Vector3f p2 = null;
            if (i == points.length) {
                p2 = points[0];
            } else {
                p2 = points[i];
            }
            distance += (p2.subtract(p1).length());
            p1 = p2;
        }
        return distance;
    }

    static void drawCircle(float x, float y, float z, float r, float g, float b, float radius) {

        int i;
        int triangleAmount = 100; //# of triangles used to draw circle
        //GLfloat radius = 0.8f; //radius
        float twicePi = (float) (2.0f * Math.PI);
        glBegin(GL_TRIANGLE_FAN);
        glColor3f(r, g, b);
        glVertex3f(x, z, y); // center of circle
        for (i = 0; i <= triangleAmount; i++) {
           /* glVertex2d(
                    x + (radius * cos(i *  (-Math.PI/3) / triangleAmount)),
                    y + (radius * sin(i * (-Math.PI/3) / triangleAmount))
            );*/
            glVertex3d(
                    x + (radius * cos(i * twicePi / triangleAmount)), z,
                    y + (radius * sin(i * twicePi / triangleAmount))
            );
        }
        glEnd();

    }

}
