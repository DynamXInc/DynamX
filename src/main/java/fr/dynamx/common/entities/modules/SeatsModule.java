package fr.dynamx.common.entities.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.client.camera.CameraMode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

import static fr.dynamx.common.DynamXMain.log;

public class SeatsModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>> {
    protected final PackPhysicsEntity<?, ? extends IPartContainer<?>> entity;
    protected BiMap<BasePartSeat, Entity> seatToPassenger = HashBiMap.create();
    protected Map<Byte, Boolean> doorsStatus;
    @Getter
    protected final CameraMode preferredCameraMode;
    @Getter
    private BasePartSeat lastRiddenSeat;


    public SeatsModule(PackPhysicsEntity<?, ? extends IPartContainer<?>> entity) {
        this(entity, CameraMode.AUTO);
    }

    public SeatsModule(PackPhysicsEntity<?, ? extends IPartContainer<?>> entity, CameraMode preferredCameraMode) {
        this.entity = entity;
        this.preferredCameraMode = preferredCameraMode;
    }

    @Override
    public void initEntityProperties() {
        for (BasePartSeat s : (List<BasePartSeat>) entity.getPackInfo().getPartsByType(BasePartSeat.class)) {
            if (s.hasDoor()) {
                if (doorsStatus == null)
                    doorsStatus = new HashMap<>();
                doorsStatus.put(s.getId(), false);
            }
        }
    }

    public boolean isEntitySitting(Entity entity) {
        return seatToPassenger.containsValue(entity);
    }

    public BasePartSeat getRidingSeat(Entity entity) {
        return seatToPassenger.inverse().get(entity);
    }

    @SideOnly(Side.CLIENT)
    public boolean isLocalPlayerDriving() {
        BasePartSeat seat = getRidingSeat(Minecraft.getMinecraft().player);
        return seat != null && seat.isDriver();
    }


