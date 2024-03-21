package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

public class TrailerEntity<T extends TrailerEntity.TrailerPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.IDoorContainer, IModuleContainer.ISeatsContainer {
    private WheelsModule wheels;
    private DoorsModule doors;
    private SeatsModule seats;

    public TrailerEntity(World world) {
        super(world);
    }

    public TrailerEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new TrailerPhysicsHandler(this);
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
        wheels = getModuleByType(WheelsModule.class);
        doors = getModuleByType(DoorsModule.class);
    }

    @Nonnull
    public WheelsModule getWheels() {
        return wheels;
    }

    @Override
    public PackPhysicsEntity<?, ?> cast() {
        return this;
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.TRAILERS.findInfo(infoName);
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        return seats;
    }

    @SideOnly(Side.CLIENT)
    public void playAttachSound() {
        TrailerAttachInfo info = getPackInfo().getSubPropertyByType(TrailerAttachInfo.class);
        if(info.getAttachSound() != null)
            SOUND_HANDLER.playSingleSound(physicsPosition, info.getAttachSound(), 1, 1);
    }

    public static class TrailerPhysicsHandler<A extends TrailerEntity<?>> extends BaseWheeledVehiclePhysicsHandler<A> {
        public TrailerPhysicsHandler(A entity) {
            super(entity);
        }

        public WheelsPhysicsHandler getWheels() {
            return getHandledEntity().getWheels().getPhysicsHandler(); //WHEELS
        }
    }
}
