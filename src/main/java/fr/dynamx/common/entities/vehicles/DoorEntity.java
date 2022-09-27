package fr.dynamx.common.entities.vehicles;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

public class DoorEntity<T extends PackEntityPhysicsHandler<PartDoor, ?>> extends PackPhysicsEntity<T, PartDoor> {
    private static final DataParameter<Integer> VEHICLE_ID = EntityDataManager.createKey(DoorEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> DOOR_ID = EntityDataManager.createKey(DoorEntity.class, DataSerializers.BYTE);
    int timer = -1;
    private BaseVehicleEntity<?> vehicleEntity;
    private DoorsModule doorAttachModule;

    public DoorEntity(World world) {
        super(world);
    }

    public DoorEntity(BaseVehicleEntity<?> vehicleEntity, Vector3f pos, float spawnAngle, byte doorID) {
        super(vehicleEntity.getInfoName(), vehicleEntity.world, pos, spawnAngle, 0);
        setDoorID(doorID);
        setVehicleEntity(vehicleEntity);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(VEHICLE_ID, -1);
        this.getDataManager().register(DOOR_ID, (byte) -1);
    }

    @Override
    protected PartDoor createInfo(String infoName) {
        return getVehicleEntity(world) instanceof TrailerEntity
                ? DynamXObjectLoaders.TRAILERS.findInfo(infoName).getPartByTypeAndId(PartDoor.class, getDoorID())
                : DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(infoName).getPartByTypeAndId(PartDoor.class, getDoorID());

    }

    @Override
    public T createPhysicsHandler() {
        return (T) new DoorPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        if (vehicleEntity != null) {
            doorAttachModule = new DoorsModule(vehicleEntity);
            modules.add(doorAttachModule);
        }
        modules.add(new MovableModule(this));
        modules.add(jointsHandler = new EntityJointsHandler(this) {
            @Override
            public void onRemoveJoint(EntityJoint<?> joint) {
                super.onRemoveJoint(joint);
                if (vehicleEntity != null) {
                    //vehicleEntity.detachDoor(((DoorEntity<?>) getEntity()).getDoorID());
                    //getEntity().physicEntity.getRigidBody().removeFromIgnoreList(vehicleEntity.physicEntity.getRigidBody());
                }
            }
        });
    }

    @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateEntityModulesEvent<>(DoorEntity.class, this, moduleList, side));
    }

    @Override
    public void initPhysicsEntity(boolean usePhysics) {
        super.initPhysicsEntity(usePhysics);
        BaseVehicleEntity<?> car = getVehicleEntity(world);
        if (car != null) {
            /*DynamXMain.physicsWorld.schedule(() -> doorJoint =  DoorsModule.createP2PJoint(car, this, -1));
            if (car.isDoorAttached(this)) {
                physicEntity.getRigidBody().addToIgnoreList(car.physicEntity.getRigidBody());
            }*/
        }
    }

    @Override
    public int getSyncTickRate() {
        return 2;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setInteger("carID", getVehicleID());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        super.readEntityFromNBT(tagCompound);
        setVehicleID(tagCompound.getInteger("carID"));
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        List<MutableBoundingBox> list = new ArrayList<>();
        for (IShapeInfo partShape : getPackInfo().getShapes()) {
            list.add(new MutableBoundingBox(partShape.getSize()).offset(partShape.getPosition()).offset(physicsPosition));
        }
        return list;
    }

    public DoorsModule getDoorAttachModule() {
        return doorAttachModule;
    }

    public BaseVehicleEntity<?> getVehicleEntity(World world) {
        if (vehicleEntity == null) {
            if (getVehicleID() != -1) {
                Entity entity = world.getEntityByID(getVehicleID());
                if (entity instanceof BaseVehicleEntity) {
                    this.vehicleEntity = (BaseVehicleEntity<?>) entity;
                }
            }
        }
        return vehicleEntity;
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
        }
        if (timer != -1) {
            if (timer >= 0) --timer;
            else timer = -1;
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        if (vehicleEntity != null) {
            /*if (vehicleEntity.getAttachedDoors() != null) {
                vehicleEntity.detachDoor(getDoorID());
            }*/
        }
    }


    public void setVehicleEntity(BaseVehicleEntity<?> vehicleEntity) {
        this.vehicleEntity = vehicleEntity;
        setVehicleID(vehicleEntity.getEntityId());
    }

    public int getVehicleID() {
        return this.getDataManager().get(VEHICLE_ID);
    }

    private void setVehicleID(int name) {
        this.getDataManager().set(VEHICLE_ID, name);
    }

    public byte getDoorID() {
        return this.getDataManager().get(DOOR_ID);
    }

    private void setDoorID(byte id) {
        this.getDataManager().set(DOOR_ID, id);
    }

    public static class DoorPhysicsHandler<A extends DoorEntity<?>> extends PackEntityPhysicsHandler<PartDoor, A> {
        public DoorPhysicsHandler(A entity) {
            super(entity);
        }

        @Override
        public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
            return DynamXPhysicsHelper.fastCreateRigidBody(handledEntity, 50, new BoxCollisionShape(getHandledEntity().getPackInfo().getScale()), position, spawnRotation);
        }
    }
}
