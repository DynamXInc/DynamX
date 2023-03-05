package fr.dynamx.utils.client;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.List;

public class CommandNetworkDebug extends CommandBase {
    @Override
    public String getName() {
        return "dnxnetdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/dnxnetdebug entity_report|pause|resume|set|get|next|prev|set_entity";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length == 1 && args[0].equalsIgnoreCase("entity_report")) {
            if(sender instanceof EntityPlayer && ((EntityPlayer) sender).getRidingEntity() instanceof PhysicsEntity) {
                ((PhysicsEntity<?>) ((EntityPlayer) sender).getRidingEntity()).printReport();
                sender.sendMessage(new TextComponentString("Report wrote in log"));
                return;
            }
            else throw new WrongUsageException("You must ride a physics entity");
        }
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
            default:
                throw new WrongUsageException(getUsage(sender));
        }
        sender.sendMessage(new TextComponentString("Selected tick is " + NetworkActivityTracker.viewIndex + ". Last is " + NetworkActivityTracker.lastTime));
        sender.sendMessage(new TextComponentString("Selected entity is " + NetworkActivityTracker.viewEntity));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1)
            return getListOfStringsMatchingLastWord(args, "entity_report");
        return super.getTabCompletions(server, sender, args, targetPos);
    }
}
