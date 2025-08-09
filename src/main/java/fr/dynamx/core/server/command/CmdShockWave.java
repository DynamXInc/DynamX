package fr.dynamx.core.server.command;

import fr.dynamx.core.common.command.ISubCommand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CmdShockWave implements ISubCommand {
    public static float explosionForce = 10;

    @Override
    public String getName() {
        return "shockwave";
    }

    @Override
    public String getUsage() {
        return "shockwave <force> - Changes shockwave force";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2) {
            explosionForce = (float) CommandBase.parseDouble(args[1]);
            sender.sendMessage(new TextComponentString("Set force to " + explosionForce));
        } else {
            throw new WrongUsageException(getUsage());
        }
    }
}
