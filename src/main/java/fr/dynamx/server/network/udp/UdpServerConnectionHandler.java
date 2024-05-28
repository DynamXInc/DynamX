package fr.dynamx.server.network.udp;

import com.mojang.authlib.GameProfile;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.auth.MessageDynamXUdpSettings;
import fr.dynamx.server.network.DynamXServerNetworkSystem;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UdpServerConnectionHandler {
    private final IDnxNetworkHandler networkHandler;
    private final List<GameProfile> loggedIn;

    public UdpServerConnectionHandler(IDnxNetworkHandler vc) {
        this.networkHandler = vc;
        this.loggedIn = new ArrayList<>();
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!this.loggedIn.contains(event.player.getGameProfile())) {
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-DEBUG] Logging " + event.player);
            this.loggedIn.add(event.player.getGameProfile());
            this.onConnected(event.player);
        }
    }

    private void onConnected(EntityPlayer entity) {
        EntityPlayerMP player = (EntityPlayerMP) entity;

        if (networkHandler instanceof UdpServerNetworkHandler) {
            UdpServerNetworkHandler voiceServer = (UdpServerNetworkHandler) this.networkHandler;
            String hash = null;

            while (hash == null) {
                try {
                    hash = this.sha256(RandomStringUtils.random(32));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-DEBUG] Initiating auth of " + player);
            voiceServer.waitingAuth.put(hash, player);
            DynamXContext.getNetwork().sendToClient(new MessageDynamXUdpSettings(this.networkHandler.getType().ordinal(), DynamXConfig.udpPort, hash, DynamXConfig.usingProxy ? ((DynamXServerNetworkSystem) DynamXContext.getNetwork()).getServerIPAdressRetriever().getAddress() : "", DynamXConfig.syncPacks), EnumPacketTarget.PLAYER, player);
        } else {
            DynamXContext.getNetwork().sendToClient(new MessageDynamXUdpSettings(this.networkHandler.getType().ordinal(), 0, "", "", DynamXConfig.syncPacks), EnumPacketTarget.PLAYER, player);
        }
    }

    @SubscribeEvent
    public void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-DEBUG] Disconnected " + event.player);
            if (networkHandler instanceof UdpServerNetworkHandler) {
                ((UdpServerNetworkHandler) networkHandler).closeConnection(event.player.getEntityId());
            }
            this.loggedIn.remove(event.player.getGameProfile());
        }
    }

    private String sha256(String s) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(s.getBytes());
        StringBuilder sb = new StringBuilder();

        for (byte aHash : Objects.requireNonNull(hash)) {
            String hex = Integer.toHexString(aHash);

            if (hex.length() == 1) {
                sb.append(0);
                sb.append(hex.charAt(hex.length() - 1));
            } else
                sb.append(hex.substring(hex.length() - 2));
        }
        return sb.toString();
    }
}