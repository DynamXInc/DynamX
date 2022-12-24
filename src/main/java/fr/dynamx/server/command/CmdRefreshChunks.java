package fr.dynamx.server.command;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.command.CommandBase.parseInt;

public class CmdRefreshChunks implements ISubCommand {
    @Override
    public String getName() {
        return "refresh_chunks";
    }

    @Override
    public String getUsage() {
        return getName() + " <x1> <y1> <z1> <x2> <y2> <z2> - Updates collisions in the given zone";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length - 1 >= 0) System.arraycopy(args, 1, args, 0, args.length - 1);
        if (args.length == 7) {
            int x1 = Math.min(parseInt(args[1]), parseInt(args[4]));
            int y1 = Math.min(parseInt(args[2]), parseInt(args[5]));
            int z1 = Math.min(parseInt(args[3]), parseInt(args[6]));
            int x2 = Math.max(parseInt(args[1]), parseInt(args[4]));
            int y2 = Math.max(parseInt(args[2]), parseInt(args[5]));
            int z2 = Math.max(parseInt(args[3]), parseInt(args[6]));
            int count = 0;
            List<VerticalChunkPos> poses = new ArrayList<>();
            VerticalChunkPos.Mutable po = new VerticalChunkPos.Mutable();
            for (int x = x1; x <= x2; x += 2) {
                for (int y = y1; y <= y2; y += 2) {
                    for (int z = z1; z <= z2; z += 2) {
                        po.setPos(x >> 4, y >> 4, z >> 4);
                        if (!poses.contains(po)) {
                            VerticalChunkPos imm = po.toImmutable();
                            DynamXContext.getPhysicsWorld(sender.getEntityWorld()).getTerrainManager().onChunkChanged(imm);
                            poses.add(imm);
                            count++;
                        }
                    }
                }
            }
            sender.sendMessage(new TextComponentString("Reloaded " + count + " collision chunks"));
        } else
            throw new WrongUsageException("/dynamx " + getUsage());
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos, List<String> r) {
        if (args.length > 1 && targetPos != null) {
            if (args.length == 2 || args.length == 5)
                r.add("" + targetPos.getX());
            else if (args.length == 3 || args.length == 6)
                r.add("" + targetPos.getY());
            else if (args.length == 4 || args.length == 7)
                r.add("" + targetPos.getZ());
        }
    }
}
