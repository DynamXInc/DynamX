package fr.dynamx.utils.client;

import fr.dynamx.api.network.sync.v3.NetworkActivityTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandNetworkDebug extends CommandBase {
    @Override
    public String getName() {
        return "dnxnetdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/dnxnetdebug pause|resume|set|get|next|prev|set_entity";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        switch (args[0]) {
            case "pause":
                NetworkActivityTracker.pause();
                sender.sendMessage(new TextComponentString("Paused"));
                break;
            case "resume":
                NetworkActivityTracker.resume();
                sender.sendMessage(new TextComponentString("Resumed"));
                break;
            case "set":
                NetworkActivityTracker.viewIndex = parseInt(args[1]);
                break;
            case "next":
                NetworkActivityTracker.viewIndex += 10;
                break;
            case "prev":
                NetworkActivityTracker.viewIndex -= 10;
                break;
            case "set_entity":
                NetworkActivityTracker.viewEntity = parseInt(args[1]);
                break;
        }
        sender.sendMessage(new TextComponentString("Selected tick is " + NetworkActivityTracker.viewIndex + ". Last is " + NetworkActivityTracker.lastTime));
        sender.sendMessage(new TextComponentString("Selected entity is " + NetworkActivityTracker.viewEntity));
    }
}
