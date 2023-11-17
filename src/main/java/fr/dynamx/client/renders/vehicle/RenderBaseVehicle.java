package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent.Render;
import fr.dynamx.api.events.VehicleEntityEvent.Render.Type;
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

public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void spawnParticles(T carEntity, float partialTicks) {
        super.spawnParticles(carEntity, partialTicks);
        if (!MinecraftForge.EVENT_BUS.post(new Render(Type.PARTICLES, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
            if (carEntity.hasModuleOfType(WheelsModule.class)) {
                //TODO GENERALIZE
                carEntity.getModuleByType(WheelsModule.class).spawnPropulsionParticles(this, partialTicks);
            }
            MinecraftForge.EVENT_BUS.post(new Render(Type.PARTICLES, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, null));
        }
    }

    @Override
    public boolean canRender(T entity) {
        return super.canRender(entity) && entity.getEntityTextureID() != -1;
    }

    @Override
    public void renderEntity(T entity, double x, double y, double z, float partialTicks, boolean useVanillaRender) {
        if (entity.getPackInfo() == null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ);
            return;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        if (modelRenderer == null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ);
            return;
        }
        EntityRenderContext context = new EntityRenderContext(this, modelRenderer, entity.getEntityTextureID(), x, y, z, partialTicks, useVanillaRender);
        ((SceneGraph<T, ModularVehicleInfo>) entity.getPackInfo().getSceneGraph()).render(entity, context, entity.getPackInfo());
    }

    public void renderMain(ModularVehicleInfo packInfo, byte textureId, boolean useVanillaRender) {
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (modelRenderer == null) {
            return;
        }
        EntityRenderContext context = new EntityRenderContext(this, modelRenderer, textureId, 0, 0, 0, 1, useVanillaRender);
        ((SceneGraph<T, ModularVehicleInfo>) packInfo.getSceneGraph()).render(null, context, packInfo);
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
