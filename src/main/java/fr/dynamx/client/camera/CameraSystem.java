package fr.dynamx.client.camera;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles camera rotation and zoom while in a vehicle
 */
public class CameraSystem {
    private static CameraMode rotationMode = CameraMode.AUTO;

    private static int zoomLevel = 4;
    private static float cameraPositionY;
    private static boolean watchingBehind = false;
    private static final Quaternion glQuatCache = new Quaternion();
    private static final com.jme3.math.Quaternion jmeQuatCache = new com.jme3.math.Quaternion();
    private static com.jme3.math.Quaternion lastCameraQuat;

    /**
     * Computes a smooth interpolated camera rotation
     */
    private static void animateCameraRotation(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, float animLength) {
        DynamXMath.slerp(step, prevRotation, rotation, jmeQuatCache);
        DynamXGeometry.inverseQuaternion(jmeQuatCache, jmeQuatCache);
        rotationMode.rotator.apply(Minecraft.getMinecraft().gameSettings.thirdPersonView, jmeQuatCache);

        jmeQuatCache.normalizeLocal();

        if (lastCameraQuat == null)
            lastCameraQuat = new com.jme3.math.Quaternion(jmeQuatCache.getX(), jmeQuatCache.getY(), jmeQuatCache.getZ(), jmeQuatCache.getW());
        //else //FIXME FIX THIS :c
        //This causes camera stuttering since the input lag fix
        //  DynamXGeometry.slerp(lastCameraQuat, jmeQuatCache, lastCameraQuat, animLength);
        lastCameraQuat.set(jmeQuatCache);
        //jmeQuatCache.set(lastCameraQuat);
        lastCameraQuat.normalizeLocal();
        glQuatCache.set(lastCameraQuat.getX(), lastCameraQuat.getY(), lastCameraQuat.getZ(), lastCameraQuat.getW());
    }

    /**
     * Adjusts roll and pitch for camera.
     * Only works when camera is inside vehicles.
     */
    public static void rotateVehicleCamera(EntityViewRenderEvent.CameraSetup event) {
        Vector3fPool.openPool();
        BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) event.getEntity().getRidingEntity();
        Entity renderEntity = event.getEntity();

        //Compute smoothed vehicle rotation, on axes according to camera mode
        animateCameraRotation(vehicle.prevRenderRotation, vehicle.renderRotation, (float) event.getRenderPartialTicks(), 0.1f);

        //Apply camera zoom
        if (ClientEventHandler.MC.gameSettings.thirdPersonView > 0) {
            performZoomAction(renderEntity, event.getYaw(), event.getPitch(), event.getRenderPartialTicks(), jmeQuatCache);
        }

        //Rotate the camera
        GlStateManager.rotate(event.getRoll(), 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(event.getPitch(), 1.0F, 0.0F, 0.0F);
        if (vehicle instanceof IModuleContainer.ISeatsContainer) {
            PartSeat seat = ((IModuleContainer.ISeatsContainer) vehicle).getSeats().getRidingSeat(renderEntity);

            if(seat == null) {
                return;
            }

            if(ClientEventHandler.MC.gameSettings.thirdPersonView > 0 && seat.getCameraPositionY() != 0) {
                cameraPositionY = seat.getCameraPositionY();
                GlStateManager.translate(0, -cameraPositionY, 0);
            }

            if (seat.getRotation() != null) {
                GlStateManager.rotate(event.getYaw() + (watchingBehind ? 180 : 0) + seat.getRotationYaw(), 0.0F, 1.0F, 0.0F);
            } else {
                GlStateManager.rotate(event.getYaw() + (watchingBehind ? 180 : 0), 0.0F, 1.0F, 0.0F);
            }
        } else {
            GlStateManager.rotate(event.getYaw() + (watchingBehind ? 180 : 0), 0.0F, 1.0F, 0.0F);
        }

        //Remove the eye translation
        GlStateManager.translate(0, -renderEntity.getEyeHeight(), 0);

        //Apply the vehicle's rotation
        GlStateManager.rotate(glQuatCache);

        //Restore the eye translation (because it's removed in the vanilla code after this event)
        GlStateManager.translate(0, renderEntity.getEyeHeight(), 0);

        //Cancel any vanilla rotation
        event.setPitch(0);
        event.setRoll(0);
        event.setYaw(0);
        Vector3fPool.closePool();
    }

