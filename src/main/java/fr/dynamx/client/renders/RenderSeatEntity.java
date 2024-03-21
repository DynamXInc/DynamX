package fr.dynamx.client.renders;

import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class RenderSeatEntity extends Render<SeatEntity> {
    public RenderSeatEntity(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(SeatEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!ClientDebugSystem.enableDebugDrawing || !DynamXDebugOptions.SEATS_AND_STORAGE.isActive())
            return;
        GlStateManager.pushMatrix();
        {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            GlStateManager.translate((float) x, (float) y, (float) z);
            TEDynamXBlock block = (TEDynamXBlock) entity.world.getTileEntity(entity.getPosition());
            if (block == null) {
                RenderGlobal.drawBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5,
                        1, 0, 0, 1);
            } else {
                Vector3fPool.openPool();
                QuaternionPool.openPool();
                MutableBoundingBox box = new MutableBoundingBox();
                for (PartBlockSeat seat : (List<PartBlockSeat>) block.getPackInfo().getPartsByType(PartBlockSeat.class)) {
                    seat.getBox(box);
                    box = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), box, block.getCollidableRotation());
                    Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), block.getCollidableRotation());
                    partPos.addLocal(block.getRelativeTranslation());
                    box.offset(partPos);
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 1, 0, 1);
                }
                QuaternionPool.closePool();
                Vector3fPool.closePool();
            }
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
        }
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(SeatEntity entity) {
        return null;
    }
}
