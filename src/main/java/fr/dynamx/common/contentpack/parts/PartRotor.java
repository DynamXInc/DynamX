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
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.HelicopterRotorModule;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BOATS}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.BOATS, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", true);
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
    public SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo>> childGraph) {
        return new PartRotorNode<>(this, modelScale, childGraph);
    }

    public enum RotorType {
        PROPELLER,
        ALWAYS_ROTATING,
        ROTATING_WHEN_STARTED
    }

    class PartRotorNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        public PartRotorNode(PartRotor part, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(part.getPosition(), GlQuaternionPool.newGlQuaternion(part.getRotation()), scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            DxModelRenderer vehicleModel = context.getModel();
            if (!vehicleModel.containsObjectOrNode(getObjectName()) || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) context.getRender(), entity, PhysicsEntityEvent.Phase.PRE, context.getPartialTicks(), vehicleModel)))
                return;
            GlStateManager.pushMatrix();
            transform();
            // Rotating the rotor.
            if (null == RotorType.ALWAYS_ROTATING) {
                //TODO
                GlStateManager.rotate(context.getPartialTicks() * getRotationSpeed(), getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
            } else if (entity != null) {
                if (null == RotorType.ROTATING_WHEN_STARTED) {
                    //TODO : check if the vehicle is started
                    // THEN ROTATE
                }
                if (entity.hasModuleOfType(HelicopterRotorModule.class)) {
                    HelicopterRotorModule partModule = entity.getModuleByType(HelicopterRotorModule.class);
                    GlStateManager.rotate((partModule.getCurAngle() + context.getPartialTicks() * partModule.getCurPower()) * getRotationSpeed(), getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                } else if (entity.hasModuleOfType(CarEngineModule.class)) {
                    CarEngineModule partModule = entity.getModuleByType(CarEngineModule.class);
                    float revs = partModule.getPhysicsHandler().getEngine().getRevs();
                    // GlStateManager.rotate((partModule.getCurAngle() + partialTicks * revs) * getRotationSpeed(), getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                } else if (entity.hasModuleOfType(BoatPropellerModule.class)) {
                    BoatPropellerModule partModule = entity.getModuleByType(BoatPropellerModule.class);
                    GlStateManager.translate(0, -0.56152, -2.6077);
                    GlStateManager.rotate((partModule.getBladeAngle() + context.getPartialTicks() * partModule.getRevs()) * getRotationSpeed(), getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                    GlStateManager.translate(0, 0.56152, 2.6077);
                }
            }
            //Scale it
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
            //Render it
            vehicleModel.renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
            GlStateManager.popMatrix();
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) context.getRender(), entity, PhysicsEntityEvent.Phase.POST, context.getPartialTicks(), vehicleModel));
        }
    }
}
