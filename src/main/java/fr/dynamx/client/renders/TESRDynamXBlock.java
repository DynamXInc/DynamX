package fr.dynamx.client.renders;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.common.MinecraftForge;

public class TESRDynamXBlock<T extends TEDynamXBlock> extends TileEntitySpecialRenderer<T> {
    protected final BaseRenderContext.BlockRenderContext context = new BaseRenderContext.BlockRenderContext();

    @Override
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        BlockObject<?> packInfo = te.getPackInfo();
        if (packInfo == null) {
            return;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (modelRenderer == null) {
            return;
        }
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        BaseRenderContext.BlockRenderContext context = this.context.setModelParams(te, modelRenderer, (byte) te.getBlockMetadata());
        context.setRenderParams(x, y, z, partialTicks, false);
        SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode = (SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>>) packInfo.getSceneGraph();
        if (!MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), te.getWorld(), te, this, context.getRenderPosition(),
                context.getPartialTicks(), destroyStage, alpha, EventStage.PRE))) {
            sceneNode.render(context, packInfo);

            Vector3f pos = DynamXUtils.toVector3f(te.getPos())
                    .add(packInfo.getTranslation().add(te.getRelativeTranslation()))
                    .add(0.5f, 1.5f, 0.5f);
            Vector3f rot = te.getRelativeRotation()
                    .add(packInfo.getRotation())
                    .add(0, te.getRotation() * 22.5f, 0);
            DynamXRenderUtils.spawnParticles(packInfo, te.getWorld(), pos, rot);

            MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity((DynamXBlock<?>) te.getBlockType(), te.getWorld(), te, this, context.getRenderPosition(),
                    context.getPartialTicks(), destroyStage, alpha, EventStage.POST));
        }
        if (shouldRenderDebug()) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            sceneNode.renderDebug(context, packInfo);
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
        }
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    public boolean shouldRenderDebug() {
        return ClientDebugSystem.enableDebugDrawing && (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()
                || DynamXDebugOptions.SEATS_AND_STORAGE.isActive() || DynamXDebugOptions.PLAYER_COLLISIONS.isActive());
    }
}
