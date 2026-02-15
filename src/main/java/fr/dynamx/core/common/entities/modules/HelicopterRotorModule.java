package fr.dynamx.core.common.entities.modules;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.core.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.world.HmWorld;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

public class HelicopterRotorModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, IPhysicsModule.IModuleWithSpawnData {
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
    public boolean listenEntityUpdates(boolean isClient) {
        return isClient;
    }

    @Override
    public void updateEntity() {
        if (engine != null) {
            float targetPower = engine.getPower();
            curPower = curPower + (targetPower - curPower) / 60; //3-seconds interpolation
            curAngle += curPower;
        }
        if (entity.getWorld().hm$isClient()) {
            int height = (int) (entity.getMcEntity().hm$getPosY() - entity.getWorld().hm$getHeight((int) entity.getMcEntity().hm$getPosX(), (int) entity.getMcEntity().hm$getPosZ()));
            if (height < 10) {
                renderParticles(entity, height);
            }
        }
    }

    private void renderParticles(BaseVehicleEntity<?> entity, int height) {
        HmWorld world = entity.getWorld();
        HmEntity mcEntity = entity.getMcEntity();

        // TODO helicopter particle hermes
        /*for (int i = 0; i < 360; i += 2) {
            int power = (int) (engine.getPower() * 10);

            if (world.hm$getRandom().nextInt(100) < power) {
                float minRadius = 5.5f - height * 0.5f;
                float radius = world.hm$getRandom().nextFloat() * 4;

                double x = Math.cos(Math.toRadians(i)) * (minRadius + radius);
                double z = Math.sin(Math.toRadians(i)) * (minRadius + radius);

                double y = world.hm$getHeight((int) (mcEntity.hm$getPosX() + x), (int) (mcEntity.hm$getPosZ() + z));
                double zSpeed = Math.sin(Math.toRadians(i)) * 0.9;
                double xSpeed = Math.cos(Math.toRadians(i)) * 0.9;

                if (world.hm$isAirBlock((int) (mcEntity.hm$getPosX() + x), (int) (y), (int) (mcEntity.hm$getPosZ() + z))) {
                    //TODO PARTICLE world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.getPosX() + x, y, entity.getPosZ() + z, xSpeed, 0, zSpeed);
                }
            }
        }*/
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        // curPower isn't computed on server side
        buffer.writeFloat(engine != null ? engine.getPower() : 0);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        curPower = additionalData.readFloat();
    }
}
