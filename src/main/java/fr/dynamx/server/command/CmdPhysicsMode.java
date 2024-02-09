package fr.dynamx.server.command;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsSimulationMode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import fr.dynamx.common.physics.world.PhysicsSimulationModes;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.List;

public class CmdPhysicsMode implements ISubCommand {
    @Override
    public String getName() {
        return "set_physics_mode";
    }

    @Override
    public String getUsage() {
        return getRootCommandUsage() + getName() + " <get|full|light> [server_only|client_server]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2 && args[1].equals("get")) {
            sender.sendMessage(new TextComponentString("Physics simulation mode is: client=" + DynamXContext.getPhysicsSimulationMode(Side.CLIENT).getName()
                    + ", server=" + DynamXContext.getPhysicsSimulationMode(Side.SERVER).getName()));
        } else if (args.length == 3 && args[2].equals("server_only")) {
            IPhysicsSimulationMode light; //TODO CLEAN
            if (args[1].equals("light")) {
                light = new PhysicsSimulationModes.LightPhysics();
            } else if (args[1].equals("full")) {
                light = new PhysicsSimulationModes.FullPhysics();
            } else {
                throw new WrongUsageException(getUsage());
            }
            DynamXContext.setPhysicsSimulationMode(Side.SERVER, light);
            sender.sendMessage(new TextComponentString("Physics simulation mode is: client=" + DynamXContext.getPhysicsSimulationMode(Side.CLIENT).getName()
                    + ", server=" + DynamXContext.getPhysicsSimulationMode(Side.SERVER).getName()));
        } else if (args.length == 3 && args[2].equals("client_server")) {
            IPhysicsSimulationMode light;
            if (args[1].equals("light")) {
                light = new PhysicsSimulationModes.LightPhysics();
            } else if (args[1].equals("full")) {
                light = new PhysicsSimulationModes.FullPhysics();
            } else {
                throw new WrongUsageException(getUsage());
            }
            DynamXContext.setPhysicsSimulationMode(Side.SERVER, light);
            DynamXContext.setPhysicsSimulationMode(Side.CLIENT, light);
            sender.sendMessage(new TextComponentString("Physics simulation mode is: client=" + DynamXContext.getPhysicsSimulationMode(Side.CLIENT).getName()
                    + ", server=" + DynamXContext.getPhysicsSimulationMode(Side.SERVER).getName()));
            DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, light, -1), EnumPacketTarget.ALL);
        } else {
            throw new WrongUsageException(getUsage());
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.add("full");
            r.add("light");
            r.add("get");
        } else if (args.length == 3 && (args[1].equals("full") || args[1].equals("light"))) {
            r.add("server_only");
            r.add("client_server");
        }
    }
}
