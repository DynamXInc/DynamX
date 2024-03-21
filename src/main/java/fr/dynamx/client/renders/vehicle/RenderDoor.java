package fr.dynamx.client.renders.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class RenderDoor<T extends DoorEntity<?>> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderDoor(RenderManager manager) {
        super(manager);
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(DoorEntity.class, this));
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        if (entity.getPackInfo() == null) {
            return null;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getOwner().getModel());
        if (modelRenderer == null) {
            return null;
        }
        return context.setModelParams(entity, modelRenderer, entity.getEntityTextureId());
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        Vector3f scale = entity.getPackInfo().getScaleModifier();
        //TODO USE SCENE GRAPH
        GlStateManager.pushMatrix();
        setupRenderTransform(entity, context.getRenderPosition(), context.getPartialTicks());
        GlStateManager.scale(scale.x, scale.y, scale.z);
        renderModelGroup(context.getModel(), entity.getPackInfo().getObjectName(), entity, context.getTextureId(), false);
        DynamXRenderUtils.popGlAllAttribBits();
        GlStateManager.popMatrix();
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        ((SceneNode<BaseRenderContext.EntityRenderContext, PartDoor>) entity.getPackInfo().getSceneGraph()).renderDebug(context, entity.getPackInfo());
    }
}
