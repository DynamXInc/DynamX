package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.*;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.*;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TrailerEntity<T extends TrailerEntity.TrailerPhysicsHandler<?>> extends BaseVehicleEntity<T> implements
        IModuleContainer.IPropulsionContainer<IPropulsionModule<BaseWheeledVehiclePhysicsHandler<?>>>, IModuleContainer.IDoorContainer, IMovableModuleContainer, IModuleContainer.ISeatsContainer
{
    private IPropulsionModule<BaseWheeledVehiclePhysicsHandler<?>> wheels;
    private DoorsModule doors;
    private ISeatsModule seats;

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
    public void createModules(ModuleListBuilder modules) {
        modules.add(seats = new SeatsModule(this));
        modules.add(wheels = new WheelsModule(this));
        super.createModules(modules);
        doors = getModuleByType(DoorsModule.class);
    }

    @Nonnull
    @Override
    public IPropulsionModule<BaseWheeledVehiclePhysicsHandler<?>> getPropulsion() {
        return wheels;
    }

    @Override
    public BaseVehicleEntity<?> cast() {
        return this;
    }

    @Override
    protected ModularVehicleInfo<?> createInfo(String infoName) {
        return DynamXObjectLoaders.TRAILERS.findInfo(infoName);
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public ISeatsModule getSeats() {
        if(seats == null) //We may need seats before modules are created, because of seats sync
            seats = new SeatsModule(this);
        return seats;
    }

    public static class TrailerPhysicsHandler<A extends TrailerEntity<?>> extends BaseWheeledVehiclePhysicsHandler<A>
    {
        public TrailerPhysicsHandler(A entity) {
            super(entity);
        }

        @Override
        public IPropulsionHandler getPropulsion() {
            return getHandledEntity().getPropulsion().getPhysicsHandler(); //WHEELS
        }
    }
}
