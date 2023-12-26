package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "steeringwheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class SteeringWheelInfo extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "ObjectName", required = false, defaultValue = "SteeringWheel")
    protected String objectName = "SteeringWheel";
    @PackFileProperty(configNames = {"Rotation", "BaseRotation", "BaseRotationQuat"}, required = false, defaultValue = "From model", description = "SteeringWheelInfo.Rotation")
    protected Quaternion steeringWheelBaseRotation = null;

    public SteeringWheelInfo(ModularVehicleInfo owner) {
        super(owner, "steeringwheel");
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        Quaternion rot = readPositionFromModel(owner.getModel(), getObjectName(), true, steeringWheelBaseRotation == null);
        if (rot != null)
            steeringWheelBaseRotation = rot;
        super.appendTo(owner);
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
            super(part.getPosition(), GlQuaternionPool.newGlQuaternion(part.getSteeringWheelBaseRotation()), SteeringWheelInfo.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            DxModelRenderer vehicleModel = context.getModel();
            /* Rendering the steering wheel */
            GlStateManager.pushMatrix();
            //Translate to the steering wheel rotation point
            transformToRotationPoint();
            //Rotate the steering wheel
            int directingWheel = VehicleEntityProperties.getPropertyIndex(packInfo.getDirectingWheel(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
            if (entity != null && entity.hasModuleOfType(WheelsModule.class)) {
                WheelsModule m = entity.getModuleByType(WheelsModule.class);
                if (m.visualProperties.length > directingWheel)
                    GlStateManager.rotate(-(m.prevVisualProperties[directingWheel] + (m.visualProperties[directingWheel] - m.prevVisualProperties[directingWheel]) * context.getPartialTicks()), 0F, 0F, 1F);
            }
            //Translate to the origin of the model
            transformToPartPos();
            //Render it
            vehicleModel.renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (DynamXDebugOptions.WHEELS.isActive()) {
                /* Rendering the steering wheel debug */
                GlStateManager.pushMatrix();
                transformForDebug();
                RenderGlobal.drawBoundingBox(-0.25f, -0.25f, -0.1f, 0.25f, 0.25f, 0.1f,
                        0.5f, 1, 0.5f, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
