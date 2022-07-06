package fr.dynamx.common.network.udp;

import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

public class EncapsulatedUDPPacket extends UDPPacket
{
    private final IDnxPacket message;

    public EncapsulatedUDPPacket(IDnxPacket packet) {
        this.message = packet;
    }

    @Override
    public byte id() {
        return (byte) (10+ DynamXNetwork.getUdpMessageId(message));
    }

    @Override
    public void write(ByteBuf bu) {
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Write packet "+message+" "+id());
        message.toBytes(bu);
    }

    public static void readAndHandle(byte id, ByteBuf data, EntityPlayer player)
    {
        IDnxPacket packet = DynamXNetwork.getUdpPacketById(id-10);
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Read packet "+packet+" "+id);
        packet.fromBytes(data);
        /*Thread t = new Thread(() ->
        {
            try {
                //System.out.println("Thread is " + Thread.currentThread().getName());
                Thread.sleep(55);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            packet.handleUDPReceive(player, player.world.isRemote ? Side.CLIENT : Side.SERVER);
        /*});
        t.setName("Lol"+player.rand.nextInt(10000));
        t.start();*/
    }
}
