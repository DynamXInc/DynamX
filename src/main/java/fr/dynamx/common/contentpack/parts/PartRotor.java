package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.HelicopterRotorModule;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;

import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BOATS}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> implements IDrawablePart<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.BOATS, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "none")
    protected Quaternion rotation;
    @PackFileProperty(configNames = "RotationAxis", required = false, defaultValue = "0, 1, 0")
    protected Vector3f rotationAxis = new Vector3f(0, 1, 0);
    @PackFileProperty(configNames = "RotationSpeed", required = false, defaultValue = "0.0")
    protected float rotationSpeed = 15.0f;
    @PackFileProperty(configNames = "ObjectName")
    protected String objectName = "Rotor";
    /*@Getter
    @PackFileProperty(configNames = "Type", required = false, defaultValue = "PROPELLER")
    protected RotorType type = RotorType.PROPELLER;*/

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        Quaternion rot = readPositionFromModel(owner.getModel(), getObjectName(), true, rotation == null);
        if (rot != null)
            rotation = rot;
        super.appendTo(owner);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(HelicopterRotorModule.class) && entity instanceof HelicopterEntity) {
            modules.add(new HelicopterRotorModule((BaseVehicleEntity<?>) entity));
        }
    }

    public PartRotor(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.ROTORS;
    }

    @Override
    public String getName() {
        return "PartRotor named " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public SceneNode<IRenderContext, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, ModularVehicleInfo>> childGraph) {
        return new PartRotorNode<>(this, modelScale, childGraph);
    }

    public enum RotorType {
        PROPELLER,
        ALWAYS_ROTATING,
        ROTATING_WHEN_STARTED
    }

    class PartRotorNode<A extends ModularVehicleInfo> extends SimpleNode<IRenderContext, A> {
        public PartRotorNode(PartRotor part, Vector3f scale, List<SceneNode<IRenderContext, A>> linkedChilds) {
            super(part.getPosition(), GlQuaternionPool.newGlQuaternion(part.getRotation()), PartRotor.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(IRenderContext context, A packInfo) {
            DxModelRenderer vehicleModel = context.getModel();
            if (!vehicleModel.containsObjectOrNode(getObjectName()))
                return;
            transformToRotationPoint();
            ModularPhysicsEntity<?> entity = context instanceof BaseRenderContext.EntityRenderContext ? ((BaseRenderContext.EntityRenderContext) context).getEntity() : null;
            // Rotating the rotor.
            if (null == RotorType.ALWAYS_ROTATING) {
                //TODO
                transform.rotate(context.getPartialTicks() * getRotationSpeed() * DynamXMath.TO_RADIAN, getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
            } else if (entity != null) {
                if (null == RotorType.ROTATING_WHEN_STARTED) {
                    //TODO : check if the vehicle is started
                    // THEN ROTATE
                }
                if (entity.hasModuleOfType(HelicopterRotorModule.class)) {
                    HelicopterRotorModule partModule = entity.getModuleByType(HelicopterRotorModule.class);
                    float angle = (partModule.getCurAngle() + context.getPartialTicks() * partModule.getCurPower()) * getRotationSpeed();
                    transform.rotate(angle * DynamXMath.TO_RADIAN, getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                } else if (entity.hasModuleOfType(CarEngineModule.class)) {
                    CarEngineModule partModule = entity.getModuleByType(CarEngineModule.class);
                    float revs = partModule.getPhysicsHandler().getEngine().getRevs();
                    // GlStateManager.rotate((partModule.getCurAngle() + partialTicks * revs) * getRotationSpeed(), getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                } else if (entity.hasModuleOfType(BoatPropellerModule.class)) {
                    BoatPropellerModule partModule = entity.getModuleByType(BoatPropellerModule.class);
                    transform.translate(0f, -0.56152f, -2.6077f);
                    float angle = (partModule.getBladeAngle() + context.getPartialTicks() * partModule.getRevs()) * getRotationSpeed();
                    transform.rotate(angle, getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                    transform.translate(0f, 0.56152f, 2.6077f);
                }
            }
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            //Render it
            vehicleModel.renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(IRenderContext context, A packInfo) {
            if (DynamXDebugOptions.ROTORS.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                RenderGlobal.drawBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5,
                        204F / 255, 123F / 255, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(context, packInfo);
        }
    }
}
