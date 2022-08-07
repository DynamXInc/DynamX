package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;

public class PartSeat extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfoBuilder> {
    @PackFileProperty(configNames = "Driver")
    private boolean isDriver;
    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    private String linkedDoor;
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    private Quaternion rotation;

    @PackFileProperty(configNames = "PlayerPosition", required = false, defaultValue = "sit")
    private String playerPosition;

    @PackFileProperty(configNames = "CameraRotation", required = false, defaultValue = "0")
    private float rotationYaw;

    public PartSeat(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName, 0.4f, 1.8f);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder modulableVehicleInfo) {
        super.appendTo(modulableVehicleInfo);
        modulableVehicleInfo.arrangeSeatID(this);
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> vehicleEntity, EntityPlayer player) {
        if (!(vehicleEntity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + vehicleEntity + " has PartSeats, but does not implement IHaveSeats !");
        ISeatsModule seats = ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
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
                        if(((IModuleContainer.IDoorContainer) vehicleEntity).getDoors() == null)
                            return false;
                        if (!door.isEnabled() || ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().isDoorAttached(door.getId())) {
                            if (!door.isEnabled() || ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().isDoorOpened(door.getId())) {
                                boolean didMount = mount(vehicleEntity, seats, player);
                                if (didMount) {
                                    vehicleEntity.getModuleByType(DoorsModule.class).setDoorState(door.getId(), false);
                                }
                                return didMount;
                            } else {
                                return door.interact(vehicleEntity, player);
                            }
                        }
                    } else
                        DynamXMain.log.error("Cannot mount : door not attached : " + linkedDoor);
                } else
                    DynamXMain.log.error("Cannot mount : part door not found : " + linkedDoor);
            }
        } else {
            return mount(vehicleEntity, seats, player);
        }
        return false;
    }

    private boolean mount(BaseVehicleEntity<?> vehicleEntity, ISeatsModule seats, EntityPlayer player) {
        if (seats.getSeatToPassengerMap().containsValue(player)) {
            return false; //Player on another seat
        }
        seats.getSeatToPassengerMap().put(this, player);
        if (!player.startRiding(vehicleEntity, false)) //something went wrong : dismount
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

    public String getPlayerPosition() {
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
        return "PartSeat named " + getPartName() + " in " + getOwner().getName();
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
