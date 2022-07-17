package fr.dynamx.server.command;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class CmdSpawnObjects implements ISubCommand {
    @Override
    public String getName() {
        return "spawn_objects";
    }

    @Override
    public String getUsage() {
        return "spawn_objects <object_id> <x> <y> <z> <width> <height> <depth> <xSpace> <ySpace> <zSpace> <spawnsPerTick> <cooldown>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 13) {
            String id = args[1];
            if (!id.equalsIgnoreCase("ragdoll")) {
                if (!id.startsWith("props:") || DynamXObjectLoaders.PROPS.findInfo(id.replace("props:", "")) == null) {
                    if (!id.startsWith("car:") || DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(id.replace("car:", "")) == null) {
                        throw new WrongUsageException("Unknown object_id " + id);
                    }
                }
            }
            BlockPos pos = CommandBase.parseBlockPos(sender, args, 2, false);
            if (CommandBase.parseInt(args[12]) == 0) {
                sender.sendMessage(new TextComponentString("Spawning so many entities that you will laggggg !!"));
                iterate(sender, id, 0, 0, 0,
                        CommandBase.parseInt(args[5]),
                        CommandBase.parseInt(args[6]),
                        CommandBase.parseInt(args[7]),
                        CommandBase.parseInt(args[8]),
                        CommandBase.parseInt(args[9]),
                        CommandBase.parseInt(args[10]),
                        0, CommandBase.parseInt(args[11]),
                        System.currentTimeMillis(), 0, pos.getX(), pos.getY(), pos.getZ());
            } else {
                sender.sendMessage(new TextComponentString("Spawning so many entities that you will laggggg !! Starting in " + args[12] + " ticks..."));
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) CommandBase.parseInt(args[12])) {
                    @Override
                    public void run() {
                        try {
                            iterate(sender, id, 0, 0, 0,
                                    CommandBase.parseInt(args[5]),
                                    CommandBase.parseInt(args[6]),
                                    CommandBase.parseInt(args[7]),
                                    CommandBase.parseInt(args[8]),
                                    CommandBase.parseInt(args[9]),
                                    CommandBase.parseInt(args[10]),
                                    0, CommandBase.parseInt(args[11]),
                                    System.currentTimeMillis(), 0, pos.getX(), pos.getY(), pos.getZ());
                        } catch (NumberInvalidException e) {
                            e.printStackTrace();
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + " An error occured, try with a cooldown of 0 to see your mistake :)"));
                        }
                    }
                });
            }
        } else
            throw new WrongUsageException(getUsage());
    }

    private static void iterate(ICommandSender sender, String id, int x, int y, int z, int width, int height, int depth, int dx, int dy, int dz, int amount, int da, long lastMs, int count, int ox, int oy, int oz) {
        if (x < width) {
            spawn(sender.getEntityWorld(), id, ox + x, oy + y, oz + z);
            if (amount + 1 >= da) {
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 1) {
                    @Override
                    public void run() {
                        long cur = System.currentTimeMillis();
                        sender.sendMessage(new TextComponentString("Tick time " + (cur - lastMs) + " ms at " + da + " entities/tick. " + count + " spawned."));
                        iterate(sender, id, x + dx, y, z, width, height, depth, dx, dy, dz, 0, da, cur, count + 1, ox, oy, oz);
                    }
                });
            } else {
                iterate(sender, id, x + dx, y, z, width, height, depth, dx, dy, dz, amount + 1, da, lastMs, count + 1, ox, oy, oz);
            }
        } else {
            if (z + dz < depth) {
                iterate(sender, id, 0, y, z + dz, width, height, depth, dx, dy, dz, amount, da, lastMs, count, ox, oy, oz);
            } else if (y + dy < height) {
                iterate(sender, id, 0, y + dy, 0, width, height, depth, dx, dy, dz, amount, da, lastMs, count, ox, oy, oz);
            } else
                sender.sendMessage(new TextComponentString("Finished spawning of " + count + " entities !"));
        }
    }

    private static void spawn(World w, String id, int x, int y, int z) {
        Entity e = null;
        if (id.equalsIgnoreCase("ragdoll")) {
            int skin = w.rand.nextInt(36);
            e = new RagdollEntity(w, new Vector3f(x, y, z), 0, "dynamxmod:skins/crash_test_dummy_" + skin + ".png");
        } else if (id.startsWith("props")) {
            e = new PropsEntity<>(id.replace("props:", ""), w, new Vector3f(x, y, z), 0, 0);
        } else if (id.startsWith("car")) {
            //TODO SUPPORT TRAILERS ETC
            e = new CarEntity<>(id.replace("car:", ""), w, new Vector3f(x, y, z), 0, 0);
        }
        if (e != null)
            w.spawnEntity(e);
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.add("ragdoll");
            r.add("props:");
            r.add("car:");
        } else if (args.length == 3 || args.length == 4 || args.length == 5) {
            r.addAll(CommandBase.getTabCompletionCoordinate(args, 2, targetPos));
        }
    }
}
