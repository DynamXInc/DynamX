package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderDoor<T extends DoorEntity<?>> extends RenderPhysicsEntity<T> {

    public RenderDoor(RenderManager manager) {
        super(manager);
        addDebugRenderers(new VehicleDebugRenderer.DoorPointsDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(DoorEntity.class, this));
    }

    @Override
    public void renderEntity(T entity, double x, double y, double z, float partialTicks, boolean useVanillaRender) {
        BaseVehicleEntity<?> carEntity = entity.getVehicleEntity(entity.world);
        //TODO USE SCENE GRAPH
        if (carEntity != null) {
            GlStateManager.pushMatrix();
            setupRenderTransform(entity, x, y, z, partialTicks);
            DxModelRenderer vehicleModel = DynamXContext.getDxModelRegistry().getModel(carEntity.getPackInfo().getModel());
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            renderModelGroup(vehicleModel, entity.getPackInfo().getObjectName(), carEntity, carEntity.getEntityTextureID(), false);
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);
            GlStateManager.popMatrix();
        }
    }
}
