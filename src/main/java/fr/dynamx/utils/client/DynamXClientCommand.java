package fr.dynamx.utils.client;

import fr.dynamx.common.command.CmdChunkControl;
import fr.dynamx.common.command.CmdOpenDebugGui;
import fr.dynamx.common.command.CmdTerrainDebug;
import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamXClientCommand extends CommandBase {
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public DynamXClientCommand() {
        addCommand(new CmdOpenDebugGui());
        addCommand(new CmdChunkControl());
        addCommand(new CmdTerrainDebug());

        //TODO INVESTIGATE ON DISABLE MAP POOL ON CLIENT THEN REMOVE
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "disablemappool";
            }

            @Override
            public String getUsage() {
                return "disablemappool <false/true> - tests for hash map pool optimizations";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2) {
                    boolean testFullGo = parseBoolean(args[1]);
                    PooledHashMap.DISABLE_POOL = testFullGo;
                    sender.sendMessage(new TextComponentString("Set disablemappool to " + testFullGo));
                } else {
                    throw new WrongUsageException(getUsage());
                }
            }
        });
    }

    public void addCommand(ISubCommand command) {
        commands.put(command.getName(), command);
    }

    @Override
    public String getName() {
        return "dynamx_client";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        StringBuilder usage = new StringBuilder();
        commands.keySet().forEach(s -> usage.append("|").append(s));
        return "/dynamx <" + usage.substring(1) + ">";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && commands.containsKey(args[0])) {
            ISubCommand command = commands.get(args[0]);
            command.execute(server, sender, args);
        } else {
            throw new WrongUsageException(this.getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> r = new ArrayList<String>();
        if (args.length == 1) {
            r.addAll(commands.keySet());
        } else if (args.length > 1 && commands.containsKey(args[0])) {
            commands.get(args[0]).getTabCompletions(server, sender, args, targetPos, r);
        }
        return getListOfStringsMatchingLastWord(args, r);
    }
}
