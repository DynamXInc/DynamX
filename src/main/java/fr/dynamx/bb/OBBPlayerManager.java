package fr.dynamx.bb;

import com.jme3.math.Vector3f;
import fr.dynamx.bb.bbloader.BlockBenchOBBInfoLoader;
import fr.dynamx.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.dynamx.bb.ModelPlayer.*;

public class OBBPlayerManager {
    public static HashMap<String, PlayerOBBModelObject> playerOBBObjectMap = new HashMap<>();
    public static EntityPlayer entityPlayer;
    public static ModelPlayer modelPlayer = new ModelPlayer();

    public static boolean debug = false;
    public static ArrayList<OBBDebugObject> lines = new ArrayList<>();
    private static final ResourceLocation debugBoxTex = new ResourceLocation("dynamxmod:obb/debugbox_red.png");

    public static class OBBDebugObject {
        public Vector3f start;
        public Vector3f end;
        public OBBModelBox box;
        public long aliveTime;
        public Vector3f pos;

        public void render() {
            if (aliveTime < System.currentTimeMillis()) {
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableTexture2D();
            if (box != null) {
                box.axis.forEach((axi) -> {
                    Tessellator tessellator = Tessellator.getInstance();
                    tessellator.getBuffer().begin(3, DefaultVertexFormats.POSITION_COLOR);
                    tessellator.getBuffer().pos(box.center.x, box.center.y, box.center.z).color(255, 0, 0, 255)
                            .endVertex();
                    tessellator.getBuffer().pos(box.center.x + axi.x, box.center.y + axi.y, box.center.z + axi.z)
                            .color(0, 255, 0, 255).endVertex();
                    tessellator.draw();
                });
                box.axisNormal.forEach((axi) -> {
                    Tessellator tessellator = Tessellator.getInstance();
                    tessellator.getBuffer().begin(3, DefaultVertexFormats.POSITION_COLOR);
                    tessellator.getBuffer().pos(box.center.x, box.center.y, box.center.z).color(0, 255, 0, 255)
                            .endVertex();
                    tessellator.getBuffer().pos(box.center.x + axi.x, box.center.y + axi.y, box.center.z + axi.z)
                            .color(0, 255, 0, 255).endVertex();
                    tessellator.draw();
                });
            } else if (pos != null) {
                GlStateManager.pushMatrix();
                GlStateManager.enableTexture2D();
                GlStateManager.translate(pos.x, pos.y, pos.z);
                Minecraft.getMinecraft().renderEngine.bindTexture(debugBoxTex);
                ClientProxy.obbModel.renderModel(true);
                GlStateManager.disableTexture2D();
                GlStateManager.popMatrix();
            } else {
                Tessellator tessellator = Tessellator.getInstance();
                tessellator.getBuffer().begin(3, DefaultVertexFormats.POSITION_COLOR);
                tessellator.getBuffer().pos(start.x, start.y, start.z).color(0, 0, 255, 255).endVertex();
                tessellator.getBuffer().pos(end.x, end.y, end.z).color(0, 0, 255, 255).endVertex();
                tessellator.draw();
            }
            GlStateManager.enableTexture2D();
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }
    }

    public static class PlayerOBBModelObject extends OBBModelObject {
        public long nextSyncTime = 0;
        public boolean isSyncing = false;

