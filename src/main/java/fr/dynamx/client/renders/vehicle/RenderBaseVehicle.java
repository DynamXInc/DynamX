package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent.Render;
import fr.dynamx.api.events.VehicleEntityEvent.Render.Type;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
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

import javax.annotation.Nullable;

public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void renderMain(T entity, float partialTicks) {
        renderMain(entity, entity.getPackInfo(), entity.getEntityTextureID(), partialTicks, false);
    }

    public void renderMain(@Nullable T carEntity, ModularVehicleInfo packInfo, byte textureId, float partialTicks, boolean forceVanillaRender) {
        DxModelRenderer vehicleModel = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (!MinecraftForge.EVENT_BUS.post(new Render(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)) && packInfo.isModelValid()) {
            /* Rendering the chassis */
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
            renderMainModel(vehicleModel, carEntity, textureId, forceVanillaRender);
            GlStateManager.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
        }
        MinecraftForge.EVENT_BUS.post(new Render(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public void renderParts(T entity, float partialTicks) {
        renderParts(entity, entity.getPackInfo(), entity.getEntityTextureID(), partialTicks, false);
    }

    public void renderParts(@Nullable T carEntity, ModularVehicleInfo packInfo, byte textureId, float partialTicks, boolean forceVanillaRender) {
        if (!MinecraftForge.EVENT_BUS.post(new Render(Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
            if (packInfo.isModelValid()) {
                packInfo.getDrawableParts().forEach(d -> ((IDrawablePart<T>) d).drawParts(carEntity, this, packInfo, textureId, partialTicks, forceVanillaRender));
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
