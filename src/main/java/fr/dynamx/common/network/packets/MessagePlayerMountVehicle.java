package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.handlers.TaskScheduler;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
@AllArgsConstructor
public class MessagePlayerMountVehicle implements IDnxPacket, IMessageHandler<MessagePlayerMountVehicle, IDnxPacket> {
    private int entityID;
    private byte doorID;

    public MessagePlayerMountVehicle() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        doorID = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeByte(doorID);
    }

    @Override
    public IDnxPacket onMessage(MessagePlayerMountVehicle message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.server.addScheduledTask(() -> {
            Entity entity = player.world.getEntityByID(message.entityID);
            if (!(entity instanceof BaseVehicleEntity)) {
                return;
            }
            BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) entity;
            DoorsModule doorsModule = vehicleEntity.getModuleByType(DoorsModule.class);
            PartDoor partDoor = doorsModule.getPartDoor(message.doorID);
            if (partDoor.isPlayerMounting()) {
                return;
            }
            PartSeat seat = partDoor.getLinkedSeat(doorsModule.vehicleEntity);
            if (player.isSneaking() || seat == null) {
                DoorsModule.DoorState switchDoorState = doorsModule.isDoorOpened(message.doorID) ? DoorsModule.DoorState.CLOSE : DoorsModule.DoorState.OPEN;
                doorsModule.switchDoorState(message.doorID);
            } else {
                if (partDoor.isEnabled()) {
                    if (doorsModule.isDoorOpened(message.doorID)) {
                        return;
                    }
                    partDoor.isPlayerMounting = true;
                    doorsModule.setDoorState(message.doorID, DoorsModule.DoorState.OPEN);
                    TaskScheduler.schedule(new TaskScheduler.ScheduledTask(partDoor.getMountDelay()) {
                        @Override
                        public void run() {
                            partDoor.isPlayerMounting = false;
                            partDoor.mount(doorsModule.vehicleEntity, seat, player);
                        }
                    });
                } else {
                    partDoor.mount(doorsModule.vehicleEntity, seat, player);
                }
            }
        });
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
