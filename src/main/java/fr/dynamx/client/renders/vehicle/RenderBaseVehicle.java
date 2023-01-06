package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent.Render;
import fr.dynamx.api.events.VehicleEntityEvent.Render.Type;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.util.vector.Quaternion;

public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        addDebugRenderers(new DebugRenderer.HullDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void renderMain(T carEntity, float partialTicks) {
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
        if (!MinecraftForge.EVENT_BUS.post(new Render(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)) && carEntity.getPackInfo().isModelValid()) {
            /* Rendering the chassis */
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            renderMainModel(vehicleModel, carEntity, carEntity.getEntityTextureID());
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);
        }
        MinecraftForge.EVENT_BUS.post(new Render(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public void renderParts(T carEntity, float partialTicks) {
        if (!MinecraftForge.EVENT_BUS.post(new Render(Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
            if (carEntity.getPackInfo().isModelValid()) {
                carEntity.getDrawableModules().forEach(d -> ((IPhysicsModule.IDrawableModule<T>) d).drawParts(this, partialTicks, carEntity));
            }
        }
        MinecraftForge.EVENT_BUS.post(new Render(Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, null));
    }

    @Override
    public void spawnParticles(T carEntity, Quaternion rotation, float partialTicks) {
        super.spawnParticles(carEntity, rotation, partialTicks);
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
