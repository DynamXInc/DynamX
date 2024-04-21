package fr.dynamx.common.network.lights;

import fr.dynamx.common.capability.itemdata.DynamXItemData;
import fr.dynamx.common.capability.itemdata.DynamXItemDataProvider;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.items.DynamXItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class PacketSyncItemLight implements IMessage {

    private UUID id;
    private int playerId = -1;
    private boolean state;

    public PacketSyncItemLight() {
    }

    public PacketSyncItemLight(UUID id, Entity entity) {
        this.id = id;
        this.playerId = entity.getEntityId();
    }

    public PacketSyncItemLight(UUID id, boolean state) {
        this.id = id;
        this.state = state;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        playerId = buf.readInt();
        state = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, id.toString());
        buf.writeInt(playerId);
        buf.writeBoolean(state);
    }

    public static class ClientHandler implements IMessageHandler<PacketSyncItemLight, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncItemLight message, MessageContext ctx) {
            Entity entityByID = Minecraft.getMinecraft().world.getEntityByID(message.playerId);
            AbstractLightsModule.ItemLightsModule module = DynamXItemData.itemInstanceLights.get(message.id);
            if(entityByID instanceof EntityPlayer){
                ItemStack heldItem = ((EntityPlayer) entityByID).getHeldItem(EnumHand.MAIN_HAND);
                DynamXItemData capability = heldItem.getCapability(DynamXItemDataProvider.DYNAMX_ITEM_DATA_CAPABILITY, null);
                if (capability == null) {
                    return null;
                }
                DynamXItem<?> dxItem = (DynamXItem<?>) heldItem.getItem();
                capability.itemModule = new AbstractLightsModule.ItemLightsModule(dxItem, dxItem.getInfo(), message.id, entityByID);
                DynamXItemData.itemInstanceLights.put(message.id, capability.itemModule);
            }else if(module != null){
                DynamXItemData.setLightOn(module, message.state);
            }
            return null;
        }
    }
}
