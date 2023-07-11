package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.HelicopterPartModule;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;


@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>>
{
    @Getter
    @PackFileProperty(configNames = "Rotation", required = false)
    private Vector3f rotation = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "RotationSpeed", required = false, defaultValue = "0.0")
    private float rotationSpeed = 15.0f;
    @Getter
    @PackFileProperty(configNames = "PartName")
    private String partName = "Rotor";


    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if(!modules.hasModuleOfClass(HelicopterPartModule.class)) {
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
    public void drawParts(@Nullable BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks, boolean forceVanillaRender) {
        DxModelRenderer vehicleModel = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        if (MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)))
            return;
        packInfo.getPartsByType(PartRotor.class).forEach(partRotor ->
                renderRotor(render, partRotor, partialTicks, entity, packInfo, textureId, vehicleModel)
        );
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public String[] getRenderedParts() {
        return new String[] {partName};
    }
    
    @SideOnly(Side.CLIENT)
    private void renderRotor(RenderPhysicsEntity<?> render, PartRotor partRotor, float partialTicks, BaseVehicleEntity<?> helicopterEntity, ModularVehicleInfo packInfo, byte textureId, DxModelRenderer vehicleModel) {
        if (!(vehicleModel instanceof ObjModelRenderer)) {
            return;
        }
        ObjObjectRenderer rotor = ((ObjModelRenderer) vehicleModel).getObjObjectRenderer(partRotor.getPartName());
        if(rotor == null || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)))
            return;
        GlStateManager.pushMatrix();
        Vector3f center = partRotor.getPosition();
        //Translation to the steering wheel rotation point (and render pos)
        GlStateManager.translate(center.x, center.y, center.z);
        if (helicopterEntity != null && helicopterEntity.hasModuleOfType(HelicopterPartModule.class)) {
            HelicopterPartModule partModule = helicopterEntity.getModuleByType(HelicopterPartModule.class);
            // Rotating the rotor.
            GlStateManager.rotate((partModule.getCurAngle() + partialTicks * partModule.getCurPower()) * partRotor.getRotationSpeed(), partRotor.getRotation().x, partRotor.getRotation().y, partRotor.getRotation().z);
        }
        //Scale it
        GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
        //Render it
        ((ObjModelRenderer) vehicleModel).renderGroup(rotor, textureId);
        GlStateManager.popMatrix();
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }
}
