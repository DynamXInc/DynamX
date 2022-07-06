package fr.dynamx.common.network.udp;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class UdpTestPacket extends UDPPacket
{
    private int id;
    private long sendTime, rcvTime;
    private String sample;
    public UdpTestPacket(int id, long sendTime)
    {
        this.id = id;
        this.sample = "By AymericRed";
        this.sendTime = sendTime;
        this.rcvTime = -1;
    }
    public UdpTestPacket(int id, String sample, long sendTime, long rcvTime)
    {
        System.out.println("RCV "+id);
        this.id = id;
        this.sample = sample;
        this.sendTime = sendTime;
        this.rcvTime = rcvTime;
    }

    @Override
    public byte id() {
        return 9;
    }

    @Override
    public void write(ByteBuf var1) {
        //System.out.println("Writing "+id);
        var1.writeInt(id);
        ByteBufUtils.writeUTF8String(var1, sample);
        var1.writeLong(sendTime);
        var1.writeLong(rcvTime);
    }
}
