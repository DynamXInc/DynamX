package fr.dynamx.common.entities.modules;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import lombok.Getter;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

public class HelicopterRotorModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    private HelicopterEngineModule engine;

    @Getter
    private float curPower, curAngle;

    public HelicopterRotorModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
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
            int height = entity.getPosition().getY() - entity.world.getHeight(entity.getPosition().getX(), entity.getPosition().getZ());
            if (height < 10) {
                renderParticles(entity, height);
            }
        }
    }

    private void renderParticles(BaseVehicleEntity<?> entity, int height) {
        World world = entity.world;
        for (int i = 0; i < 360; i += 2) {
            int power = (int) (engine.getPower() * 10);

            if (world.rand.nextInt(100) < power) {
                float minRadius = 5.5f - height * 0.5f;
                float radius = world.rand.nextFloat() * 4;

                double x = Math.cos(Math.toRadians(i)) * (minRadius + radius);
                double z = Math.sin(Math.toRadians(i)) * (minRadius + radius);

                double y = world.getHeight((int) (entity.getPosition().getX() + x), (int) (entity.getPosition().getZ() + z));
                double zSpeed = Math.sin(Math.toRadians(i)) * 0.9;
                double xSpeed = Math.cos(Math.toRadians(i)) * 0.9;

                if (world.isAirBlock(new BlockPos((int) (entity.getPosition().getX() + x), (int) (entity.getPosition().getY() + y), (int) (entity.getPosition().getZ() + z)))) {
                    world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX + x, y, entity.posZ + z, xSpeed, 0, zSpeed);
                }
            }
        }
    }
}
