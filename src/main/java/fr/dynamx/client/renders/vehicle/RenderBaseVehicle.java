package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.client.DynamXEntityRenderEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
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
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void spawnParticles(T carEntity, BaseRenderContext.EntityRenderContext context) {
        super.spawnParticles(carEntity, context);
        if (!MinecraftForge.EVENT_BUS.post(new DynamXEntityRenderEvent(carEntity, context, DynamXEntityRenderEvent.Type.PARTICLES, 0))) {
            if (carEntity.hasModuleOfType(WheelsModule.class)) {
                //TODO GENERALIZE, USE SUPER
                carEntity.getModuleByType(WheelsModule.class).spawnPropulsionParticles(this, context.getPartialTicks());
            }
        }
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
        return context.setModelParams(entity, modelRenderer, entity.getEntityTextureId());
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        ((SceneNode<BaseRenderContext.EntityRenderContext, ModularVehicleInfo>) entity.getPackInfo().getSceneGraph()).render(context, entity.getPackInfo());
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        ((SceneNode<BaseRenderContext.EntityRenderContext, ModularVehicleInfo>) entity.getPackInfo().getSceneGraph()).renderDebug(context, entity.getPackInfo());
    }

    /**
     * Renders the entity with the given texture id
     *
     * @param packInfo  The pack info of the entity
     * @param textureId The texture id
     */
    public void renderEntity(ModularVehicleInfo packInfo, byte textureId) {
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (modelRenderer == null) {
            return;
        }
        ((SceneNode<IRenderContext, ModularVehicleInfo>) packInfo.getSceneGraph()).render(context.setRenderParams(0, 0, 0, 1, true).setModelParams(modelRenderer, textureId), packInfo);
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
