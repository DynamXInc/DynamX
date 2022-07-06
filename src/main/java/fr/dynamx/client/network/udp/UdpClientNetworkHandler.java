package fr.dynamx.client.network.udp;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.sync.MessagePacksHashs;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.common.network.udp.UDPPacket;
import fr.dynamx.common.network.udp.auth.UDPClientAuthenticationPacket;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.UPDByteArrayPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpClientNetworkHandler implements IDnxNetworkHandler
{
    private boolean authed;
    private final String hash;

    public final String getHash() {
        return this.hash;
    }
    public boolean isAuthed() {
        return this.authed;
    }
    void setAuthed(boolean authed) {
        this.authed = authed;
    }

    public static volatile boolean running;
    private final int port;
    private final String host;
    private UDPClientHandler handler;
    private DatagramSocket datagramSocket;
    private InetSocketAddress address;

    public UdpClientNetworkHandler(String hash, String host, int udpPort) {
        this.hash = hash;
        this.port = udpPort;
        this.host = host;
    }

    public void authenticate() {
        this.setAuthed(false);
        this.sendPacket(new UDPClientAuthenticationPacket(hash));
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Auth rq sent");
    }

    void handleAuth() {
        DynamXMain.log.info("Successfully authenticated with udp server.");
        this.setAuthed(true);

        if(DynamXConfig.syncPacks) {
            DynamXMain.log.debug("Requesting pack sync...");
            DynamXContext.getNetwork().sendToServer(new MessagePacksHashs(PackSyncHandler.getObjects()));
        }
    }

    private byte warningThreshold;

    public void sendPacket(UDPPacket packet) {
        if (datagramSocket != null && !this.datagramSocket.isClosed()) {
            ByteBuf packetBuffer = Unpooled.buffer();
            packetBuffer.writeByte(packet.id());
            packet.write(packetBuffer);
            byte[] data = packetBuffer.array();

            if(DynamXConfig.udpDebug)
                DynamXMain.log.info("Send packet with size "+data.length);
            if(data.length > 512) { //512 is reasonable when sent from client
                if(warningThreshold == 0)
                    DynamXMain.log.warn("[UDP] Packet with id " + packet.id() + " is too large, reduce the amount of data to 512 bytes at max !");
                warningThreshold++;
                if(warningThreshold > 40)
                    warningThreshold = 0;
            }
            try {
                this.datagramSocket.send(new DatagramPacket(data, data.length, this.address));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            DynamXMain.log.error("Cannot send packet : socket closed");
    }

    @Override
    public boolean start() {
        running = true;
        this.address = new InetSocketAddress(this.host, this.port);

        try {
            this.datagramSocket = new DatagramSocket();
            this.datagramSocket.setSoTimeout(0);
            this.datagramSocket.connect(this.address);
            (new Thread(this.handler = new UDPClientHandler(this), "DynamX UDP Client Handler")).start();
        } catch (SocketException e) {
            running = false;
            e.printStackTrace();
        }

        DynamXMain.log.info("Connected to UDP[" + this.host + ":" + this.port + "] server, requesting authentication.");
        this.authenticate();

        while (running) {
            byte[] packetBuffer = UPDByteArrayPool.getINSTANCE().get(); //Note that the array is not cleaned and may contain old data
            DatagramPacket p = new DatagramPacket(packetBuffer, packetBuffer.length);

            try {
                if(DynamXConfig.udpDebug)
                    DynamXMain.log.info("Receiving...");
                this.datagramSocket.receive(p);
                if(DynamXConfig.udpDebug)
                    DynamXMain.log.info("Received length "+p.getLength());
                this.handler.packetQueue.offer(p.getData());

                synchronized (this.handler) {
                    this.handler.notify();
                    if(DynamXConfig.udpDebug)
                        DynamXMain.log.info("Notified handler");
                }
            } catch (SocketException e) {
                if(e.getMessage().contains("socket closed"))
                    DynamXMain.log.warn("UDP socket closed unexpectedly");
                else
                    DynamXMain.log.fatal("UDP connection exception", e);
                running = false;
            } catch (IOException e) {
                DynamXMain.log.fatal("UDP connection exception", e);
                running = false;
            }
        }
        DynamXMain.log.info("UDP connection closed");
        return true;
    }

    @Override
    public void stop() {
        running = false;

        if (this.datagramSocket != null)
            this.datagramSocket.close();
    }

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if(targetType == EnumPacketTarget.SERVER)
        {
            //System.out.println("Sending packet "+packet);
            sendPacket(new EncapsulatedUDPPacket(packet));
        }
        else
            throw new IllegalArgumentException("Cannot send a packet to another client, from a client !");
    }

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.DYNAMX_UDP;
    }
}