package fr.dynamx.core.client.command;

import fr.dynamx.core.common.command.ISubCommand;
import fr.dynamx.core.common.network.sync.variables.NetworkActivityTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.List;

public class CmdClientNetworkDebug implements ISubCommand {
    @Override
    public String getName() {
        return "network_debug";
    }

    @Override
    public String getUsage() {
        return "/client_dynamx network_debug <pause|resume|set|get|next|prev|set_entity>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 2) {
            throw new WrongUsageException(getUsage());
        }
        switch (args[1]) {
            case "pause":
                NetworkActivityTracker.pause();
                sender.sendMessage(new TextComponentString("Paused"));
                break;
            case "resume":
                NetworkActivityTracker.resume();
                sender.sendMessage(new TextComponentString("Resumed"));
                break;
            case "set":
                if(args.length < 3) {
                    throw new WrongUsageException("/client_dynamx network_debug set <view_index>");
                }
                NetworkActivityTracker.viewIndex = CommandBase.parseInt(args[2]);
                break;
            case "next":
                NetworkActivityTracker.viewIndex += 10;
                break;
            case "prev":
                NetworkActivityTracker.viewIndex -= 10;
                break;
            case "set_entity":
                if(args.length < 3) {
                    throw new WrongUsageException("/client_dynamx network_debug set_entity <entity_id>");
                }
                NetworkActivityTracker.viewEntity = CommandBase.parseInt(args[2]);
                break;
            default:
                throw new WrongUsageException(getUsage());
        }
        sender.sendMessage(new TextComponentString("Selected tick is " + NetworkActivityTracker.viewIndex + ". Last is " + NetworkActivityTracker.lastTime));
        sender.sendMessage(new TextComponentString("Selected entity is " + NetworkActivityTracker.viewEntity));
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if(args.length == 2) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "pause", "resume", "set", "next", "prev", "set_entity"));
        }
    }
}
