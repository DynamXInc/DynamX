package fr.dynamx.server.command;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.items.DynamXItemSpawner;
import lombok.SneakyThrows;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CmdSpawnObjects implements ISubCommand {
    private final List<String> objectIds = new ArrayList<>();

    public CmdSpawnObjects() {
        objectIds.addAll(DynamXObjectLoaders.WHEELED_VEHICLES.getInfos().keySet());
        objectIds.addAll(DynamXObjectLoaders.TRAILERS.getInfos().keySet());
        objectIds.addAll(DynamXObjectLoaders.BOATS.getInfos().keySet());
        objectIds.addAll(DynamXObjectLoaders.HELICOPTERS.getInfos().keySet());
        objectIds.addAll(DynamXObjectLoaders.PROPS.getInfos().keySet());
        objectIds.add("ragdoll");
    }

    @Override
    public String getName() {
        return "spawn_objects";
    }

    @Override
    public String getUsage() {
        return "spawn_objects <object_id> <x> <y> <z> [cooldown] [width] [height] [depth] [xSpace] [ySpace] [zSpace] [spawnsPerStep] [delayBetweenSteps]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 5)
            throw new WrongUsageException(getUsage());
        String id = args[1];
        if (!objectIds.contains(id))
            throw new WrongUsageException("Unknown object_id " + id);
        int cooldown = 0, width = 1, height = 1, depth = 1, xSpace = 1, ySpace = 1, zSpace = 1, spawnsPerTick = 1;
        short interDelay = 1;
        switch (args.length) {
            case 14:
                interDelay = (short) CommandBase.parseInt(args[13]);
            case 13:
                spawnsPerTick = CommandBase.parseInt(args[12]);
            case 12:
                zSpace = CommandBase.parseInt(args[11]);
                if (zSpace < 1)
                    throw new WrongUsageException("zSpace must be >= 1");
            case 11:
                ySpace = CommandBase.parseInt(args[10]);
                if (ySpace < 1)
                    throw new WrongUsageException("ySpace must be >= 1");
            case 10:
                xSpace = CommandBase.parseInt(args[9]);
                if (xSpace < 1)
                    throw new WrongUsageException("xSpace must be >= 1");
            case 9:
                depth = CommandBase.parseInt(args[8]);
            case 8:
                height = CommandBase.parseInt(args[7]);
            case 7:
                width = CommandBase.parseInt(args[6]);
            case 6:
                cooldown = CommandBase.parseInt(args[5]);
        }
        BlockPos pos = CommandBase.parseBlockPos(sender, args, 2, false);
        if (cooldown == 0) {
            sender.sendMessage(new TextComponentString("Spawning so many entities that you will laggggg !!"));
            iterate(sender, id, 0, 0, 0, width, height, depth, xSpace, ySpace, zSpace,
                    0, spawnsPerTick, System.currentTimeMillis(), 0, pos.getX(), pos.getY(), pos.getZ(), interDelay);
        } else {
            sender.sendMessage(new TextComponentString("Spawning so many entities that you will laggggg !! Starting in " + cooldown + " ticks..."));
            int finalWidth = width, finalHeight = height, finalDepth = depth, finalXSpace = xSpace, finalYSpace = ySpace, finalZSpace = zSpace, finalSpawnsPerTick = spawnsPerTick;
            short finalInterDelay = interDelay;
            TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) cooldown) {
                @Override
                public void run() {
                    try {
                        iterate(sender, id, 0, 0, 0, finalWidth, finalHeight, finalDepth, finalXSpace, finalYSpace, finalZSpace,
                                0, finalSpawnsPerTick, System.currentTimeMillis(), 0, pos.getX(), pos.getY(), pos.getZ(), finalInterDelay);
                    } catch (CommandException e) {
                        e.printStackTrace();
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + " An error occured, try with a cooldown of 0 to see your mistake :)"));
                    }
                }
            });
        }
    }

    private static void iterate(ICommandSender sender, String id, int x, int y, int z, int width, int height, int depth, int dx, int dy, int dz, int amount, int da, long lastMs, int count, int ox, int oy, int oz, short interDelay) throws CommandException {
        if (x < width) {
            spawn(sender.getEntityWorld(), id, ox + x, oy + y, oz + z);
            if (amount + 1 >= da) {
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask(interDelay) {
                    @SneakyThrows
                    @Override
                    public void run() {
                        long cur = System.currentTimeMillis();
                        sender.sendMessage(new TextComponentString("Tick time " + (cur - lastMs) + " ms at " + da + " entities/step. " + count + " spawned."));
                        iterate(sender, id, x + dx, y, z, width, height, depth, dx, dy, dz, 0, da, cur, count + 1, ox, oy, oz, interDelay);
                    }
                });
            } else {
                iterate(sender, id, x + dx, y, z, width, height, depth, dx, dy, dz, amount + 1, da, lastMs, count + 1, ox, oy, oz, interDelay);
            }
        } else {
            if (z + dz < depth) {
                iterate(sender, id, 0, y, z + dz, width, height, depth, dx, dy, dz, amount, da, lastMs, count, ox, oy, oz, interDelay);
            } else if (y + dy < height) {
                iterate(sender, id, 0, y + dy, 0, width, height, depth, dx, dy, dz, amount, da, lastMs, count, ox, oy, oz, interDelay);
            } else
                sender.sendMessage(new TextComponentString("Finished spawning of " + count + " entities !"));
        }
    }

    private static void spawn(World w, String id, int x, int y, int z) throws CommandException {
        if (id.equalsIgnoreCase("ragdoll")) {
            int skin = w.rand.nextInt(36);
            Entity e = new RagdollEntity(w, new Vector3f(x, y, z), 0, "dynamxmod:skins/crash_test_dummy_" + skin + ".png");
            w.spawnEntity(e);
        } else {
            DynamXItemSpawner<?> item = getSpawnItem(id);
            if (item == null) {
                throw new CommandException("Item for " + id + " not found. Check the loading errors.");
            }
            float rotationYaw = 0;
            PackPhysicsEntity<?, ?> entity = item.getSpawnEntity(w, null, new Vector3f(x, y, z), rotationYaw, 0);
            if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Spawn(w, entity, null, item, new Vec3d(x, y, z)))) {
                w.spawnEntity(entity);
            }
        }
    }

    public static DynamXItemSpawner<?> getSpawnItem(String vehicleModel) {
        PropObject<?> prop = DynamXObjectLoaders.PROPS.findInfo(vehicleModel);
        if (prop != null)
            return (DynamXItemSpawner<?>) DynamXObjectLoaders.PROPS.owners.stream().filter(o -> o.getInfo().getFullName().equals(vehicleModel)).findFirst().orElse(null);
        ModularVehicleInfo info = DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(vehicleModel);
        if (info != null)
            return (DynamXItemSpawner<?>) DynamXObjectLoaders.WHEELED_VEHICLES.owners.stream().filter(o -> o.getInfo().getFullName().equals(vehicleModel)).findFirst().orElse(null);
        info = DynamXObjectLoaders.TRAILERS.findInfo(vehicleModel);
        if (info != null)
            return (DynamXItemSpawner<?>) DynamXObjectLoaders.TRAILERS.owners.stream().filter(o -> o.getInfo().getFullName().equals(vehicleModel)).findFirst().orElse(null);
        info = DynamXObjectLoaders.BOATS.findInfo(vehicleModel);
        if (info != null)
            return (DynamXItemSpawner<?>) DynamXObjectLoaders.BOATS.owners.stream().filter(o -> o.getInfo().getFullName().equals(vehicleModel)).findFirst().orElse(null);
        info = DynamXObjectLoaders.HELICOPTERS.findInfo(vehicleModel);
        if (info != null)
            return (DynamXItemSpawner<?>) DynamXObjectLoaders.HELICOPTERS.owners.stream().filter(o -> o.getInfo().getFullName().equals(vehicleModel)).findFirst().orElse(null);
        return null;
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.addAll(objectIds);
        } else if (args.length == 3 || args.length == 4 || args.length == 5) {
            r.addAll(CommandBase.getTabCompletionCoordinate(args, 2, targetPos));
        }
    }
}
