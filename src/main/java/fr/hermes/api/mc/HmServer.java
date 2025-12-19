package fr.hermes.api.mc;

public interface HmServer {
    boolean isDedicatedServer();

    String getHostname(); // mc.getServerHostname()

    void sendGlobalChatMessage(String message);
}
