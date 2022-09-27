package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class MessageSeatsSync extends PhysicsEntityMessage<MessageSeatsSync> {
    private final Map<Byte, Integer> seatToEntity = new HashMap<>();

    public MessageSeatsSync() {
        super(null);
    }

    public MessageSeatsSync(IModuleContainer.ISeatsContainer vehicleEntity) {
        super(vehicleEntity.cast());
        for (Map.Entry<PartSeat, EntityPlayer> e : vehicleEntity.getSeats().getSeatToPassengerMap().entrySet()) {
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
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(seatToEntity.size());
        for (Map.Entry<Byte, Integer> e : seatToEntity.entrySet()) {
            buf.writeByte(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    @Override
    public int getMessageId() {
        return 3;
    }

    public Map<Byte, Integer> getSeatToEntity() {
        return seatToEntity;
    }
}
