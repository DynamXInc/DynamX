package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;

@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartEntitySeat extends BasePartSeat<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @Getter
    @Setter
    @PackFileProperty(configNames = "Driver")
    private boolean isDriver;

    @Getter
    @Setter
    @Nullable
    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    private String linkedDoor;

    public PartEntitySeat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
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

    public boolean hasDoor() {
        return getLinkedDoor() != null;
    }

    @Nullable
    public PartDoor getLinkedPartDoor(BaseVehicleEntity<?> vehicleEntity) {
        return getLinkedDoor() == null ? null : vehicleEntity.getPackInfo().getPartsByType(PartDoor.class).stream().filter(partDoor -> partDoor.getPartName().equals(getLinkedDoor()))
                .findFirst().orElse(null);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + entity + " has PartSeats, but does not implement IHaveSeats !");
        if (entity instanceof HelicopterEntity)
            return; //Helicopters have their own SeatsModule
        if (!modules.hasModuleOfClass(SeatsModule.class))
            modules.add(new SeatsModule(entity));
    }
}
