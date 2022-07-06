package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.movables.PickingObjectHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePickObject implements IDnxPacket, IMessageHandler<MessagePickObject, IDnxPacket> {
    private MovableModule.Action moduleAction;

    public MessagePickObject() {
    }

    public MessagePickObject(MovableModule.Action action) {
        this.moduleAction = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        MovableModule.Action action = new MovableModule.Action();
        action.setEnumAction(MovableModule.EnumAction.values()[buf.readInt()]);
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            if (action.getMovableAction() == MovableModule.EnumAction.TAKE || action.getMovableAction() == MovableModule.EnumAction.PICK || action.getMovableAction() == MovableModule.EnumAction.THROW) {
                action.setInfo(new Object[]{buf.readInt()});
            } else if (action.getMovableAction() == MovableModule.EnumAction.LENGTH_CHANGE) {
                action.setInfo(new Object[]{buf.readBoolean(), buf.readInt()});
            } else if (action.getMovableAction() == MovableModule.EnumAction.ATTACH_OBJECTS) {
                action.setInfo(new Object[]{buf.readBoolean()});
            }
        }
        this.moduleAction = action;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(moduleAction.getMovableAction().ordinal());
        buf.writeInt(moduleAction.getInfo().length);
        for (Object o : moduleAction.getInfo()) {
            if (o instanceof Boolean) {
                buf.writeBoolean((Boolean) o);
            } else if (o instanceof Integer) {
                buf.writeInt((Integer) o);
            } else if (o instanceof Float) {
                buf.writeFloat((Float) o);
            }
        }
    }

    @Override
    public IDnxPacket onMessage(MessagePickObject message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        DynamXContext.getPhysicsWorld().schedule(() -> PickingObjectHelper.handlePickingControl(message.moduleAction, player));
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