    private static final Vector3f pt0 = new Vector3f();
    private static final Vector3f pt1 = new Vector3f();
    private static final Vector3f pt2 = new Vector3f();
    private static final Vector3f pt3 = new Vector3f();

    private static final Map<Vector3f, Vector3f> cameraRadius = new HashMap<>();

    public static void drawDebug() {
        Vector3fPool.openPool();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();

        Vector3f eye = Vector3fPool.get(0, Minecraft.getMinecraft().player.getEyeHeight(), 0);
        eye = DynamXGeometry.rotateVectorByQuaternion(eye, jmeQuatCache.inverse());
        GlStateManager.translate(-Minecraft.getMinecraft().getRenderViewEntity().posX + eye.x, -Minecraft.getMinecraft().getRenderViewEntity().posY + eye.y, -Minecraft.getMinecraft().getRenderViewEntity().posZ + eye.z);

        RenderGlobal.drawBoundingBox(pt0.x - 0.1f, pt0.y - 0.1f, pt0.z - 0.1f, pt0.x + 0.1f, pt0.y + 0.1f, pt0.z + 0.1f, 1, 1, 1, 1);

        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.glBegin(GL11.GL_LINES);
        GlStateManager.glVertex3f(CameraSystem.pt0.x, CameraSystem.pt0.y, CameraSystem.pt0.z);
        GlStateManager.glVertex3f(CameraSystem.pt3.x, CameraSystem.pt3.y, CameraSystem.pt3.z);
        GlStateManager.glEnd();

        GlStateManager.color(0, 1, 0, 1);
        cameraRadius.forEach((pt1, pt2) -> {
            GlStateManager.glBegin(GL11.GL_LINES);
            GlStateManager.glVertex3f(pt1.x, pt1.y, pt1.z);
            GlStateManager.glVertex3f(pt2.x, pt2.y, pt2.z);
            GlStateManager.glEnd();
        });

        GlStateManager.color(1, 1, 0, 1);
        GlStateManager.glBegin(GL11.GL_LINES);
        GlStateManager.glVertex3f(CameraSystem.pt0.x, CameraSystem.pt0.y, CameraSystem.pt0.z);
        GlStateManager.glVertex3f(CameraSystem.pt1.x, CameraSystem.pt1.y, CameraSystem.pt1.z);
        GlStateManager.glEnd();

        GlStateManager.color(0, 0, 1, 1);
        GlStateManager.glBegin(GL11.GL_LINES);
        GlStateManager.glVertex3f(CameraSystem.pt0.x, CameraSystem.pt0.y, CameraSystem.pt0.z);
        GlStateManager.glVertex3f(CameraSystem.pt2.x, CameraSystem.pt2.y, CameraSystem.pt2.z);
        GlStateManager.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();

        Vector3fPool.closePool();
    }

