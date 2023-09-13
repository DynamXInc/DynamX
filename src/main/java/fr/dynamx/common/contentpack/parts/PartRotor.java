package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.BoatPropellerModule;
import fr.dynamx.common.entities.modules.CarEngineModule;
import fr.dynamx.common.entities.modules.HelicopterPartModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;


@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>> {
    @Getter
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "none")
    protected Quaternion rotation;
    @Getter
    @PackFileProperty(configNames = "RotationAxis", required = false, defaultValue = "0, 1, 0")
    protected Vector3f rotationAxis = new Vector3f(0, 1, 0);
    @Getter
    @PackFileProperty(configNames = "RotationSpeed", required = false, defaultValue = "0.0")
    protected float rotationSpeed = 15.0f;
    @Getter
    @PackFileProperty(configNames = "PartName")
    protected String partName = "Rotor";
    /*@Getter
    @PackFileProperty(configNames = "Type", required = false, defaultValue = "PROPELLER")
    protected RotorType type = RotorType.PROPELLER;*/

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(HelicopterPartModule.class)) {
            modules.add(new HelicopterPartModule((BaseVehicleEntity<?>) entity));
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
    public void drawParts(@Nullable BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks) {
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(packInfo.getModel());
        if (MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)))
            return;
        packInfo.getPartsByType(PartRotor.class).forEach(partRotor ->
                renderRotor(render, partRotor, partialTicks, entity, packInfo, textureId, vehicleModel)
        );
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public String[] getRenderedParts() {
        return new String[]{partName};
    }

    @SideOnly(Side.CLIENT)
    private void renderRotor(RenderPhysicsEntity<?> render, PartRotor partRotor, float partialTicks, BaseVehicleEntity<?> vehicleEntity, ModularVehicleInfo packInfo, byte textureId, ObjModelRenderer vehicleModel) {
        ObjObjectRenderer rotor = vehicleModel.getObjObjectRenderer(partRotor.getPartName());
        if (rotor == null || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, vehicleEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)))
            return;
        GlStateManager.pushMatrix();
        Vector3f center = partRotor.getPosition();
        //Translation to the steering wheel rotation point (and render pos)
        GlStateManager.translate(center.x, center.y, center.z);
        if (partRotor.getRotation() != null)
            GlStateManager.rotate(GlQuaternionPool.get(partRotor.getRotation()));
        // Rotating the rotor.
        if(null == RotorType.ALWAYS_ROTATING) {
            //TODO
            GlStateManager.rotate(partialTicks * partRotor.getRotationSpeed(), partRotor.getRotationAxis().x, partRotor.getRotationAxis().y, partRotor.getRotationAxis().z);
        } else if(vehicleEntity != null) {
            if (null == RotorType.ROTATING_WHEN_STARTED) {
                //TODO : check if the vehicle is started
                // THEN ROTATE
            }
            if (vehicleEntity.hasModuleOfType(HelicopterPartModule.class)) {
                HelicopterPartModule partModule = vehicleEntity.getModuleByType(HelicopterPartModule.class);
                GlStateManager.rotate((partModule.getCurAngle() + partialTicks * partModule.getCurPower()) * partRotor.getRotationSpeed(), partRotor.getRotationAxis().x, partRotor.getRotationAxis().y, partRotor.getRotationAxis().z);
            } else if(vehicleEntity.hasModuleOfType(CarEngineModule.class)) {
                CarEngineModule partModule = vehicleEntity.getModuleByType(CarEngineModule.class);
                float revs = partModule.getPhysicsHandler().getEngine().getRevs();
               // GlStateManager.rotate((partModule.getCurAngle() + partialTicks * revs) * partRotor.getRotationSpeed(), partRotor.getRotationAxis().x, partRotor.getRotationAxis().y, partRotor.getRotationAxis().z);
            } else if(vehicleEntity.hasModuleOfType(BoatPropellerModule.class)) {
                //TODO MOTEUR SUR BATEAUX ET SONS ET RPMS
            }
        }
        //Scale it
        GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
        //Render it
        vehicleModel.renderGroup(rotor, textureId);
        GlStateManager.popMatrix();
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, vehicleEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    public enum RotorType {
        PROPELLER,
        ALWAYS_ROTATING,
        ROTATING_WHEN_STARTED
    }
}
