package fr.dynamx.client.network.udp;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.CommandUdp;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.UPDByteArrayPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.concurrent.LinkedBlockingQueue;

public class UDPClientHandler implements Runnable {
    final LinkedBlockingQueue<byte[]> packetQueue;
    private final UdpClientNetworkHandler client;
    private final long startTime;

    UDPClientHandler(UdpClientNetworkHandler client) {
        this.client = client;
        this.packetQueue = new LinkedBlockingQueue<>();
        this.startTime = System.currentTimeMillis();
    }

    private void handleAuthComplete() {
        this.client.handleAuth();
    }

    public void read(byte[] data) {
        ByteBuf in = Unpooled.wrappedBuffer(data);
        byte id = in.readByte();

        if (id == 0)
            this.handleAuthComplete();
        else if (id == 9) {
            int testId = in.readInt();
            String sample = ByteBufUtils.readUTF8String(in);
            long sentTime = in.readLong() - CommandUdp.startTime, rcvTime = in.readLong() - CommandUdp.startTime, clientRcv = System.currentTimeMillis() - CommandUdp.startTime;
            CommandUdp.received[testId] = true;
            System.out.println("Packet " + testId + " sent at " + sentTime + " received on srv at " + rcvTime + " on client at " + clientRcv);
        } else {
            if (id >= 10) {
                if (Minecraft.getMinecraft().player != null)
                    EncapsulatedUDPPacket.readAndHandle(id, in, Minecraft.getMinecraft().player);
            } else
                throw new IllegalArgumentException("Illegal dynamx packet id " + id);
        }
        UPDByteArrayPool.getINSTANCE().free(data);
    }

    @Override
    public void run() {
        while (UdpClientNetworkHandler.running) {
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("Looping to handle " + packetQueue);
            if (!this.packetQueue.isEmpty()) {
                try {
                    this.read(this.packetQueue.poll());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                synchronized (this) {
                    try {
                        if (client.isAuthed())
                            this.wait(1000);
                        else
                            this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!client.isAuthed() && (System.currentTimeMillis() - startTime) > 25000) {
                DynamXMain.log.warn("Failed to establish an UDP connection : timed out (0x2)");
                Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(new TextComponentString("DynamX UDP connection timed out (Auth started)"));
            }
        }
    }
}