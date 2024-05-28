package fr.dynamx.server.network;

import fr.dynamx.api.network.*;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.VanillaNetworkHandler;
import fr.dynamx.server.network.udp.ServerIPAdressRetriever;
import fr.dynamx.server.network.udp.UdpServerConnectionHandler;
import fr.dynamx.server.network.udp.UdpServerNetworkHandler;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class DynamXServerNetworkSystem implements IDnxNetworkSystem {
    private final VanillaNetworkHandler VANILLA_NETWORK;
    private final IDnxNetworkHandler QUICK_NETWORK;
    private ServerIPAdressRetriever serverIPAdressRetriever;

    public DynamXServerNetworkSystem(EnumNetworkType networkType) {
        System.out.println("START NET OF TYPE " + networkType + " " + DynamXConfig.useUdp);
        VANILLA_NETWORK = new VanillaNetworkHandler();
        switch (networkType) {
            case VANILLA_TCP:
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            case DYNAMX_UDP:
                QUICK_NETWORK = new UdpServerNetworkHandler();
                break;
            default:
                throw new UnsupportedOperationException("Network type " + networkType + " isn't supported for the moment !");
        }
    }

    @Override
    public <T> void sendToClient(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        switch (packet.getPreferredNetwork()) {
            case VANILLA_TCP:
                VANILLA_NETWORK.sendPacket(packet, targetType, target);
                break;
            default:
                QUICK_NETWORK.sendPacket(packet, targetType, target);
        }
    }

    @Override
    public VanillaNetworkHandler getVanillaNetwork() {
        return VANILLA_NETWORK;
    }

    @Override
    public void startNetwork() {
        this.serverIPAdressRetriever = new ServerIPAdressRetriever();
        this.serverIPAdressRetriever.init();

        DynamXMain.log.info(VANILLA_NETWORK.start() ? "Started [" + VANILLA_NETWORK.getType() + "] Server." : "Failed to start [" + VANILLA_NETWORK.getType() + "] Server.");
        if (VANILLA_NETWORK != QUICK_NETWORK)
            DynamXMain.log.info(QUICK_NETWORK.start() ? "Started [" + QUICK_NETWORK.getType() + "] Server." : "Failed to start [" + QUICK_NETWORK.getType() + "] Server.");
        MinecraftForge.EVENT_BUS.register(new UdpServerConnectionHandler(QUICK_NETWORK));
    }

    @Override
    public void stopNetwork() {
        if (this.QUICK_NETWORK instanceof UdpServerNetworkHandler) {
            ((UdpServerNetworkHandler) this.QUICK_NETWORK).waitingAuth.clear();
        }

        VANILLA_NETWORK.stop();
        if (VANILLA_NETWORK != QUICK_NETWORK)
            QUICK_NETWORK.stop();
    }

    @Override
    public boolean isConnected() {
        return serverIPAdressRetriever != null;
    }

    @Override
    public IDnxNetworkHandler getQuickNetwork() {
        return QUICK_NETWORK;
    }

    public ServerIPAdressRetriever getServerIPAdressRetriever() {
        return this.serverIPAdressRetriever;
    }
}
