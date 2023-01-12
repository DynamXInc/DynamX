package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;

@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartSeat extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @PackFileProperty(configNames = "Driver")
    private boolean isDriver;

    @PackFileProperty(configNames = "ShouldLimitFieldOfView", required = false, defaultValue = "true")
    @Accessors(fluent = true)
    @Getter
    private boolean shouldLimitFieldOfView = true;

    @PackFileProperty(configNames = "MaxYaw", required = false, defaultValue = "-105")
    @Getter
    private float maxYaw = -105.0f;

    @PackFileProperty(configNames = "MinYaw", required = false, defaultValue = "105")
    @Getter
    private float minYaw = 105.0f;

    @PackFileProperty(configNames = "MaxPitch", required = false, defaultValue = "-105")
    @Getter
    private float maxPitch = -105.0f;

    @PackFileProperty(configNames = "MinPitch", required = false, defaultValue = "105")
    @Getter
    private float minPitch = 105.0f;

    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    private String linkedDoor;
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    private Quaternion rotation;

    @PackFileProperty(configNames = "PlayerPosition", required = false, defaultValue = "SIT")
    private EnumSeatPlayerPosition playerPosition;

    @PackFileProperty(configNames = "CameraRotation", required = false, defaultValue = "0")
    private float rotationYaw;

    @PackFileProperty(configNames = "CameraPositionY", required = false, defaultValue = "0")
    @Getter
    private float cameraPositionY;

    @PackFileProperty(configNames = "PlayerSize", required = false, defaultValue = "1 1 1")
    @Getter
    private Vector3f playerSize;

    public PartSeat(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0.4f, 1.8f);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.SEATS_AND_STORAGE;
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        owner.arrangeSeatID(this);
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> vehicleEntity, EntityPlayer player) {
        if (!(vehicleEntity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + vehicleEntity + " has PartSeats, but does not implement IHaveSeats !");
        SeatsModule seats = ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
        Entity seatRider = seats.getSeatToPassengerMap().get(this);
        if (seatRider != null) {
            if (seatRider != player) {
                player.sendMessage(new TextComponentString("The seat is already taken"));
                return false;
            }
        }
        if (hasDoor()) {
            if (vehicleEntity instanceof CarEntity) {
                PartDoor door = getLinkedPartDoor(vehicleEntity);
                if (door != null) {
                    if (!door.isPlayerMounting()) {
                        IModuleContainer.IDoorContainer doorContainer = (IModuleContainer.IDoorContainer) vehicleEntity;
                        if (doorContainer.getDoors() == null)
                            return false;
                        if (!door.isEnabled() || doorContainer.getDoors().isDoorAttached(door.getId())) {
                            if (!door.isEnabled() || doorContainer.getDoors().isDoorOpened(door.getId())) {
                                boolean didMount = mount(vehicleEntity, seats, player);
                                if (didMount) {
                                    vehicleEntity.getModuleByType(DoorsModule.class).setDoorState(door.getId(), DoorsModule.DoorState.CLOSING);
                                }
                                return didMount;
                            } else {
                                return door.interact(vehicleEntity, player);
                            }
                        }
                    } //else
                        //DynamXMain.log.error("Cannot mount : player mounting : " + linkedDoor);
                } else
                    DynamXMain.log.error("Cannot mount : part door not found : " + linkedDoor);
            }
        } else {
            return mount(vehicleEntity, seats, player);
        }
        return false;
    }

    public boolean mount(BaseVehicleEntity<?> vehicleEntity, SeatsModule seats, Entity entity) {
        if (seats.getSeatToPassengerMap().containsValue(entity)) {
            return false; //Player on another seat
        }
        seats.getSeatToPassengerMap().put(this, entity);
        if (!entity.startRiding(vehicleEntity, false)) //something went wrong : dismount
        {
            seats.getSeatToPassengerMap().remove(this);
            return false;
        }
        return true;
    }

    public boolean hasDoor() {
        return getLinkedDoor() != null;
    }

    public boolean isDriver() {
        return isDriver;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public EnumSeatPlayerPosition getPlayerPosition() {
        return playerPosition;
    }

    public float getRotationYaw() {
        return rotationYaw;
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/seat.png");
    }

    @Override
    public String getName() {
        return "PartSeat_" + getPartName();
    }

    @Nullable
    public String getLinkedDoor() {
        return linkedDoor;
    }

    @Nullable
    public PartDoor getLinkedPartDoor(BaseVehicleEntity<?> vehicleEntity) {
        return getLinkedDoor() == null ? null : vehicleEntity.getPackInfo().getPartsByType(PartDoor.class).stream().filter(partDoor -> partDoor.getPartName().equals(getLinkedDoor()))
                .findFirst().orElse(null);
    }
}
