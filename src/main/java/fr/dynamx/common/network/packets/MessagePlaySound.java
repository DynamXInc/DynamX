package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePlaySound implements IDnxPacket, IMessageHandler<MessagePlaySound, IDnxPacket> {

    private Vector3f pos;
    private float volume, pitch;

    public MessagePlaySound() {
    }

    public MessagePlaySound(Vector3f pos, float volume, float pitch) {
        this.pos = pos;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeVector3f(buf, pos);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = DynamXUtils.readVector3f(buf);
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public IDnxPacket onMessage(MessagePlaySound message, MessageContext ctx) {
        Minecraft.getMinecraft().player.world.playSound(message.pos.x, message.pos.y, message.pos.z, SoundEvents.BLOCK_ANVIL_HIT, SoundCategory.AMBIENT,
                message.volume, message.pitch, true);
        Minecraft.getMinecraft().player.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, message.pos.x, message.pos.y + 0.2, message.pos.z, 0, 0, 0);
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
