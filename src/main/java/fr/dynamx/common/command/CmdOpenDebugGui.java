package fr.dynamx.common.command;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.client.gui.NewGuiDnxDebug;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessageOpenDebugGui;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
        if (sender instanceof EntityPlayerMP) {
            DynamXContext.getNetwork().sendToClient(new MessageOpenDebugGui((byte) 125), EnumPacketTarget.PLAYER, (EntityPlayerMP) sender);
        } else if(sender instanceof EntityPlayer) {
            executeClient();
        } // else it's server console
    }

    @SideOnly(Side.CLIENT)
    private void executeClient() {
        ACsGuiApi.asyncLoadThenShowGui("Dnx Debug", NewGuiDnxDebug::new);
    }
}
