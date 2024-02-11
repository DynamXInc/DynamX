package fr.dynamx.client.renders;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class RenderProp<T extends PropsEntity<?>> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderProp(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug(), new DebugRenderer.StoragesDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(PropsEntity.class, this));
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        if (entity.getPackInfo() == null) {
            return null;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        if (modelRenderer == null) {
            return null;
        }
        return context.setModelParams(entity, modelRenderer, entity.getEntityTextureID());
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        ((SceneNode<BaseRenderContext.EntityRenderContext, PropObject<?>>) entity.getPackInfo().getSceneGraph()).render(context, entity.getPackInfo());
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        ((SceneNode<BaseRenderContext.EntityRenderContext, PropObject<?>>) entity.getPackInfo().getSceneGraph()).renderDebug(context, entity.getPackInfo());
    }
}
