package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class MessageSeatsSync extends PhysicsEntityMessage<MessageSeatsSync> {
    private final Map<Byte, Integer> seatToEntity = new HashMap<>();

    public MessageSeatsSync() {
        super(null);
    }

    public MessageSeatsSync(IModuleContainer.ISeatsContainer vehicleEntity) {
        super(vehicleEntity.cast());
        for (Map.Entry<PartSeat, Entity> e : vehicleEntity.getSeats().getSeatToPassengerMap().entrySet()) {
            seatToEntity.put(e.getKey().getId(), e.getValue().getEntityId());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        int size = buf.readInt();
        for (int i = 0; i < size; i++)
            seatToEntity.put(buf.readByte(), buf.readInt());
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        if (entity instanceof IModuleContainer.ISeatsContainer)
            ((IModuleContainer.ISeatsContainer) entity).getSeats().updateSeats((MessageSeatsSync) message, entity.getSynchronizer());
        else
            log.fatal("Received seats packet for an entity that have no seats !");
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        throw new IllegalStateException();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(seatToEntity.size());
        for (Map.Entry<Byte, Integer> e : seatToEntity.entrySet()) {
            buf.writeByte(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    public Map<Byte, Integer> getSeatToEntity() {
        return seatToEntity;
    }
}
