package fr.hermes.api.mc.entities;

public interface HmServerPlayerEntity extends HmPlayerEntity {
    boolean isPlayerConnected();
    //target.connection != null && target.connection.getNetworkManager().isChannelOpen()
}
