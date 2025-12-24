package fr.hermes.api.mc;

public interface HmServer {
    boolean hm$isDedicatedServer();

    String hm$getHostname(); // mc.getServerHostname()

    void hm$sendGlobalChatMessage(String message);

    void hm$addScheduledTask(Runnable task);
}
