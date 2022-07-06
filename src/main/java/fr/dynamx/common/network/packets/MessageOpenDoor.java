package fr.dynamx.common.network.packets;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageOpenDoor implements IDnxPacket, IMessageHandler<MessageOpenDoor, IMessage> {

    public int vehicleID;
    public boolean isDoorOpened;

    public MessageOpenDoor() {}

    public MessageOpenDoor(BaseVehicleEntity<?> vehicle, boolean isDoorOpened) {
        this.vehicleID = vehicle.getEntityId();
        this.isDoorOpened = isDoorOpened;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
        buf.writeBoolean(isDoorOpened);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
        isDoorOpened = buf.readBoolean();
    }

    @Override
    public IMessage onMessage(MessageOpenDoor message, MessageContext ctx) {
        BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) ctx.getServerHandler().player.getEntityWorld().getEntityByID(message.vehicleID);
        //System.out.println(vehicleEntity);
        if (vehicleEntity != null) {
            PartSeat seat = ((IModuleContainer.ISeatsContainer)vehicleEntity).getSeats().getRidingSeat(ctx.getServerHandler().player);
            ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().setDoorState(seat.getLinkedPartDoor(vehicleEntity).getId(), message.isDoorOpened);
        }
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

}
