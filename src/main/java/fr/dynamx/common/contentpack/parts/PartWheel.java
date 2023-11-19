package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "wheel", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartWheel extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("isRight".equals(key))
            return new IPackFilePropertyFixer.FixResult("IsRight", true);
        return null;
    };

    @Accessors(fluent = true)
    @PackFileProperty(configNames = "IsRight")
    protected boolean isRight;
    @PackFileProperty(configNames = "IsSteerable")
    protected boolean wheelIsSteerable;
    @PackFileProperty(configNames = "MaxTurn")
    protected float wheelMaxTurn;
    @PackFileProperty(configNames = "DrivingWheel")
    protected boolean drivingWheel;
    @PackFileProperty(configNames = "HandBrakingWheel", required = false)
    protected boolean handBrakingWheel;
    @PackFileProperty(configNames = "AttachedWheel")
    protected String defaultWheelName;
    @PackFileProperty(configNames = "MudGuard", required = false)
    protected String mudGuardObjectName;
    @PackFileProperty(configNames = "RotationPoint", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    protected Vector3f rotationPoint;
    @PackFileProperty(configNames = "SuspensionAxis", required = false)
    protected Quaternion suspensionAxis;

    protected PartWheelInfo defaultWheelInfo;

    public PartWheel(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0.75f, 0.75f);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        if (getRotationPoint() == null)
            rotationPoint = getPosition();
        else
            getRotationPoint().multLocal(getScaleModifier(owner));
        if (suspensionAxis != null && suspensionAxis.inverse() == null) {
            DynamXErrorManager.addPackError(getPackName(), "wheel_invalid_suspaxis", ErrorLevel.LOW, getName(), "The SuspensionAxis should be an invertible Quaternion");
            suspensionAxis = null;
        }
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.WHEELS;
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer with) {
        return false;
    }

    public void setDefaultWheelInfo(PartWheelInfo partWheelInfo) {
        if (partWheelInfo == null) {
            throw new IllegalArgumentException("Attached wheel info " + getDefaultWheelName() + " was not found !");
        } else {
            defaultWheelInfo = partWheelInfo;
            setBox(new AxisAlignedBB(-partWheelInfo.getWheelWidth(), -partWheelInfo.getWheelRadius(), -partWheelInfo.getWheelRadius(),
                    partWheelInfo.getWheelWidth(), partWheelInfo.getWheelRadius(), partWheelInfo.getWheelRadius()));
        }
    }

    @Override
    public String getName() {
        return "PartWheel named " + getPartName();
    }

    @Override
    public String[] getRenderedParts() {
        if (getMudGuardObjectName() != null)
            return new String[]{getMudGuardObjectName()};
        return new String[0];
    }

    @Override
    public String getObjectName() {
        return null;
    }

    @Override
    public void onTexturesChange(BaseVehicleEntity<?> entity) {
        if (!entity.hasModuleOfType(WheelsModule.class))
            return;
        entity.getModuleByType(WheelsModule.class).computeWheelsTextureIds();
    }

    @Override
    public SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo>> childGraph) {
        return new PartWheelNode<>(this, modelScale, childGraph);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!(entity instanceof BaseVehicleEntity))
            throw new IllegalStateException("The entity " + entity + " has PartSeats, but isn't a vehicle !");
        if (!modules.hasModuleOfClass(WheelsModule.class))
            modules.add(new WheelsModule((BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>>) entity));
    }

    class PartWheelNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        public PartWheelNode(PartWheel wheel, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(wheel.getPosition(), null, scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            WheelsModule wheelsModule = entity != null ? entity.getModuleByType(WheelsModule.class) : null;
            if (wheelsModule != null && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.REMOVED)
                return;
            PartWheelInfo info = wheelsModule != null ? wheelsModule.getWheelInfo(getId()) : getDefaultWheelInfo();
            if (!info.isModelValid())
                return;
            GlStateManager.pushMatrix();
            /* Translation to the wheel rotation point */
            GlStateManager.translate(getRotationPoint().x, getRotationPoint().y, getRotationPoint().z);

            /* Apply wheel base rotation */
            com.jme3.math.Quaternion baseRotation = getSuspensionAxis();
            if (baseRotation != null && baseRotation.getW() != 0)
                GlStateManager.rotate(GlQuaternionPool.get(baseRotation));

            int index;
            if (wheelsModule != null) {
                /* Suspension translation */
                index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.SUSPENSION_LENGTH);
                GlStateManager.translate(0, -(wheelsModule.prevVisualProperties[index] + (wheelsModule.visualProperties[index] - wheelsModule.prevVisualProperties[index]) * context.getPartialTicks())
                        - info.getSuspensionRestLength(), 0);

                /* Steering rotation*/
                if (isWheelIsSteerable()) {
                    index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
                    GlStateManager.rotate((wheelsModule.prevVisualProperties[index] + (wheelsModule.visualProperties[index] - wheelsModule.prevVisualProperties[index]) * context.getPartialTicks()), 0.0F, 1.0F, 0.0F);
                }
            }
            /* Render mudguard */
            if (getMudGuardObjectName() != null) {
                //TODO MUDGUARD MUST HAVE IS SCENE, CONTAINING THE WHEEL
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0.2, 0);
                GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
                context.getModel().renderGroups(getMudGuardObjectName(), context.getTextureId(), context.isUseVanillaRender());
                GlStateManager.popMatrix();
            }

            //Remove wheel base rotation
            if (baseRotation != null && baseRotation.getW() != 0)
                GlStateManager.rotate(GlQuaternionPool.get(baseRotation.inverse()));

            // Translate to render pos, from rotation pos
            GlStateManager.translate(getPosition().x - getRotationPoint().x, getPosition().y - getRotationPoint().y, getPosition().z - getRotationPoint().z);

            if (wheelsModule != null) {
                index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.ROTATION_ANGLE);
                //Fix sign problems for wheel rotation
                float prev = wheelsModule.prevVisualProperties[index];
                if (prev - wheelsModule.visualProperties[index] > 180)
                    prev -= 360;
                if (prev - wheelsModule.visualProperties[index] < -180)
                    prev += 360;
                //Then render
                if (isRight()) {
                    /* Wheel rotation (Right-Side)*/
                    GlStateManager.rotate(180, 0, 1, 0);
                    GlStateManager.rotate((prev + (wheelsModule.visualProperties[index] - prev) * context.getPartialTicks()), -1.0F, 0.0F, 0.0F);
                } else {
                    /* Wheel rotation (Left-Side)*/
                    GlStateManager.rotate(-(prev + (wheelsModule.visualProperties[index] - prev) * context.getPartialTicks()), -1.0F, 0.0F, 0.0F);
                }
            }
            /*Rendering the wheels */
            DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(info.getModel());
            //Scale
            scale.set(info.getScaleModifier());
            GlStateManager.scale(scale.x, scale.y, scale.z);
            //If the wheel is not flattened, or the model does not supports flattening
            if (wheelsModule == null || wheelsModule.getWheelsStates()[getId()] != WheelsModule.WheelState.ADDED_FLATTENED || !model.renderGroups("rim", wheelsModule.getWheelsTextureId()[getId()], context.isUseVanillaRender())) {
                byte wheelTextureId = wheelsModule != null ? wheelsModule.getWheelsTextureId()[getId()] : info.getIdForVariant(packInfo.getVariantName(context.getTextureId()));
                context.getRender().renderModel(model, entity, wheelTextureId, context.isUseVanillaRender());
            }
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (DynamXDebugOptions.WHEELS.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                AxisAlignedBB box = getBox();
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        isDrivingWheel() ? 0 : 1, isDrivingWheel() ? 1 : 0, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
