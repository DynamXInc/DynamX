package fr.dynamx.client.handlers;

import com.jme3.bullet.objects.PhysicsRigidBody;
import fr.dynamx.api.contentpack.object.IShapeProvider;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.*;
import fr.dynamx.utils.debug.renderer.PhysicsDebugRenderer;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

import java.util.*;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class ClientDebugSystem {
    private static final List<ProfilingData.Measure> physicsTicks = new ArrayList<>();
    public static boolean enableDebugDrawing;
    public static int MOVE_DEBUG;

    private static final Map<Long, RigidBodyTransform>[] prevRigidBodyStates = new Map[]{new HashMap(), new HashMap()};
    private static byte curRigidBodyStatesIndex;

    private static final Minecraft MC = Minecraft.getMinecraft();

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void tickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            enableDebugDrawing = DynamXDebugOptions.DEBUG_RENDER.isActive();
            if (Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.ticksExisted % 5 == 0) {
                int request = 0;
                for (DynamXDebugOptions.DebugCategories categories : DynamXDebugOptions.DebugCategories.values()) {
                    for (DynamXDebugOption o : categories.getOptions()) {
                        if (o.serverRequestMask() != 0 && o.isActive()) {
                            request = request | o.serverRequestMask();
                            break;
                        }
                    }
                }
                if (request != 0) {
                    DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(request));
                }
            }

            if (enableDebugDrawing && DynamXDebugOptions.PHYSICS_DEBUG.isActive() && DynamXContext.getPhysicsWorld() != null) {
                QuaternionPool.openPool();
                Vector3fPool.openPool();
                curRigidBodyStatesIndex++;
                if (curRigidBodyStatesIndex > 1) {
                    curRigidBodyStatesIndex = 0;
                }
                prevRigidBodyStates[curRigidBodyStatesIndex].clear();
                for (PhysicsRigidBody body : DynamXContext.getPhysicsWorld().getDynamicsWorld().getRigidBodyList()) {
                    prevRigidBodyStates[curRigidBodyStatesIndex].put(body.nativeId(), new RigidBodyTransform(body));
                }
                Vector3fPool.closePool();
                QuaternionPool.closePool();
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void overlay(RenderGameOverlayEvent.Post event) {
        if (event.getType().equals(RenderGameOverlayEvent.ElementType.TEXT) && enableDebugDrawing) {
            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            String s = "Drawing debug";
            fontRenderer.drawString(s, event.getResolution().getScaledWidth() - fontRenderer.getStringWidth(s) - 2, 2, 0xFFBC00);
            if (DynamXContext.getPhysicsWorld() != null) {
                s = "Entities: " + DynamXContext.getPhysicsWorld().getLoadedEntityCount();
            } else
                s = "Not simulating...";
            fontRenderer.drawString(s, event.getResolution().getScaledWidth() - fontRenderer.getStringWidth(s) - 2, 12, 0xFFBC00);
            //fontRenderer.drawString("Physics time: " + BasePhysicsWorld.TIME +" ms", event.getResolution().getScaledWidth() - fontRenderer.getStringWidth(s) - 40, 22, 0xFFBC00);

            if (DynamXDebugOptions.PROFILING.isActive()) {
                if (Minecraft.getMinecraft().player.ticksExisted % 3 == 0) {
                    ProfilingData d = Profiler.get().getData(Profiler.Profiles.BULLET_STEP_SIM);
                    if (d != null)
                        physicsTicks.add(d.save());
                }
                if (!physicsTicks.isEmpty()) {
                    if (physicsTicks.size() > 105)
                        physicsTicks.remove(0);
                    int x = 2;
                    int c = 0;
                    for (ProfilingData.Measure d1 : physicsTicks) {
                        d1.draw(x, event.getResolution().getScaledHeight() - 20, fontRenderer, c);
                        x += 12;
                        c++;
                    }
                }
            } else if (!physicsTicks.isEmpty())
                physicsTicks.clear();
        }
    }

    static BasePart<?> lastPart = null;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void worldRender(RenderWorldLastEvent event) {
        if (enableDebugDrawing) {
            GlStateManager.pushMatrix();

            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();

            EntityPlayer rootPlayer = Minecraft.getMinecraft().player;
            double x = rootPlayer.lastTickPosX + (rootPlayer.posX - rootPlayer.lastTickPosX) * event.getPartialTicks();
            double y = rootPlayer.lastTickPosY + (rootPlayer.posY - rootPlayer.lastTickPosY) * event.getPartialTicks();
            double z = rootPlayer.lastTickPosZ + (rootPlayer.posZ - rootPlayer.lastTickPosZ) * event.getPartialTicks();
            GlStateManager.translate(-x, -y, -z);

            drawDebug(DynamXDebugOptions.BLOCK_BOXES);
            drawDebug(DynamXDebugOptions.CLIENT_BLOCK_BOXES);
            drawDebug(DynamXDebugOptions.SLOPE_BOXES);
            drawDebug(DynamXDebugOptions.CLIENT_SLOPE_BOXES);
            //Draw chunks after slopes, else slope are not visible because we do not cull
            drawDebug(DynamXDebugOptions.CHUNK_BOXES);
            drawDebug(DynamXDebugOptions.CLIENT_CHUNK_BOXES);

            if (DynamXDebugOptions.PHYSICS_DEBUG.isActive()) {
                Vector3fPool.openPool();
                QuaternionPool.openPool();
                GlQuaternionPool.openPool();

                byte prevRigidBodyStatesIndex = (byte) (curRigidBodyStatesIndex - 1);
                if (prevRigidBodyStatesIndex < 0)
                    prevRigidBodyStatesIndex = 1;
                for (PhysicsRigidBody body : DynamXContext.getPhysicsWorld().getDynamicsWorld().getRigidBodyList()) {
                    PhysicsDebugRenderer.debugRigidBody(body, prevRigidBodyStates[prevRigidBodyStatesIndex].get(body.nativeId()), prevRigidBodyStates[curRigidBodyStatesIndex].get(body.nativeId()), event.getPartialTicks());
                }
                DynamXContext.getPhysicsWorld().getDynamicsWorld().getSoftBodyList().forEach(PhysicsDebugRenderer::debugSoftBody);
                Vector3fPool.closePool();
                QuaternionPool.closePool();

                GlStateManager.disableDepth();
                Vector3fPool.openPool();
                QuaternionPool.openPool();
                DynamXContext.getPhysicsWorld().getDynamicsWorld().getJointList().forEach(PhysicsDebugRenderer::debugConstraint);
                GlQuaternionPool.closePool();
                Vector3fPool.closePool();
                QuaternionPool.closePool();
                GlStateManager.enableDepth();

            }


            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();

            if (DynamXDebugOptions.CAMERA_RAYCAST.isActive()) {
                CameraSystem.drawDebug();
            }

            Vector3fPool.openPool();
            if (MC.objectMouseOver != null) {
                if (!rootPlayer.isSneaking()) {
                    disableShapeDebug(lastPart);
                    Vector3fPool.closePool();
                    return;
                }
                if (!(MC.objectMouseOver.entityHit instanceof PackPhysicsEntity)) {
                    disableShapeDebug(lastPart);
                    Vector3fPool.closePool();
                    return;
                }
                PackPhysicsEntity<?, ?> entityHit = (PackPhysicsEntity<?, ?>) MC.objectMouseOver.entityHit;
                if (!(entityHit.getPackInfo() instanceof IShapeProvider)) {
                    disableShapeDebug(lastPart);
                    Vector3fPool.closePool();
                    return;
                }
                Predicate<BasePart<?>> wantedShape = null;
                Optional<DynamXDebugOption> dynamXDebugOptions = DynamXDebugOptions.getAllOptions()
                        .stream()
                        .filter(dynamXDebugOption -> !dynamXDebugOption.equals(DynamXDebugOptions.DEBUG_RENDER)
                                && dynamXDebugOption.isActive()).findFirst();
                if (dynamXDebugOptions.isPresent()) {
                    wantedShape = basePart -> {
                        if(basePart.getDebugOption() != null) {
                            return basePart.getDebugOption().equals(dynamXDebugOptions.get());
                        }
                        return false;
                    };
                }
                BasePart<?> basePart = DynamXUtils.rayTestPart(rootPlayer, entityHit, (IShapeProvider<?>) entityHit.getPackInfo(), wantedShape);
                if (basePart == null) {
                    disableShapeDebug(lastPart);
                    Vector3fPool.closePool();
                    return;
                }
                GlStateManager.pushMatrix();
                GlQuaternionPool.openPool();
                QuaternionPool.openPool();
                Quaternion rot = ClientDynamXUtils.computeInterpolatedGlQuaternion(
                        entityHit.prevRenderRotation,
                        entityHit.renderRotation,
                        event.getPartialTicks(), false);
                double entityX = entityHit.lastTickPosX + (entityHit.posX - entityHit.lastTickPosX) * event.getPartialTicks();
                double entityY = entityHit.lastTickPosY + (entityHit.posY - entityHit.lastTickPosY) * event.getPartialTicks();
                double entityZ = entityHit.lastTickPosZ + (entityHit.posZ - entityHit.lastTickPosZ) * event.getPartialTicks();
                GlStateManager.translate(-x, -y, -z);
                GlStateManager.translate(entityX, entityY, entityZ);
                GlStateManager.rotate(rot);
                float yOffset = 0;
                if (basePart instanceof PartSeat)
                    yOffset = 0.9f;
                DynamXRenderUtils.drawNameplate(MC.fontRenderer, basePart.getPartName(),
                        basePart.getPosition().x,
                        basePart.getPosition().y + yOffset + 1,
                        basePart.getPosition().z,
                        ClientDynamXUtils.computeInterpolatedGlQuaternion(
                                entityHit.prevRenderRotation,
                                entityHit.renderRotation,
                                event.getPartialTicks(), true),
                        0, rootPlayer.rotationYaw, rootPlayer.rotationPitch, false);
                if (lastPart != null && lastPart.getDebugOption() != null)
                    lastPart.getDebugOption().disable();
                if (basePart.getDebugOption() != null)
                    basePart.getDebugOption().enable();
                if (wantedShape == null)
                    lastPart = basePart;
                GlQuaternionPool.closePool();
                QuaternionPool.closePool();
                GlStateManager.popMatrix();
            } else {
                //disableShapeDebug(lastPart);
            }
            Vector3fPool.closePool();

        }
    }

    private static void disableShapeDebug(BasePart<?> basePart) {
        if (basePart == null || basePart.getDebugOption() == null)
            return;
        basePart.getDebugOption().disable();
    }

    @SideOnly(Side.CLIENT)
    private static void drawDebug(DynamXDebugOption.TerrainDebugOption option) {
        //  if(option == DynamXDebugOptions.SLOPE_BOXES)
        //    System.out.println(option.isActive(terrainDebugMode)+" "+option.dataIn);
        if (!option.getDataIn().isEmpty() && option.isActive()) {
            GlStateManager.pushMatrix();
            try {
                for (Map.Entry<Integer, TerrainDebugData> pos : option.getDataIn().entrySet()) {
                    switch (pos.getValue().getRenderer()) {
                        case BLOCKS:
                        case DYNAMXBLOCKS:
                            drawAABBDebug(pos.getValue());
                            break;
                        case STAIRS:
                            drawSlopeDebug(pos.getValue().getData(), pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB(), 0.2f);
                            break;
                        case CUSTOM_SLOPE:
                            if (pos.getValue().getRenderer() == TerrainDebugRenderer.CUSTOM_SLOPE) {
                                float margin = 0.02f;
                                float[] p = pos.getValue().getData();
                                drawAABBDebug(new float[]{p[p.length - 3] - margin, p[p.length - 2] - margin, p[p.length - 1] - margin, p[p.length - 3] + margin, p[p.length - 2] + margin, p[p.length - 1] + margin,
                                        pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB()});
                            }
                            //no break here
                        case SLOPES:
                            drawSlopeDebug(pos.getValue().getData(), pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB(), 0.5f);
                            break;
                    }
                }
            } catch (ConcurrentModificationException ignored) {
            } //Server is modifying something
            GlStateManager.popMatrix();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void drawSlopeDebug(float[] pos, float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
        GlStateManager.disableCull();
        //System.out.println("Draw slope "+ ArrayUtils.toString(pos));
        if (pos.length == 16) { //Custom slopes handling
            GlStateManager.glBegin(GL11.GL_QUADS);
            for (int i = 0; i < 4; i++) {
                GlStateManager.glVertex3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
            }
            GlStateManager.glEnd();
        } else {
            GlStateManager.glBegin(GL11.GL_TRIANGLES);
            for (int i = 0; i < pos.length / 3; i++) {
                GlStateManager.glVertex3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
            }
            GlStateManager.glEnd();
            GlStateManager.color(1, 0, 0, 1);
            GlStateManager.glBegin(GL11.GL_LINES);
            for (int i = 0; i < pos.length / 3 - 1; i++) {
                GlStateManager.glVertex3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
                GlStateManager.glVertex3f(pos[i * 3 + 3], pos[i * 3 + 4], pos[i * 3 + 5]);
            }
        }
        GlStateManager.glEnd();
        GlStateManager.enableCull();
    }

    @SideOnly(Side.CLIENT)
    public static void drawAABBDebug(TerrainDebugData debugData) {
        float[] pos = debugData.getData();
        GlStateManager.color(debugData.getRenderer().getR(), debugData.getRenderer().getG(), debugData.getRenderer().getB(), 0.15f);
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.glBegin(GL11.GL_QUADS);
        fillFaceBox(pos[0], pos[1] - 0.03f, pos[2], pos[3], pos[4] + 0.03f, pos[5]);
        GlStateManager.glEnd();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableCull();
        GlStateManager.disableDepth();
        bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.glLineWidth(4);
        drawFaceBoxBorders(bufferbuilder, pos[0], pos[1], pos[2], pos[3], pos[4], pos[5], debugData.getRenderer().getR(), debugData.getRenderer().getG(), debugData.getRenderer().getB(), 1);
        tessellator.draw();
        //GlStateManager.enableCull();
    }

    @SideOnly(Side.CLIENT)
    public static void drawAABBDebug(float[] pos) {
        if (pos.length != 9)
            throw new IllegalStateException("Pos must have 9 floats !");
        GlStateManager.color(pos[6], pos[7], pos[8], 0.15f);
        GlStateManager.disableCull();
        GlStateManager.glBegin(GL11.GL_QUADS);
        fillFaceBox(pos[0], pos[1] - 0.03f, pos[2], pos[3], pos[4] + 0.03f, pos[5]);
        GlStateManager.glEnd();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.glLineWidth(4);
        drawFaceBoxBorders(bufferbuilder, pos[0], pos[1], pos[2], pos[3], pos[4], pos[5], pos[6], pos[7], pos[8], 1);
        tessellator.draw();
        GlStateManager.enableCull();
    }

    @SideOnly(Side.CLIENT)
    public static void fillFaceBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        GlStateManager.glVertex3f(minX, minY, minZ);
        GlStateManager.glVertex3f(maxX, minY, minZ);
        GlStateManager.glVertex3f(maxX, minY, maxZ);
        GlStateManager.glVertex3f(minX, minY, maxZ);

        GlStateManager.glVertex3f(minX, maxY, minZ);
        GlStateManager.glVertex3f(maxX, maxY, minZ);
        GlStateManager.glVertex3f(maxX, maxY, maxZ);
        GlStateManager.glVertex3f(minX, maxY, maxZ);

        GlStateManager.glVertex3f(minX, minY, minZ);
        GlStateManager.glVertex3f(minX, maxY, minZ);
        GlStateManager.glVertex3f(minX, maxY, maxZ);
        GlStateManager.glVertex3f(minX, minY, maxZ);

        GlStateManager.glVertex3f(maxX, minY, minZ);
        GlStateManager.glVertex3f(maxX, maxY, minZ);
        GlStateManager.glVertex3f(maxX, maxY, maxZ);
        GlStateManager.glVertex3f(maxX, minY, maxZ);

        GlStateManager.glVertex3f(minX, minY, minZ);
        GlStateManager.glVertex3f(maxX, minY, minZ);
        GlStateManager.glVertex3f(maxX, maxY, minZ);
        GlStateManager.glVertex3f(minX, maxY, minZ);

        GlStateManager.glVertex3f(minX, minY, maxZ);
        GlStateManager.glVertex3f(maxX, minY, maxZ);
        GlStateManager.glVertex3f(maxX, maxY, maxZ);
        GlStateManager.glVertex3f(minX, maxY, maxZ);
    }

    @SideOnly(Side.CLIENT)
    public static void drawFaceBoxBorders(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha) {
        buffer.pos(minX, minY, minZ).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        if (maxX != minX) {
            buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            if (maxZ != minZ)
                buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        }
        if (maxZ != minZ)
            buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        if (maxY != minY) {
            buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            if (maxX != minX)
                buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            if (maxZ != minZ) {
                if (maxX != minX)
                    buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
                buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            }
            buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            if (maxZ != minZ) {
                buffer.pos(minX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex();
                buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
                if (maxX != minX) {
                    buffer.pos(maxX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex();
                    buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
                }
            }
            if (maxX != minX) {
                buffer.pos(maxX, maxY, minZ).color(red, green, blue, 0.0F).endVertex();
                buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
                buffer.pos(maxX, minY, minZ).color(red, green, blue, 0.0F).endVertex();
            }
        }
    }
}
