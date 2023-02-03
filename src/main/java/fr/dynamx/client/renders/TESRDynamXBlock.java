package fr.dynamx.client.renders;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;

import java.util.ConcurrentModificationException;

import static fr.dynamx.utils.debug.renderer.VehicleDebugRenderer.PlayerCollisionsDebug.*;

public class TESRDynamXBlock<T extends TEDynamXBlock> extends TileEntitySpecialRenderer<T> {
    @Override
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.getBlockObjectInfo() != null && te.getBlockType() instanceof DynamXBlock) { //the instanceof fixes a crash
            Vector3fPool.openPool();
            if (!MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), getWorld(), te, this, x, y, z, partialTicks, destroyStage, alpha, EventStage.PRE))) {
                GlStateManager.pushMatrix();
                applyTransform(te, x, y, z);

                Vector3f pos = DynamXUtils.toVector3f(te.getPos())
                        .add(te.getBlockObjectInfo().getTranslation().add(te.getRelativeTranslation()))
                        .add(0.5f, 1.5f, 0.5f);

                Vector3f rot = te.getRelativeRotation()
                        .add(te.getBlockObjectInfo().getRotation())
                        .add(0, te.getRotation() * 22.5f, 0);

                //Rendering the model
                DynamXContext.getObjModelRegistry().getModel(te.getBlockObjectInfo().getModel()).renderModel((byte) te.getBlockMetadata());
                DynamXRenderUtils.spawnParticles(te.getBlockObjectInfo(), te.getWorld(), pos, rot);
                MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), getWorld(), te, this, x, y, z, partialTicks, destroyStage, alpha, EventStage.POST));
                GlStateManager.popMatrix();
            }

            if (shouldRenderDebug()) {
                renderDebug(te, x, y, z);
            }
            Vector3fPool.closePool();
        }
    }


    public void applyTransform(TEDynamXBlock te, double x, double y, double z) {
        // Translate to block render pos and add the config translate value
        GlStateManager.translate(
                x + 0.5D + te.getBlockObjectInfo().getTranslation().x + te.getRelativeTranslation().x,
                y + 1.5D + te.getBlockObjectInfo().getTranslation().y + te.getRelativeTranslation().y,
                z + 0.5D + te.getBlockObjectInfo().getTranslation().z + te.getRelativeTranslation().z);
        // Scale to the config scale value
        GlStateManager.scale(
                te.getBlockObjectInfo().getScaleModifier().x * (te.getRelativeScale().x != 0 ? te.getRelativeScale().x : 1),
                te.getBlockObjectInfo().getScaleModifier().y * (te.getRelativeScale().y != 0 ? te.getRelativeScale().y : 1),
                te.getBlockObjectInfo().getScaleModifier().z * (te.getRelativeScale().z != 0 ? te.getRelativeScale().z : 1));
        // Correct rotation of the block
        GlStateManager.rotate(te.getRotation() * 22.5f, 0.0F, -1.0F, 0.0F);
        float rotate = te.getRelativeRotation().x + te.getBlockObjectInfo().getRotation().x;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 1, 0, 0);
        rotate = te.getRelativeRotation().y + te.getBlockObjectInfo().getRotation().y;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 0, 1, 0);
        rotate = te.getRelativeRotation().z + te.getBlockObjectInfo().getRotation().z;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 0, 0, 1);
    }

    public boolean shouldRenderDebug() {
        return ClientDebugSystem.enableDebugDrawing && (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive() || DynamXDebugOptions.PLAYER_COLLISIONS.isActive());
    }

    public void renderDebug(TEDynamXBlock te, double x, double y, double z) {
        Vector3fPool.openPool();
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.translate(x, y, z);
        if (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()) {
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            Quaternion q = te.getCollisionInfo().getRotation();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.5D, 1.5D, 0.5D);
            GlStateManager.rotate(GlQuaternionPool.get(q));
            GlStateManager.translate(-0.5D, -1.5D, -0.5D);
            for (AxisAlignedBB partShape : te.getUnrotatedCollisionBoxes())
                RenderGlobal.drawSelectionBoundingBox(partShape, 0, 1, 1, 1);
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
        }
        if (DynamXDebugOptions.PLAYER_COLLISIONS.isActive()) {
            /* Start of Aymeric's collision debug */
            GlStateManager.translate(-te.getPos().getX() + 0.5D, -te.getPos().getY() + 1.5D, -te.getPos().getZ() + 0.5D);

            try {
                for (MutableBoundingBox bb : te.getCollisionInfo().getCollisionBoxes()) {

                    RenderGlobal.drawBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, 1, 1, 0, 1);
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }

            if (VehicleDebugRenderer.PlayerCollisionsDebug.lastTemp != null) {
                RenderGlobal.drawSelectionBoundingBox(VehicleDebugRenderer.PlayerCollisionsDebug.lastTemp, 1, 0, 1, 1);
            }
            //if(RotatedCollisionHandler.pos != null)
            //GlStateManager.translate(RotatedCollisionHandler.pos.x, RotatedCollisionHandler.pos.y, RotatedCollisionHandler.pos.z);
            if (motion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        motion.x * 10 + pos.x, motion.y * 10 + pos.y,
                        motion.z * 10 + pos.z, 1, 0, 0, 1);
            }
            if (rotatedmotion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        rotatedmotion.x * 10 + pos.x, rotatedmotion.y * 10 + pos.y,
                        rotatedmotion.z * 10 + pos.z, 0, 1, 0, 1);
            }
            if (realmotionrot != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        realmotionrot.x * 10 + pos.x, realmotionrot.y * 10 + pos.y,
                        realmotionrot.z * 10 + pos.z, 1, 0, 1, 1);
            }
            if (realmotion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        realmotion.x * 10 + pos.x, realmotion.y * 10 + pos.y,
                        realmotion.z * 10 + pos.z, 0, 0, 1, 1);
            }

                /*BoundingBoxPool.getINSTANCE().openSubPool();
                DynamXContext.getPlayerToCollision().forEach((player, playerPhysicsHandler) -> {
                    if (playerPhysicsHandler.getBodyIn() != null) {
                        BoundingBox bb = playerPhysicsHandler.getBodyIn().boundingBox(BoundingBoxPool.get());
                        Vector3f min = bb.getMin(Vector3fPool.get());
                        Vector3f max = bb.getMax(Vector3fPool.get());
                        RenderGlobal.drawBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z, 0.2f, 0.5f, 0.7f, 1);
                    }
                });
                BoundingBoxPool.getINSTANCE().closeSubPool();*/
            /*GlStateManager.rotate(-entity.rotationYaw, 0, 1, 0);
            GlStateManager.rotate(-entity.rotationPitch, 1, 0, 0);
            GlStateManager.rotate(entity.rotationRoll, 0, 0, 1);*/
            /* End of Aymeric's collision debug*/
        }
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        Vector3fPool.closePool();
    }
}
