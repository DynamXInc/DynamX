package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartBlockSeat<T extends ISubInfoTypeOwner<T>> extends BasePartSeat<Entity, T> {
    public PartBlockSeat(T owner, String partName) {
        super(owner, partName);
    }

    @Override
    public boolean interact(Entity entity, EntityPlayer with) {
        if (entity instanceof SeatEntity)
            return with.startRiding(entity);
        else if (entity instanceof PropsEntity) {
            PropsEntity<?> vehicleEntity = (PropsEntity<?>) entity;
            SeatsModule seats = ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
            Entity seatRider = seats.getSeatToPassengerMap().get(this);
            if (seatRider != null) {
                if (seatRider != with) {
                    with.sendMessage(new TextComponentString("The seat is already taken"));
                    return false;
                }
            }
            return mount(vehicleEntity, seats, with);
        } else {
            return false;
        }
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + entity + " has PartSeats, but does not implement IHaveSeats !");
        if (!modules.hasModuleOfClass(SeatsModule.class))
            modules.add(new SeatsModule(entity));
    }
}