    /**
     * Zooms using the by the zoomLevel amount, raycasting around the camera to avoid having the camera inside of a bloc
     *
     * @param yaw       pitch : the entity's yaw and pitch
     * @param vRotation : the vehicle's rotation
     */
    private static void performZoomAction(Entity entity, float yaw, float pitch, double partialTicks, com.jme3.math.Quaternion vRotation) {
        boolean debug = ClientDebugSystem.enableDebugDrawing && DynamXDebugOptions.CAMERA_RAYCAST.isActive() && Keyboard.isKeyDown(Keyboard.KEY_B);
        if (debug)
            cameraRadius.clear();
        float f = entity.getEyeHeight();
        float d3 = zoomLevel + f;
        //Camera pos
        double d0 = (entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks);
        double d1 = (entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks);
        double d2 = (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks);
        if (debug)
            pt0.set((float) d0, (float) d1, (float) d2);
        Vector3f eye = Vector3fPool.get(0, f + (ClientEventHandler.MC.gameSettings.thirdPersonView > 0 ? cameraPositionY : 0), 0);
        eye = DynamXGeometry.rotateVectorByQuaternion(eye, vRotation.inverse());
        d0 += eye.x;
        d1 += eye.y;
        d2 += eye.z;

        if (ClientEventHandler.MC.gameSettings.thirdPersonView == 2) {
            pitch += 180.0F;
        }
        if (watchingBehind)
            yaw += 180f;

        //Thanks to Mojang for this code
        float d4 = (-MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F)) * d3;
        float d5 = (MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F)) * d3;
        float d6 = (-MathHelper.sin(pitch * 0.017453292F)) * d3;

        //Raycast
        for (int i = 0; i < 8; ++i) {
            float f3 = (float) ((i & 1) * 2 - 1);
            float f4 = (float) ((i >> 1 & 1) * 2 - 1);
            float f5 = (float) ((i >> 2 & 1) * 2 - 1);
            f3 = f3 * 0.1F;
            f4 = f4 * 0.1F;
            f5 = f5 * 0.1F;

            //Apply camera rotation offset of the vehicle
            Vector3f start = DynamXGeometry.getRotatedPoint(Vector3fPool.get(f3, f4, f5), 0, 180, 0);//PhysicsHelper.getRotatedPoint(Vector3fPool.get(f3, f4, f5), vPitch, MathHelper.wrapDegrees(vYaw+180), vRoll);
            start = DynamXGeometry.rotateVectorByQuaternion(start, vRotation.inverse());

            Vector3f end = DynamXGeometry.getRotatedPoint(Vector3fPool.get(-d4 + f3, -d6 + f4, -d5 + f5), 0, 180, 0);//PhysicsHelper.getRotatedPoint(Vector3fPool.get(- d4 + f3 + f5, - d6 + f4, - d5 + f5), vPitch, MathHelper.wrapDegrees(vYaw+180), vRoll);
            end = DynamXGeometry.rotateVectorByQuaternion(end, vRotation.inverse());
            if (debug) {
                pt1.set(start);
                pt1.addLocal(pt0);
                pt2.set(end);
                pt2.addLocal(pt0);
                cameraRadius.put(Vector3fPool.get(-38, "came", pt1), Vector3fPool.get(-38, "came", pt2));
            }

            RayTraceResult raytraceresult = ClientEventHandler.MC.world.rayTraceBlocks(new Vec3d(d0 + start.x, d1 + start.y, d2 + start.z), new Vec3d(d0 + end.x, d1 + end.y, d2 + end.z), false, true, false);

            if (raytraceresult != null && raytraceresult.entityHit != entity.getRidingEntity()) {
                float d7 = (float) raytraceresult.hitVec.distanceTo(new Vec3d(d0, d1, d2));
                if (d7 < d3) {
                    d3 = d7;
                    if (debug)
                        pt3.set((float) raytraceresult.hitVec.x, (float) raytraceresult.hitVec.y, (float) raytraceresult.hitVec.z);
                }
            }
        }

        //f=0;
        //Apply final zoom, works very well with the "+3*f", I don't know why
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            //if(-d3+f*3<0) //if we can de-zoom (we don't want to co inside the camera corpse)
            GL11.glTranslated(0, 0F, -d3 + f * 3);
        } else if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            //if(d3-f*3>0) //if we can de-zoom (we don't want to co inside the camera corpse)
            GL11.glTranslated(0, 0F, d3 - f * 3);
        }
    }

    public static void changeCameraZoom(boolean zoomOut) {
        //System.out.println("Current zoom is "+zoomLevel);
        if (zoomOut) {
            if (zoomLevel < DynamXConfig.maxZoomOut)
                zoomLevel += 2;
        } else {
            if (zoomLevel > 4) {
                zoomLevel -= 2;
            }
        }
    }

    public static void setCameraZoom(int zoomLevel) {
        CameraSystem.zoomLevel = zoomLevel;
    }

    public static CameraMode cycleCameraMode() {
        switch (rotationMode) {
            case AUTO:
                rotationMode = CameraMode.FIXED;
                break;
            case FIXED:
                rotationMode = CameraMode.FREE;
                break;
            case FREE:
                rotationMode = CameraMode.AUTO;
                break;
        }
        return rotationMode;
    }

    public static void setWatchingBehind(boolean watchingBehind) {
        CameraSystem.watchingBehind = watchingBehind;
    }
}
