package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.HelicopterPhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Basic wheel implementation <br>
 * Works with an {@link EngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
public class HelicopterPropulsionModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    private final HelicopterEngineModule module;
    protected HelicopterPhysicsHandler helicopterPhysics;

    //TODO private float targetPower;

    public HelicopterPropulsionModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
        module = entity.getModuleByType(HelicopterEngineModule.class);
    }

    @Override
    public void initEntityProperties() {
    }

    @Override
    public void initPhysicsEntity(BaseVehiclePhysicsHandler<?> handler) {
        helicopterPhysics = new HelicopterPhysicsHandler(this, handler);
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            if (entity.ticksExisted > 10) {
                //TODO RAYTRACE IF ON GROUND
            }
        }
    }

    public HelicopterPhysicsHandler getPhysicsHandler() {
        return helicopterPhysics;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
            this.entity.getPackInfo().getPartsByType(PartRotor.class).forEach(partRotor -> {
                renderRotor(render, partRotor, partialTicks, carEntity, vehicleModel);
            });
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
        }
    }

    @SideOnly(Side.CLIENT)
    private void renderRotor(RenderPhysicsEntity<?> render, PartRotor partRotor, float partialTicks, BaseVehicleEntity<?> helicopterEntity, ObjModelRenderer vehicleModel) {
        ObjObjectRenderer rotor = vehicleModel.getObjObjectRenderer(partRotor.getPartName());
        if (rotor != null) {
            if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
                GlStateManager.pushMatrix();
                Vector3f center = partRotor.getPosition();
                //Translation to the steering wheel rotation point (and render pos)
                GlStateManager.translate(center.x, center.y, center.z);
                // Rotating the rotor.
                HelicopterEntity helicopter = (HelicopterEntity) helicopterEntity;
                if (helicopter.getEngine().isEngineStarted()) {
                    //get power from engine
                    HelicopterEngineModule d = (HelicopterEngineModule) helicopter.getEngine();
                    float power = d.getPower(); //TODO INTERPOLATION
                    GlStateManager.rotate((entity.ticksExisted + partialTicks) * partRotor.getRotationSpeed() * power, partRotor.getRotation().x, partRotor.getRotation().y, partRotor.getRotation().z);
                }
                //Scale it
                GlStateManager.scale(helicopterEntity.getPackInfo().getScaleModifier().x, helicopterEntity.getPackInfo().getScaleModifier().y, helicopterEntity.getPackInfo().getScaleModifier().z);
                //Render it
                vehicleModel.renderGroup(rotor, helicopterEntity.getEntityTextureID());
                GlStateManager.popMatrix();
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
            }
        }
    }
}
