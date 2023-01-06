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
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Basic wheel implementation <br>
 * Works with an {@link CarEngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
public class HelicopterRotorsModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>>, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    private HelicopterEngineModule engine;

    private float curPower, curAngle;

    public HelicopterRotorsModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    @Override
    public void initEntityProperties() {
        engine = entity.getModuleByType(HelicopterEngineModule.class);
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    public void updateEntity() {
        float targetPower = engine.getPower();
        curPower = curPower + (targetPower - curPower) / 60; //3-seconds interpolation
        curAngle += curPower;
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
                GlStateManager.rotate((curAngle + partialTicks * curPower) * partRotor.getRotationSpeed(), partRotor.getRotation().x, partRotor.getRotation().y, partRotor.getRotation().z);
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