    public BiMap<BasePartSeat, Entity> getSeatToPassengerMap() {
        return seatToPassenger;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        seatToPassenger.forEach((s, p) -> tag.setString("Seat" + s.getId(), p.getUniqueID().toString()));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        for (BasePartSeat seat : (List<BasePartSeat>) entity.getPackInfo().getPartsByType(BasePartSeat.class)) {
            if (tag.hasKey("Seat" + seat.getId(), Constants.NBT.TAG_STRING)) {
                EntityPlayer player = entity.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(tag.getString("Seat" + seat.getId())));
                if (player != null) {
                    seatToPassenger.put(seat, player);
                }
            }
        }
    }

    @Nullable
    public Entity getControllingPassenger() {
        return seatToPassenger.entrySet().stream().filter(e -> e.getKey().isDriver()).findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public void updatePassenger(Entity passenger) {
        BasePartSeat seat = getRidingSeat(passenger);
        if (seat == null) {
            return;
        }
        Vector3fPool.openPool();
        Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), entity.renderRotation);//PhysicsHelper.getRotatedPoint(seat.getPosition(), -this.rotationPitch, this.rotationYaw, this.rotationRoll);
        passenger.setPosition(entity.posX + posVec.x, entity.posY + posVec.y, entity.posZ + posVec.z);
        Vector3fPool.closePool();
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    public void applyOrientationToEntity(Entity passenger) {
        BasePartSeat seat = getRidingSeat(passenger);
        if (seat != null && seat.shouldLimitFieldOfView()) {
            float f = MathHelper.wrapDegrees(passenger.rotationYaw);
            float f1 = MathHelper.clamp(f, seat.getMaxYaw(), seat.getMinYaw());
            passenger.rotationYaw = f1;
            f = MathHelper.wrapDegrees(passenger.prevRotationYaw);
            f1 = MathHelper.clamp(f, seat.getMaxYaw(), seat.getMinYaw());
            passenger.prevRotationYaw = f1;

            float f2 = MathHelper.wrapDegrees(passenger.rotationPitch);
            float f3 = MathHelper.clamp(f2, seat.getMaxPitch(), seat.getMinPitch());
            passenger.rotationPitch = f3;
            f2 = MathHelper.wrapDegrees(passenger.prevRotationPitch);
            f3 = MathHelper.clamp(f2, seat.getMaxPitch(), seat.getMinPitch());
            passenger.prevRotationPitch = f3;
        }
    }

    @Override
    public void addPassenger(Entity passenger) {
        if (entity.world.isRemote) {
            return;
        }
        BasePartSeat hitPart = seatToPassenger.inverse().get(passenger);
        if (hitPart != null) {
            if (hitPart.isDriver() && passenger instanceof EntityPlayer) {
                entity.getSynchronizer().onPlayerStartControlling((EntityPlayer) passenger, true);
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.EntityMount(Side.SERVER, passenger, entity, this, hitPart));
            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
        } else {
            log.error("Cannot add passenger : " + passenger + " : seat not found !");
        }
        //Client side is managed by updateSeats
    }

    @Override
    public void removePassenger(Entity passenger) {
        BasePartSeat seat = getRidingSeat(passenger);
        if (entity.world.isRemote || seat == null) {
            return;
        }
        lastRiddenSeat = seat;
        seatToPassenger.remove(seat);
        if (seat.isDriver() && passenger instanceof EntityPlayer) {
            entity.getSynchronizer().onPlayerStopControlling((EntityPlayer) passenger, true);
        }
        DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.EntityDismount(entity.world.isRemote ? Side.CLIENT : Side.SERVER, passenger, entity, this, seat));
        //Client side is managed by updateSeats
    }

    public void updateSeats(MessageSeatsSync msg, PhysicsEntitySynchronizer<?> netHandler) {
        List<BasePartSeat> remove = new ArrayList<>(0);
        //Search for players who dismounted the entity
        for (Map.Entry<BasePartSeat, Entity> entity : seatToPassenger.entrySet()) {
            if (msg.getSeatToEntity().containsValue(entity.getValue().getEntityId())) {
                continue;
            }
            remove.add(entity.getKey());
            if (entity.getKey().isDriver() && entity.getValue() instanceof EntityPlayer) {
                netHandler.onPlayerStopControlling((EntityPlayer) entity.getValue(), true);
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.EntityDismount(Side.CLIENT, entity.getValue(), this.entity, this, entity.getKey()));
        }
        //And remove them
        if (!remove.isEmpty())
            remove.forEach(seatToPassenger::remove);
        //Search for players who mounted on the entity
        if (entity.getPackInfo() == null) //The seats may be sync before the entity's vehicle info is initialized
        {
            PackPhysicsEntity notGeneric = entity;
            notGeneric.setPackInfo(notGeneric.createInfo(entity.getInfoName()));
            if (entity.getPackInfo() == null)
                log.fatal("Failed to find info " + entity.getInfoName() + " for modular entity seats sync. Entity : " + entity);
        }
        for (Map.Entry<Byte, Integer> e : msg.getSeatToEntity().entrySet()) {
            BasePartSeat seat = entity.getPackInfo().getPartByTypeAndId(BasePartSeat.class, e.getKey());
            if (seat != null) {
                Entity passengerEntity = entity.world.getEntityByID(e.getValue());
                if (passengerEntity != null) {
                    if (seatToPassenger.get(seat) != passengerEntity) { //And add them
                        seatToPassenger.put(seat, passengerEntity);
                        if (seat.isDriver() && passengerEntity instanceof EntityPlayer) {
                            netHandler.onPlayerStartControlling((EntityPlayer) passengerEntity, true);
                        }
                        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.EntityMount(Side.CLIENT, passengerEntity, entity, this, seat));
                    }
                } else {
                    log.warn("Entity with id " + e.getValue() + " not found for seat in " + entity);
                }
            } else {
                log.warn("Seat with id " + e.getKey() + " not found in " + entity);
            }
        }
    }
}
