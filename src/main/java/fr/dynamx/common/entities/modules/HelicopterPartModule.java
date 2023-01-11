package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartHandle;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Basic wheel implementation <br>
 * Works with an {@link CarEngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
public class HelicopterPartModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>>, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    public int Dx;
    public int Dy;
    private HelicopterEngineModule engine;

    private float curPower, curAngle;

    public HelicopterPartModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    @Override
    public void initEntityProperties() {
        engine = entity.getModuleByType(HelicopterEngineModule.class);
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    public void updateEntity() {
        if (engine != null) {
            float targetPower = engine.getPower();
            curPower = curPower + (targetPower - curPower) / 60; //3-seconds interpolation
            curAngle += curPower;
        }
        if (entity.world.isRemote) {
            int height = (int) (entity.getPosition().getY() - entity.world.getHeight((int) entity.getPosition().getX(), (int) entity.getPosition().getZ()));
            if (height < 10) {
                renderParticles(entity,height);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
            this.entity.getPackInfo().getPartsByType(PartRotor.class).forEach(partRotor -> {
                renderRotor(render, partRotor, partialTicks, carEntity, vehicleModel);
            });
            //TODO: patch handle
            this.entity.getPackInfo().getPartsByType(PartHandle.class).forEach(partHandle -> {
                renderHandle(render, partHandle, partialTicks, carEntity, vehicleModel);
            });
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
        }
    }



    private void renderParticles( BaseVehicleEntity<?> carEntity, int height) {
        World world = carEntity.world;
        for (int i = 0; i < 360; i += 2) {
            int power = (int) (engine.getPower() * 10);

            if (world.rand.nextInt(100) < power) {

                float radius = world.rand.nextFloat() * 4;
                float minradius = 5.5f;
                minradius -= height * 0.5f;

                double x = Math.cos(Math.toRadians(i)) * (minradius + radius);
                double z = Math.sin(Math.toRadians(i)) * (minradius + radius);

                double y = world.getHeight((int) (carEntity.getPosition().getX() + x), (int) (carEntity.getPosition().getZ() + z));
                double zSpeed = Math.sin(Math.toRadians(i)) * 0.9;
                double xSpeed = Math.cos(Math.toRadians(i)) * 0.9;

                if (world.isAirBlock(new BlockPos((int) (carEntity.getPosition().getX() + x), (int) (carEntity.getPosition().getY() + y), (int) (carEntity.getPosition().getZ() + z)))) {
                    world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, carEntity.posX + x, y, carEntity.posZ + z, xSpeed, 0, zSpeed);
                }

            }

        }

    }

    @SideOnly(Side.CLIENT)
    private void renderRotor(RenderPhysicsEntity<?> render, PartRotor partRotor, float partialTicks, BaseVehicleEntity<?> helicopterEntity, ObjModelRenderer vehicleModel) {
        ObjObjectRenderer rotor = vehicleModel.getObjObjectRenderer(partRotor.getPartName());
        if (rotor != null) {
            if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
                GlStateManager.pushMatrix();
                Vector3f center = partRotor.getPosition();
                //Translation to the steering wheel rotation point (and render pos)
                GlStateManager.translate(center.x, center.y, center.z);
                // Rotating the rotor.
                GlStateManager.rotate((curAngle + partialTicks * curPower) * partRotor.getRotationSpeed(), partRotor.getRotation().x, partRotor.getRotation().y, partRotor.getRotation().z);
                //Scale it
                GlStateManager.scale(helicopterEntity.getPackInfo().getScaleModifier().x, helicopterEntity.getPackInfo().getScaleModifier().y, helicopterEntity.getPackInfo().getScaleModifier().z);
                //Render it
                vehicleModel.renderGroup(rotor, helicopterEntity.getEntityTextureID());
                GlStateManager.popMatrix();
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.ROTOR, (RenderBaseVehicle<?>) render, helicopterEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void renderHandle(RenderPhysicsEntity<?> render, PartHandle partHandle, float partialTicks, BaseVehicleEntity<?> carEntity, ObjModelRenderer vehicleModel) {
        ObjObjectRenderer handle = vehicleModel.getObjObjectRenderer(partHandle.getPartName());
        if (handle != null) {
            if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.HANDLE, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, vehicleModel))) {
                GlStateManager.pushMatrix();
                Vector3f center = partHandle.getPosition();
                //Translation to the steering wheel rotation point (and render pos)
                GlStateManager.translate(center.x, center.y, center.z);
                // Rotating the handle with Dx and Dy
                if (Dx > 0) {
                    GlStateManager.rotate((float) Dx, 0, 0, 0.5F);
                } else {
                    GlStateManager.rotate((float) -Dx, 0, 0, -0.5F);
                }
                if (Dy > 0) {
                    GlStateManager.rotate((float) Dy, 0.5F, 0, 0);
                } else {
                    GlStateManager.rotate((float) -Dy, -0.5F, 0, 0);
                }

                //Scale it
                GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                //Render it
                vehicleModel.renderGroup(handle, carEntity.getEntityTextureID());
                GlStateManager.popMatrix();
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.HANDLE, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, vehicleModel));
            }
        }
    }
}
