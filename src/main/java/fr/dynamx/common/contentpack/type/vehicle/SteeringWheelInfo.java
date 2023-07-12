package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 */
@RegisteredSubInfoType(name = "steeringwheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class SteeringWheelInfo extends SubInfoType<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>> {
    @PackFileProperty(configNames = "PartName", required = false, defaultValue = "SteeringWheel")
    private String partName = "SteeringWheel";
    @PackFileProperty(configNames = {"Rotation", "BaseRotation", "BaseRotationQuat"}, required = false, defaultValue = "none")
    private Quaternion steeringWheelBaseRotation = null;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position = new Vector3f(0.5f, 1.1f, 1);

    public SteeringWheelInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addSubProperty(this);
        getSteeringWheelPosition().multLocal(owner.getScaleModifier());
    }

    public String getPartName() {
        return partName;
    }

    public Quaternion getSteeringWheelBaseRotation() {
        return steeringWheelBaseRotation;
    }

    public Vector3f getSteeringWheelPosition() {
        return position;
    }

    @Override
    public String getName() {
        return "SteeringWheel";
    }

    @Override
    public void drawParts(@Nullable BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks) {
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(packInfo.getModel());
        /* Rendering the steering wheel */
        ObjObjectRenderer steeringWheel = vehicleModel.getObjObjectRenderer(getPartName());
        if (steeringWheel == null || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
            return;
        }
        GlStateManager.pushMatrix();
        Vector3f center = getSteeringWheelPosition();
        //Translation to the steering wheel rotation point (and render pos)
        GlStateManager.translate(center.x, center.y, center.z);

        //Apply steering wheel base rotation
        if (getSteeringWheelBaseRotation() != null)
            GlStateManager.rotate(GlQuaternionPool.get(getSteeringWheelBaseRotation()));
        //Rotate the steering wheel
        int directingWheel = VehicleEntityProperties.getPropertyIndex(packInfo.getDirectingWheel(), VehicleEntityProperties.EnumVisualProperties.STEERANGLE);
        if (entity != null && entity.hasModuleOfType(WheelsModule.class)) {
            WheelsModule m = entity.getModuleByType(WheelsModule.class);
            if(m.visualProperties.length > directingWheel)
                GlStateManager.rotate(-(m.prevVisualProperties[directingWheel] + (m.visualProperties[directingWheel] - m.prevVisualProperties[directingWheel]) * partialTicks), 0F, 0F, 1F);
        }

        //Scale it
        GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
        //Render it
        vehicleModel.renderGroup(steeringWheel, textureId);
        GlStateManager.popMatrix();
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public String[] getRenderedParts() {
        return new String[] {getPartName()};
    }
}
