package fr.dynamx.core.client.renders;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.EventPhase;
import fr.dynamx.core.client.handlers.ClientDebugSystem;
import fr.dynamx.core.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.core.client.renders.scene.BaseRenderContext;
import fr.dynamx.core.client.renders.scene.node.SceneNode;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.type.objects.BlockObject;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.client.DynamXRenderUtils;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.block.Block;
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
        JmeVector3fPool.openPool();
        QuaternionPool.openPool();
        BaseRenderContext.BlockRenderContext context = this.context.setModelParams(te, modelRenderer, (byte) te.getBlockMetadata());
        context.setRenderParams(x, y, z, partialTicks, false);
        SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode = (SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>>) packInfo.getSceneGraph();
        Block block = te.getBlockType(); // The case where this is reached, and the block here is air exists
        if (!MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity(block instanceof DynamXBlock ? (DynamXBlock<?>) block : null, context, sceneNode, this, destroyStage, alpha, EventPhase.PRE))) {
            sceneNode.render(context, packInfo, null);
            Vector3f pos = DynamXUtils.toVector3f(te.getPos())
                    .add(packInfo.getTranslation().add(te.getRelativeTranslation()))
                    .add(0.5f, 1.5f, 0.5f);
            Vector3f rot = te.getRelativeRotation()
                    .add(packInfo.getRotation())
                    .add(0, te.getRotation() * 22.5f, 0);
            DynamXRenderUtils.spawnParticles(packInfo, te.getWorld(), pos, rot);
            MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity(block instanceof DynamXBlock ? (DynamXBlock<?>) block : null, context, sceneNode, this, destroyStage, alpha, EventPhase.POST));
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
        JmeVector3fPool.closePool();
    }

    public boolean shouldRenderDebug() {
        return ClientDebugSystem.enableDebugDrawing;
    }
}
