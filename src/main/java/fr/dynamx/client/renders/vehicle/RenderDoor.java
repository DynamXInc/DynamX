package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderDoor<T extends DoorEntity<?>> extends RenderPhysicsEntity<T> {

    public RenderDoor(RenderManager manager) {
        super(manager);
        addDebugRenderers(new VehicleDebugRenderer.DoorPointsDebug(), new DebugRenderer.HullDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(DoorEntity.class, this));
    }

    @Override
    public void renderMain(T entity, float partialsTicks) {
        BaseVehicleEntity<?> carEntity = entity.getVehicleEntity(entity.world);
        if (carEntity != null) {
            DxModelRenderer vehicleModel = DynamXContext.getDxModelRegistry().getModel(carEntity.getPackInfo().getModel());
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            renderModelGroup(vehicleModel, entity.getPackInfo().getPartName(), carEntity, carEntity.getEntityTextureID());
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);
        }
    }

    @Override
    public void renderParts(T entity, float partialTicks) {

    }

}
