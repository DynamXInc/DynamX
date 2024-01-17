package fr.dynamx.client.renders;

import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.common.MinecraftForge;

public class TESRDynamXBlock<T extends TEDynamXBlock> extends TileEntitySpecialRenderer<T> {
    protected final EntityRenderContext context = new EntityRenderContext(null);

    @Override
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.getPackInfo() == null) {
            return;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
        if (modelRenderer == null) {
            return;
        }
        EntityRenderContext context = this.context.setEntityParams(modelRenderer, (byte) te.getBlockMetadata());
        context.setRenderParams(x, y, z, partialTicks, false);
        if (!MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), te.getWorld(), te, this, context.getX(), context.getY(), context.getZ(),
                context.getPartialTicks(), destroyStage, alpha, EventStage.PRE))) {
            ((SceneGraph<T, BlockObject<?>>) te.getPackInfo().getSceneGraph()).render(te, context, te.getPackInfo());

            MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), te.getWorld(), te, this, context.getX(), context.getY(), context.getZ(),
                    context.getPartialTicks(), destroyStage, alpha, EventStage.POST));
        }
        if (shouldRenderDebug()) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            ((SceneGraph<T, BlockObject<?>>) te.getPackInfo().getSceneGraph()).renderDebug(te, context, te.getPackInfo());
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
        }
    }

    public boolean shouldRenderDebug() {
        return ClientDebugSystem.enableDebugDrawing && (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()
                || DynamXDebugOptions.SEATS_AND_STORAGE.isActive() || DynamXDebugOptions.PLAYER_COLLISIONS.isActive());
    }
}