        public PlayerOBBModelObject() {
            boneUpdatePoseListeners.add((bone) -> {
                if (bone.name.equals("head")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, -5, 0);
                    }
                    bone.rotation.set(modelPlayer.bipedHead.rotateAngleX, modelPlayer.bipedHead.rotateAngleY,
                            modelPlayer.bipedHead.rotateAngleZ);
                }
                if (bone.name.equals("body")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, -5, 0);
                    }
                    bone.rotation.set(modelPlayer.bipedBody.rotateAngleX, modelPlayer.bipedBody.rotateAngleY,
                            modelPlayer.bipedBody.rotateAngleZ);
                }
                if (bone.name.equals("rightArm")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, -5, 0);
                    }
                    bone.rotation.set(modelPlayer.bipedRightArm.rotateAngleX, modelPlayer.bipedRightArm.rotateAngleY,
                            modelPlayer.bipedRightArm.rotateAngleZ);
                }
                if (bone.name.equals("leftArm")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, -5, 0);
                    }
                    bone.rotation.set(modelPlayer.bipedLeftArm.rotateAngleX, modelPlayer.bipedLeftArm.rotateAngleY,
                            modelPlayer.bipedLeftArm.rotateAngleZ);
                }
                if (bone.name.equals("rightLeg")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, 0, -4);
                    }
                    bone.rotation.set(modelPlayer.bipedRightLeg.rotateAngleX, modelPlayer.bipedRightLeg.rotateAngleY,
                            modelPlayer.bipedRightLeg.rotateAngleZ);
                }
                if (bone.name.equals("leftLeg")) {
                    bone.translation.set(0, 0, 0);
                    if (entityPlayer.isSneaking()) {
                        bone.translation.translate(0, 0, -4);
                    }
                    bone.rotation.set(modelPlayer.bipedLeftLeg.rotateAngleX, modelPlayer.bipedLeftLeg.rotateAngleY,
                            modelPlayer.bipedLeftLeg.rotateAngleZ);
                }
            });
        }

        public List<OBBModelBox> calculateIntercept(OBBModelBox testBox) {
            List<OBBModelBox> list = new ArrayList<>();
            for (OBBModelBox box : boxes) {
                if (OBBModelBox.testCollisionOBBAndOBB(box, testBox)) {
                    list.add(box.copy());
                }
            }
            return list;
        }
    }

    public static PlayerOBBModelObject getPlayerOBBObject(String name) {
        PlayerOBBModelObject playerOBBObject = playerOBBObjectMap.get(name);
        if (playerOBBObject == null) {
            playerOBBObject = BlockBenchOBBInfoLoader.loadOBBInfo(PlayerOBBModelObject.class,
                    new ResourceLocation("dynamxmod:obb/player.obb.json"));
            playerOBBObjectMap.put(name, playerOBBObject);
        }
        return playerOBBObject;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(PlayerTickEvent event) {
        /*if (event.phase != Phase.END) {
            return;
        }*/
        /*if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            return;
        }
        entityPlayer = event.player;
        PlayerOBBModelObject playerOBBObject = playerOBBObjectMap.get(event.player.getName());
        if (playerOBBObject == null) {
            playerOBBObject = BlockBenchOBBInfoLoader.loadOBBInfo(PlayerOBBModelObject.class,
                    new ResourceLocation("dynamxmod:obb/player.obb.json"));
            playerOBBObjectMap.put(event.player.getName(), playerOBBObject);
        }
        computePose(event, playerOBBObject, 1);*/
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerRender(RenderPlayerEvent.Post event) {
        float partialTick = event.getPartialRenderTick();
        entityPlayer = event.getEntityPlayer();
        PlayerOBBModelObject playerOBBObject = playerOBBObjectMap.get(event.getEntityPlayer().getName());
        if (playerOBBObject == null) {
            playerOBBObject = BlockBenchOBBInfoLoader.loadOBBInfo(PlayerOBBModelObject.class,
                    new ResourceLocation("dynamxmod:obb/player.obb.json"));
            playerOBBObjectMap.put(event.getEntityPlayer().getName(), playerOBBObject);
        }
        computePose(event, playerOBBObject, partialTick);
        if (Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()) {
            GlStateManager.pushMatrix();
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            GlStateManager.translate(
                    -(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTick),
                    -(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTick),
                    -(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTick));
            playerOBBObject.renderDebugBoxes();
            playerOBBObject.renderDebugAxis();
            GlStateManager.popMatrix();
        }
    }

    public static void computePose(Event event, PlayerOBBModelObject playerOBBObject, float partialTick) {
        if (!playerOBBObject.isSyncing && System.currentTimeMillis() >= playerOBBObject.nextSyncTime) {
            playerOBBObject.isSyncing = true;
            //playerOBBObject.nextSyncTime = System.currentTimeMillis() + 1000 / 60;
            modelPlayer.swingProgress = entityPlayer.swingProgress;
            modelPlayer.isSneak = entityPlayer.isSneaking();
            modelPlayer.isRiding = entityPlayer.isRiding();
            ArmPose mainPose = ArmPose.EMPTY;
            ArmPose offPose = ArmPose.EMPTY;
            ItemStack itemstack = entityPlayer.getHeldItemMainhand();
            ItemStack itemstack1 = entityPlayer.getHeldItemOffhand();
            if (!itemstack.isEmpty()) {
                mainPose = ArmPose.ITEM;

                if (entityPlayer.getItemInUseCount() > 0) {
                    EnumAction enumaction = itemstack.getItemUseAction();

                    if (enumaction == EnumAction.BLOCK) {
                        mainPose = ArmPose.BLOCK;
                    } else if (enumaction == EnumAction.BOW) {
                        mainPose = ArmPose.BOW_AND_ARROW;
                    }
                }
            }

            if (!itemstack1.isEmpty()) {
                offPose = ArmPose.ITEM;

                if (entityPlayer.getItemInUseCount() > 0) {
                    EnumAction enumaction1 = itemstack1.getItemUseAction();

                    if (enumaction1 == EnumAction.BLOCK) {
                        offPose = ArmPose.BLOCK;
                    } else if (enumaction1 == EnumAction.BOW) {
                        offPose = ArmPose.BOW_AND_ARROW;
                    }
                }
            }
            if (entityPlayer.getPrimaryHand() == EnumHandSide.RIGHT) {
                modelPlayer.rightArmPose = mainPose;
                modelPlayer.leftArmPose = offPose;
            } else {
                modelPlayer.rightArmPose = offPose;
                modelPlayer.leftArmPose = mainPose;
            }
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                if (event instanceof RenderPlayerEvent) {
                    modelPlayer.copyFrom(((RenderPlayerEvent) event).getRenderer().getMainModel());
                }else{
                    RenderPlayer renderplayer = (RenderPlayer) Minecraft.getMinecraft().getRenderManager().<AbstractClientPlayer>getEntityRenderObject(Minecraft.getMinecraft().player);
                    net.minecraft.client.model.ModelPlayer mainModel = renderplayer.getMainModel();
                    modelPlayer.copyFrom(mainModel);
                }
            } else {
                modelPlayer.setRotationAngles(entityPlayer.limbSwing, entityPlayer.prevLimbSwingAmount,
                        entityPlayer.ticksExisted, entityPlayer.rotationYaw - entityPlayer.renderYawOffset,
                        entityPlayer.rotationPitch, 1, entityPlayer);
            }
            playerOBBObject.updatePose();
            playerOBBObject.scene.resetMatrix();
            double lx = entityPlayer.lastTickPosX;
            double ly = entityPlayer.lastTickPosY;
            double lz = entityPlayer.lastTickPosZ;
            double x = entityPlayer.posX;
            double y = entityPlayer.posY;
            double z = entityPlayer.posZ;
            x = lx + (x - lx) * partialTick;
            y = ly + (y - ly) * partialTick;
            z = lz + (z - lz) * partialTick;
            playerOBBObject.scene.translate((float) x, (float) y, (float) z);
            float yaw = entityPlayer.renderYawOffset;
            if (entityPlayer.isRiding()) {
                yaw = entityPlayer.rotationYaw;
            }
            if (entityPlayer.isEntityAlive() && entityPlayer.isPlayerSleeping()) {
                playerOBBObject.scene.rotate(entityPlayer.getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
                playerOBBObject.scene.rotate(90, 0.0F, 0.0F, 1.0F);
                playerOBBObject.scene.rotate(270.0F, 0.0F, 1.0F, 0.0F);
            } else if (entityPlayer.isElytraFlying()) {
                playerOBBObject.scene.rotate(yaw / 180 * 3.14159f, 0, -1, 0);
                float f = (float) entityPlayer.getTicksElytraFlying() + partialTick;
                float f1 = MathHelper.clamp(f * f / 100.0F, 0.0F, 1.0F);
                playerOBBObject.scene.rotate(-f1 * (-90.0F - entityPlayer.rotationPitch) / 180 * 3.14159f, 1, 0, 0);
                Vec3d vec3d = entityPlayer.getLook(partialTick);
                double d0 = entityPlayer.motionX * entityPlayer.motionX + entityPlayer.motionZ * entityPlayer.motionZ;
                double d1 = vec3d.x * vec3d.x + vec3d.z * vec3d.z;

                if (d0 > 0.0D && d1 > 0.0D) {
                    double d2 = (entityPlayer.motionX * vec3d.x + entityPlayer.motionZ * vec3d.z)
                            / (Math.sqrt(d0) * Math.sqrt(d1));
                    double d3 = entityPlayer.motionX * vec3d.z - entityPlayer.motionZ * vec3d.x;
                    playerOBBObject.scene.rotate((float) (Math.signum(d3) * Math.acos(d2)), 0, 1, 0);
                }
            } else {
                boolean flag = false;
                if (!flag) {
                    playerOBBObject.scene.rotate(yaw / 180 * 3.14159f, 0, -1, 0);
                }
            }
            playerOBBObject.scene.scale(1 / 16f * 0.9375F, 1 / 16f * 0.9375F, 1 / 16f * 0.9375F);
            playerOBBObject.computePose();
            playerOBBObject.isSyncing = false;
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorld(RenderWorldLastEvent event) {
        if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            float partialTick = event.getPartialTicks();
            entityPlayer = Minecraft.getMinecraft().player;

            PlayerOBBModelObject playerOBBObject = playerOBBObjectMap.get(entityPlayer.getName());
            if (playerOBBObject == null) {
                playerOBBObject = BlockBenchOBBInfoLoader.loadOBBInfo(PlayerOBBModelObject.class,
                        new ResourceLocation("dynamxmod:obb/player.obb.json"));
                playerOBBObjectMap.put(entityPlayer.getName(), playerOBBObject);
            }
            computePose(event, playerOBBObject, partialTick);
            if (Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()) {
                GlStateManager.pushMatrix();
                Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
                GlStateManager.translate(
                        -(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTick),
                        -(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTick),
                        -(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTick));
                playerOBBObject.renderDebugBoxes();
                playerOBBObject.renderDebugAxis();
                GlStateManager.popMatrix();
            }
        }
        debug = false;
        if (Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()) {
            debug = true;
            GlStateManager.pushMatrix();
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            GlStateManager.translate(
                    -(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.getPartialTicks()),
                    -(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.getPartialTicks()),
                    -(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.getPartialTicks()));
            lines.forEach(OBBDebugObject::render);
            GlStateManager.popMatrix();
        }
    }
}
