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
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
    @PackFileProperty(configNames = "IsRight", required = false, defaultValue = "True if name contains 'right'")
    protected boolean isRight;
    @PackFileProperty(configNames = "IsSteerable", required = false, defaultValue = "True if name contains 'front'")
    protected boolean wheelIsSteerable;
    @PackFileProperty(configNames = "MaxTurn", required = false, defaultValue = "0")
    protected float wheelMaxTurn;
    @PackFileProperty(configNames = "DrivingWheel")
    protected boolean drivingWheel;
    @PackFileProperty(configNames = "HandBrakingWheel", required = false)
    protected boolean handBrakingWheel;
    @PackFileProperty(configNames = "AttachedWheel")
    protected String defaultWheelName;
    @PackFileProperty(configNames = "Rim", required = false)
    protected String rimObjectName;
    @PackFileProperty(configNames = {"Tire", "Tyre"}, required = false)
    protected String tireObjectName;
    @PackFileProperty(configNames = "MudGuard", required = false)
    protected String mudGuardObjectName;
    @PackFileProperty(configNames = "RotationPoint", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, defaultValue = "From model")
    protected Vector3f rotationPoint;
    @PackFileProperty(configNames = "SuspensionAxis", required = false, defaultValue = "From model")
    protected Quaternion suspensionAxis;

    protected PartWheelInfo defaultWheelInfo;

    public PartWheel(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0.75f, 0.75f);
        wheelIsSteerable = partName.toLowerCase().contains("front");
        isRight = partName.toLowerCase().contains("right");
    }

    protected void readMudguardPositionFromModel(ResourceLocation model) {
        if (getRotationPoint() == null) {
            DxModelData modelData = DynamXContext.getDxModelDataFromCache(DynamXUtils.getModelPath(getPackName(), model));
            if (modelData != null) {
                setRotationPoint(DynamXUtils.readPartPosition(modelData, getMudGuardObjectName(), true));
                if (suspensionAxis == null)
                    suspensionAxis = DynamXUtils.readPartRotation(modelData, getMudGuardObjectName());
            }
            if (getRotationPoint() == null) {
                DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, owner.getFullName(), "3D object '" + getMudGuardObjectName() + "' of part " + getName() + " (for property 'RotationPoint')");
            }
        }
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        if (getRimObjectName() != null)
            readPositionFromModel(owner.getModel(), getRimObjectName(), true, false);
        if (getMudGuardObjectName() != null)
            readMudguardPositionFromModel(owner.getModel());
        super.appendTo(owner);
        if (getRotationPoint() == null)
            rotationPoint = getPosition();
        else
            getRotationPoint().multLocal(getScaleModifier(owner));
        if (suspensionAxis != null && (suspensionAxis.inverse() == null || suspensionAxis.getW() == 0)) {
            DynamXErrorManager.addPackError(getPackName(), "wheel_invalid_suspaxis", ErrorLevel.HIGH, getName(), "The SuspensionAxis should be an invertible Quaternion");
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
        }
        defaultWheelInfo = partWheelInfo;
        setBox(new AxisAlignedBB(-partWheelInfo.getWheelWidth(), -partWheelInfo.getWheelRadius(), -partWheelInfo.getWheelRadius(),
                partWheelInfo.getWheelWidth(), partWheelInfo.getWheelRadius(), partWheelInfo.getWheelRadius()));
        if (getRimObjectName() == null && partWheelInfo.getModel() == null) {
            DynamXErrorManager.addPackError(getPackName(), "wheel_no_model", ErrorLevel.HIGH, owner.getFullName(), getName() + " using wheel info: " + partWheelInfo.getFullName());
        }
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public String[] getRenderedParts() {
        return Stream.of(getRimObjectName(), getTireObjectName(), getMudGuardObjectName()).filter(Objects::nonNull).toArray(String[]::new);
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
        if (getMudGuardObjectName() != null) {
            PartAttachedWheelNode<BaseVehicleEntity<?>, ModularVehicleInfo> wheelNode = new PartAttachedWheelNode<>(this, modelScale, childGraph);
            if (childGraph == null)
                childGraph = new ArrayList<>();
            childGraph.add(wheelNode);
            return new PartBaseWheelNode<>(this, modelScale, childGraph, true);
        }
        return new PartBaseWheelNode<>(this, modelScale, childGraph, false);
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

    protected void applyWheelRotation(EntityRenderContext context, PartWheelInfo info, WheelsModule wheelsModule) {
        boolean isSeparateModel = getRimObjectName() == null;
        int index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.ROTATION_ANGLE);
        //Fix sign problems for wheel rotation
        float prev = wheelsModule.prevVisualProperties[index];
        if (prev - wheelsModule.visualProperties[index] > 180)
            prev -= 360;
        if (prev - wheelsModule.visualProperties[index] < -180)
            prev += 360;
        //Then render
        if (isRight() && isSeparateModel) {
            /* Wheel rotation (Right-Side)*/
            GlStateManager.rotate(180, 0, 1, 0);
            GlStateManager.rotate((prev + (wheelsModule.visualProperties[index] - prev) * context.getPartialTicks()), -1.0F, 0.0F, 0.0F);
        } else {
            /* Wheel rotation (Left-Side)*/
            GlStateManager.rotate(-(prev + (wheelsModule.visualProperties[index] - prev) * context.getPartialTicks()), -1.0F, 0.0F, 0.0F);
        }
    }

    protected void renderWheel(@Nullable BaseVehicleEntity<?> entity, EntityRenderContext context, ModularVehicleInfo entityInfo, PartWheelInfo wheelInfo, WheelsModule wheelsModule) {
        boolean isSeparateModel = getRimObjectName() == null;
        DxModelRenderer model = isSeparateModel ? DynamXContext.getDxModelRegistry().getModel(wheelInfo.getModel()) : context.getModel();
        /*Rendering the wheels */
        byte wheelTextureId = wheelsModule != null ? wheelsModule.getWheelsTextureId()[getId()] : wheelInfo.getIdForVariant(entityInfo.getVariantName(context.getTextureId()));
        if (isSeparateModel) {
            //If the wheel is not flattened, or the model does not supports flattening
            if (wheelsModule == null || wheelsModule.getWheelsStates()[getId()] != WheelsModule.WheelState.ADDED_FLATTENED || !model.renderGroup("rim", wheelsModule.getWheelsTextureId()[getId()], context.isUseVanillaRender())) {
                context.getRender().renderModel(model, entity, wheelTextureId, context.isUseVanillaRender());
            }
        } else {
            if (getRimObjectName() != null) {
                context.getModel().renderGroup(getRimObjectName(), wheelTextureId, context.isUseVanillaRender());
            }
            if (getTireObjectName() != null || wheelsModule == null || wheelsModule.getWheelsStates()[getId()] != WheelsModule.WheelState.ADDED_FLATTENED) {
                context.getModel().renderGroup(getTireObjectName(), wheelTextureId, context.isUseVanillaRender());
            }
        }
    }

    /**
     * If the wheel has a mudguard, it will be rendered, and the wheel will be a child of this node (in a {@link PartAttachedWheelNode}). <br>
     * Otherwise, the wheel will be rendered in this node.
     */
    class PartBaseWheelNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        private final boolean isMudGuard;

        public PartBaseWheelNode(PartWheel wheel, Vector3f scale, List<SceneGraph<T, A>> linkedChilds, boolean isMudGuard) {
            super(isMudGuard ? wheel.getRotationPoint() : wheel.getPosition(), GlQuaternionPool.newGlQuaternion(wheel.getSuspensionAxis()), PartWheel.this.isAutomaticPosition, scale, linkedChilds);
            this.isMudGuard = isMudGuard;
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            WheelsModule wheelsModule = entity != null ? entity.getModuleByType(WheelsModule.class) : null;
            boolean hasWheelsModule = wheelsModule != null;
            if (!isMudGuard && hasWheelsModule && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.REMOVED)
                return;
            PartWheelInfo info = hasWheelsModule ? wheelsModule.getWheelInfo(getId()) : getDefaultWheelInfo();
            if (!info.isModelValid() && getRimObjectName() == null && !isMudGuard)
                return;
            GlStateManager.pushMatrix();
            transformToRotationPoint();
            int index;
            if (hasWheelsModule) {
                /* Steering rotation*/
                if (isWheelIsSteerable()) {
                    index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
                    GlStateManager.rotate((wheelsModule.prevVisualProperties[index] + (wheelsModule.visualProperties[index] - wheelsModule.prevVisualProperties[index]) * context.getPartialTicks()), 0.0F, 1.0F, 0.0F);
                }
                /* Suspension translation */
                index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.SUSPENSION_LENGTH);
                GlStateManager.translate(0, -(wheelsModule.prevVisualProperties[index] + (wheelsModule.visualProperties[index] - wheelsModule.prevVisualProperties[index]) * context.getPartialTicks())
                        - info.getSuspensionRestLength(), 0);
                /* Wheel rotation */
                if (!isMudGuard) {
                    applyWheelRotation(context, info, wheelsModule);
                }
            }
            transformToPartPos();
            /* Render node */
            if (isMudGuard)
                context.getModel().renderGroup(getMudGuardObjectName(), context.getTextureId(), context.isUseVanillaRender());
            else
                renderWheel(entity, context, packInfo, info, wheelsModule);
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (!isMudGuard && DynamXDebugOptions.WHEELS.isActive()) {
                GlStateManager.pushMatrix();
                DynamXRenderUtils.glTranslate(getPosition());
                AxisAlignedBB box = getBox();
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        isDrivingWheel() ? 0 : 1, isDrivingWheel() ? 1 : 0, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }

    /**
     * Wheel rendered as a child of {@link PartBaseWheelNode} (when there is a mudguard)
     */
    class PartAttachedWheelNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        public PartAttachedWheelNode(PartWheel wheel, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(PartWheel.this.isAutomaticPosition ? wheel.getPosition() : new Vector3f(wheel.getPosition().subtract(wheel.getRotationPoint())), null, PartWheel.this.isAutomaticPosition, scale, linkedChilds);
            if (wheel.getSuspensionAxis() != null && !isAutomaticPosition) //Note that we have the mudguard translation and rotation applied, so the translation must "anticipate" this rotation.
                //Formula: translation = (wheelPos - mudGuardPos) * inverse(suspensionRotation) where mudGuardPos and suspensionRotation are applied in the previous node
                translation.set(DynamXGeometry.rotateVectorByQuaternion(translation, wheel.getSuspensionAxis().inverse()));
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            WheelsModule wheelsModule = entity != null ? entity.getModuleByType(WheelsModule.class) : null;
            boolean hasWheelsModule = wheelsModule != null;
            if (hasWheelsModule && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.REMOVED)
                return;
            PartWheelInfo info = hasWheelsModule ? wheelsModule.getWheelInfo(getId()) : getDefaultWheelInfo();
            if (!info.isModelValid() && getRimObjectName() == null)
                return;
            GlStateManager.pushMatrix();
            transformToRotationPoint();
            if (hasWheelsModule) {
                applyWheelRotation(context, info, wheelsModule);
            }
            transformToPartPos();
            renderWheel(entity, context, packInfo, info, wheelsModule);
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (DynamXDebugOptions.WHEELS.isActive()) {
                GlStateManager.pushMatrix();
                DynamXRenderUtils.glTranslate(getPosition());
                AxisAlignedBB box = getBox();
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        isDrivingWheel() ? 0 : 1, isDrivingWheel() ? 1 : 0, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
