package fr.dynamx.server.command;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessageOpenDebugGui;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class CmdOpenDebugGui implements ISubCommand {
    @Override
    public String getName() {
        return "debug_gui";
    }

    @Override
    public String getUsage() {
        return "debug_gui";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            DynamXContext.getNetwork().sendToClient(new MessageOpenDebugGui((byte) 125), EnumPacketTarget.PLAYER, (EntityPlayerMP) sender);
        }
    }
}
