package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.DynamXEntityRenderEvents;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    protected final EntityRenderContext context = new EntityRenderContext(this);

    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void spawnParticles(T carEntity, EntityRenderContext context) {
        super.spawnParticles(carEntity, context);
        if (!MinecraftForge.EVENT_BUS.post(new DynamXEntityRenderEvents.Render(carEntity, context, DynamXEntityRenderEvents.Render.Type.PARTICLES, 0))) {
            if (carEntity.hasModuleOfType(WheelsModule.class)) {
                //TODO GENERALIZE, USE SUPER
                carEntity.getModuleByType(WheelsModule.class).spawnPropulsionParticles(this, context.getPartialTicks());
            }
        }
    }

    @Override
    @Nullable
    public EntityRenderContext getRenderContext(T entity) {
        if (entity.getPackInfo() == null) {
            return null;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        if (modelRenderer == null) {
            return null;
        }
        return context.setEntityParams(modelRenderer, entity.getEntityTextureID());
    }

    @Override
    public void renderEntity(T entity, EntityRenderContext context) {
        ((SceneGraph<T, ModularVehicleInfo>) entity.getPackInfo().getSceneGraph()).render(entity, context, entity.getPackInfo());
    }

    /**
     * Renders the entity with the given texture id
     *
     * @param packInfo The pack info of the entity
     * @param textureId The texture id
     */
    public void renderEntity(ModularVehicleInfo packInfo, byte textureId) {
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (modelRenderer == null) {
            return;
        }
        ((SceneGraph<T, ModularVehicleInfo>) packInfo.getSceneGraph()).render(null, context.setEntityParams(modelRenderer, textureId).setRenderParams(0, 0, 0, 1, true), packInfo);
    }

    public static class RenderCar<T extends CarEntity<?>> extends RenderBaseVehicle<T> {
        public RenderCar(RenderManager manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, true);
        }
    }

    public static class RenderTrailer<T extends TrailerEntity<?>> extends RenderBaseVehicle<T> {
        public RenderTrailer(RenderManager manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, false);
        }
    }

    public static class RenderBoat<T extends BoatEntity<?>> extends RenderBaseVehicle<T> {
        public RenderBoat(RenderManager manager) {
            super(manager);
            BoatDebugRenderer.addAll(this);
        }
    }

    public static class RenderHelicopter<T extends HelicopterEntity<?>> extends RenderBaseVehicle<T> {
        public RenderHelicopter(RenderManager manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, true);
        }
    }
}
