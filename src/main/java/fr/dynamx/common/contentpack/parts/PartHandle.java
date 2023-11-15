package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;


@RegisteredSubInfoType(name = "handle", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartHandle extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>>
{
    @Getter
    @PackFileProperty(configNames = "PartName")
    private String partName = "handle";

    public PartHandle(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.HANDLES;
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
        packInfo.getPartsByType(PartHandle.class).forEach(partHandle ->
                renderHandle(render, partHandle, partialTicks, entity, packInfo, textureId, vehicleModel)
        );
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }

    @Override
    public String[] getRenderedParts() {
        return new String[] {partName};
    }

    @SideOnly(Side.CLIENT)
    private void renderHandle(RenderPhysicsEntity<?> render, PartHandle partHandle, float partialTicks, BaseVehicleEntity<?> carEntity, ModularVehicleInfo packInfo, byte textureId, ObjModelRenderer vehicleModel) {
        ObjObjectRenderer handle = vehicleModel.getObjObjectRenderer(partHandle.getPartName());
        if (handle == null || MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.HANDLE, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel)))
            return;
        GlStateManager.pushMatrix();
        Vector3f center = partHandle.getPosition();
        //Translation to the steering wheel rotation point (and render pos)
        GlStateManager.translate(center.x, center.y, center.z);
        if(carEntity != null && carEntity.hasModuleOfType(HelicopterEngineModule.class)) {
            HelicopterEngineModule engine = carEntity.getModuleByType(HelicopterEngineModule.class);
            // Rotating the handle with Dx and Dy
            float dx = engine.getRollControls().get(0);
            float dy = engine.getRollControls().get(1);
            GlStateManager.rotate(dx, 0, 0, dx > 0 ? 0.5f : -0.5f);
            GlStateManager.rotate(dy, dy > 0 ? 0.5f : -0.5f, 0, 0);
        }
        //Scale it
        GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
        //Render it
        vehicleModel.renderGroup(handle, textureId);
        GlStateManager.popMatrix();
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.HANDLE, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
    }
}
