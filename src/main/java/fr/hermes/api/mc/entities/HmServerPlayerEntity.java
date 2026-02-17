package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.HmMinecraftServer;
import net.minecraft.util.text.TextFormatting;

public interface HmServerPlayerEntity extends HmPlayerEntity {
    boolean hm$isPlayerConnected();

    HmMinecraftServer hm$getServer();
    //target.connection != null && target.connection.getNetworkManager().isChannelOpen()

    void hm$sendMessage(String message);

    void hm$sendTranslatedMessage(String translationKey, Object... args);

    void hm$sendTranslatedMessage(String translationKey, TextFormatting color, Object... args);
}
