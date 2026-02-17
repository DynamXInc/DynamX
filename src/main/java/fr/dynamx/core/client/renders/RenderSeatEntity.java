package fr.dynamx.core.client.renders;

import com.jme3.math.Vector3f;
import fr.dynamx.core.client.handlers.ClientDebugSystem;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.core.common.entities.SeatEntity;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
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
                JmeVector3fPool.openPool();
                QuaternionPool.openPool();
                MutableBoundingBox box = new MutableBoundingBox();
                for (PartBlockSeat seat : (List<PartBlockSeat>) block.getPackInfo().getPartsByType(PartBlockSeat.class)) {
                    seat.getBox(box);
                    box = DynamXContext.getCollisionHandler().rotateBB(JmeVector3fPool.get(), box, block.getCollidableRotation());
                    Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), block.getCollidableRotation());
                    partPos.addLocal(block.getRelativeTranslation());
                    box.offset(partPos);
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 1, 0, 1);
                }
                QuaternionPool.closePool();
                JmeVector3fPool.closePool();
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
