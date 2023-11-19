package fr.dynamx.client.renders.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class RenderDoor<T extends DoorEntity<?>> extends RenderPhysicsEntity<T> {
    protected final EntityRenderContext context = new EntityRenderContext(this);

    public RenderDoor(RenderManager manager) {
        super(manager);
        addDebugRenderers(new VehicleDebugRenderer.DoorPointsDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(DoorEntity.class, this));
    }

    @Override
    @Nullable
    public EntityRenderContext getRenderContext(T entity) {
        if (entity.getPackInfo() == null) {
            return null;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getOwner().getModel());
        if (modelRenderer == null) {
            return null;
        }
        return context.setEntityParams(modelRenderer, entity.getEntityTextureID());
    }

    @Override
    public void renderEntity(T entity, EntityRenderContext context) {
        Vector3f scale = entity.getPackInfo().getScaleModifier();
        //TODO USE SCENE GRAPH
        GlStateManager.pushMatrix();
        setupRenderTransform(entity, context.getX(), context.getY(), context.getZ(), context.getPartialTicks());
        GlStateManager.scale(scale.x, scale.y, scale.z);
        renderModelGroup(context.getModel(), entity.getPackInfo().getObjectName(), entity, context.getTextureId(), false);
        GlStateManager.popMatrix();
    }
}
