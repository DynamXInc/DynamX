package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "steeringwheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class SteeringWheelInfo extends SubInfoType<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", true);
        return null;
    };

    @PackFileProperty(configNames = "ObjectName", required = false, defaultValue = "SteeringWheel")
    protected String objectName = "SteeringWheel";
    @PackFileProperty(configNames = {"Rotation", "BaseRotation", "BaseRotationQuat"}, required = false, defaultValue = "none", description = "SteeringWheelInfo.Rotation")
    protected Quaternion steeringWheelBaseRotation = null;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    protected Vector3f position = new Vector3f(0.5f, 1.1f, 1);

    public SteeringWheelInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addSubProperty(this);
        getPosition().multLocal(owner.getScaleModifier());
    }

    @Override
    public SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo>> childGraph) {
        return new SteeringWheelNode<>(this, modelScale, childGraph);
    }

    @Override
    public String getNodeName() {
        return getName();
    }

    @Override
    public String getName() {
        return "SteeringWheel";
    }

    class SteeringWheelNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        public SteeringWheelNode(SteeringWheelInfo part, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(part.getPosition(), GlQuaternionPool.newGlQuaternion(part.getSteeringWheelBaseRotation()), scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            DxModelRenderer vehicleModel = context.getModel();
            /* Rendering the steering wheel */
            if (!vehicleModel.containsObjectOrNode(getObjectName()) || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) context.getRender(), entity, PhysicsEntityEvent.Phase.PRE, context.getPartialTicks(), vehicleModel))) {
                return;
            }
            GlStateManager.pushMatrix();
            transform();
            //Rotate the steering wheel
            int directingWheel = VehicleEntityProperties.getPropertyIndex(packInfo.getDirectingWheel(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
            if (entity != null && entity.hasModuleOfType(WheelsModule.class)) {
                WheelsModule m = entity.getModuleByType(WheelsModule.class);
                if (m.visualProperties.length > directingWheel)
                    GlStateManager.rotate(-(m.prevVisualProperties[directingWheel] + (m.visualProperties[directingWheel] - m.prevVisualProperties[directingWheel]) * context.getPartialTicks()), 0F, 0F, 1F);
            }
            //Render it
            vehicleModel.renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
            //TODO PASS CONTEXT MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }
    }
}
