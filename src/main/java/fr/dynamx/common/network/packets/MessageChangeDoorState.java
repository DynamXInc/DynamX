package fr.dynamx.common.network.packets;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageChangeDoorState implements IDnxPacket, IMessageHandler<MessageChangeDoorState, IMessage> {

    public int vehicleID;
    public DoorsModule.DoorState doorState;
    public byte doorId;

    public MessageChangeDoorState() {}

    public MessageChangeDoorState(BaseVehicleEntity<?> vehicle, DoorsModule.DoorState doorState, byte doorId) {
        this.vehicleID = vehicle.getEntityId();
        this.doorState = doorState;
        this.doorId = doorId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
        buf.writeInt(doorState.ordinal());
        buf.writeByte(doorId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
        doorState = DoorsModule.DoorState.values()[buf.readInt()];
        doorId = buf.readByte();
    }

    @Override
    public IMessage onMessage(MessageChangeDoorState message, MessageContext ctx) {
        BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) ctx.getServerHandler().player.getEntityWorld().getEntityByID(message.vehicleID);
        //System.out.println(vehicleEntity);
        if (vehicleEntity != null) {
            IModuleContainer.ISeatsContainer seatsContainer = (IModuleContainer.ISeatsContainer) vehicleEntity;
            IModuleContainer.IDoorContainer doorContainer = (IModuleContainer.IDoorContainer) vehicleEntity;
            PartSeat seat = seatsContainer.getSeats().getRidingSeat(ctx.getServerHandler().player);
            if(seat == null)
                return null;
            if(seat.getLinkedPartDoor(vehicleEntity) == null){
                DynamXMain.log.warn("No linked door for seat " + seat.getPartName());
                return null;
            }
            doorContainer.getDoors().setDoorState(message.doorId != -1 ? message.doorId : seat.getLinkedPartDoor(vehicleEntity).getId(), message.doorState);
        }
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

}
