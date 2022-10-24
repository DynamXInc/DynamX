package fr.dynamx.common.entities.modules;

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.HelicopterPhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Basic wheel implementation <br>
 *     Works with an {@link EngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
public class HelicopterPropulsionModule implements IPropulsionModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>>
{
    /**
     * Entity visual properties, accessible via the {@link IPhysicsModule}s
     */
    public float[] visualProperties;
    /**
     * Entity prev visual properties
     */
    public float[] prevVisualProperties;

    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    protected HelicopterPhysicsHandler wheelsPhysics;

    public HelicopterPropulsionModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    @Override
    public void initEntityProperties() {
        int wheelCount = 0; //TODO
        visualProperties = new float[wheelCount * VehicleEntityProperties.EnumVisualProperties.values().length];
        prevVisualProperties = new float[visualProperties.length];
    }

    @Override
    public void initPhysicsEntity(BaseVehiclePhysicsHandler<?> handler) {
        wheelsPhysics = new HelicopterPhysicsHandler(this, handler);
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            if(entity.ticksExisted > 10) {
                //TODO RAYTRACE IF ON GROUND
            }
        }

        if (simulatingPhysics) {
            System.arraycopy(visualProperties, 0, prevVisualProperties, 0, prevVisualProperties.length);
            updateVisualProperties(visualProperties, prevVisualProperties);
        }
        else {
            System.arraycopy(visualProperties, 0, prevVisualProperties, 0, prevVisualProperties.length);
        }
    }


    public void updateVisualProperties(float[] visualProperties, float[] prevVisualProperties) {
        if (wheelsPhysics != null) {
            byte n =0;//TODO  wheelsPhysics.getNumWheels();

            for (int i = 0; i < n; i++) {
                VehicleWheel info = null;//wheelsPhysics.getHandler().getPhysicsVehicle().getWheel(i);
                if (info.isFrontWheel()) {
                    visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.STEERANGLE)] = (info.getSteerAngle() * DynamXGeometry.radToDeg);
                }

                int ind = VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.ROTATIONANGLE);
                //Update prevRotation, so we have -180<prevRotationYaw-rotationYaw<180 to avoid visual glitch
                float[] angles = DynamXMath.interpolateAngle((info.getRotationAngle() * DynamXGeometry.radToDeg)%360, visualProperties[ind], 1);
                prevVisualProperties[ind] = angles[0];
                visualProperties[ind] = angles[1];

                    //System.out.println("Wheel "+i+" "+info.getSuspensionLength()+" "+info.getRestLength());
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.SUSPENSIONLENGTH)]=  info.getSuspensionLength() + info.getRestLength();
                Vector3f pos = Vector3fPool.get();
                info.getCollisionLocation(pos);
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONX)] = pos.x;
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONY)] = pos.y;
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONZ)] = pos.z;
            }
        }
    }

    @Override
    public IPropulsionHandler getPhysicsHandler() {
        return wheelsPhysics;
    }

    @Override
    public float[] getPropulsionProperties() {
        return new float[0]; //TODO
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder, List<ResourceLocation> variables) {
        if(side.isServer())
            variables.add(VehicleSynchronizedVariables.Visuals.NAME);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        ObjModelClient vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());

        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            if(getPropulsionProperties() != null) {
                this.entity.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> {
                    renderWheel(render, partWheel, partialTicks);
                });
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {
    }

    @SideOnly(Side.CLIENT)
    protected void renderWheel(RenderPhysicsEntity<?> render, PartWheel partWheel, float partialTicks) {
    }
}
