package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.HmMinecraftServer;

public interface HmServerPlayerEntity extends HmPlayerEntity {
    boolean hm$isPlayerConnected();

    HmMinecraftServer hm$getServer();
    //target.connection != null && target.connection.getNetworkManager().isChannelOpen()
}
