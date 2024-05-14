package fr.dynamx.server.command;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.physics.entities.modules.CarEnginePhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.PooledHashMap;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID)
public class DynamXCommands extends CommandBase {
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public static float explosionForce = 10;

    public DynamXCommands() {
        addCommand(new CmdSlopes());
        addCommand(new CmdReloadConfig());
        addCommand(new CmdRefreshChunks());
        addCommand(new CmdNetworkConfig());
        addCommand(new CmdChunkControl());
        addCommand(new CmdSpawnObjects());
        addCommand(new CmdKillEntities());
        addCommand(new CmdOpenDebugGui());
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "spawn_ragdoll";
            }

            @Override
            public String getUsage() {
                return "spawn_ragdoll [help|player] [life_expectancy] [velocity_x] [velocity_y] [velocity_z]";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2 && args[1].equalsIgnoreCase("help")) {
                    sender.sendMessage(new TextComponentString("Usage: /dynamx " + getUsage()));
                    sender.sendMessage(new TextComponentString("Transforms the given 'player' in a ragdoll, during 'life_expectancy' ticks (or -1 for eternity). " +
                            "You can add a velocity to the ragdoll, the default value is 0, 0, 0."));
                    return;
                }
                EntityPlayerMP player;
                if (args.length >= 2)
                    player = CommandBase.getPlayer(server, sender, args[1]);
                else if (sender instanceof EntityPlayerMP)
                    player = (EntityPlayerMP) sender;
                else
                    throw new WrongUsageException("You're not a player !");
                Vector3fPool.openPool();
                int life = -1;
                if (args.length >= 3)
                    life = CommandBase.parseInt(args[2]);
                RagdollEntity ragdollEntity = new RagdollEntity(player.world, DynamXUtils.toVector3f(player.getPositionVector().add(0, player.getDefaultEyeHeight(), 0)),
                        player.rotationYaw + 180, player.getName(), (short) life, player);
                Vector3f velocity = new Vector3f(0, 0, 0); //should be permanent
                if (args.length == 6)
                    velocity.set((float) CommandBase.parseDouble(args[3]), (float) CommandBase.parseDouble(args[4]), (float) CommandBase.parseDouble(args[5]));
                ragdollEntity.setPhysicsInitCallback((a, b) -> {
                    if (b != null && b.getCollisionObject() != null) {
                        ((PhysicsRigidBody) b.getCollisionObject()).setLinearVelocity(velocity);
                    }
                });
                player.setInvisible(true);
                player.world.spawnEntity(ragdollEntity);
                if(DynamXContext.usesPhysicsWorld(player.world)) {
                    DynamXContext.getPlayerToCollision().get(player).ragdollEntity = ragdollEntity;
                    DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false, player.world);
                }
                Vector3fPool.closePool();
                if (player != sender) {
                    sender.sendMessage(new TextComponentString("Ragdoll spawned!"));
                }
            }

            @Override
            public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
                if (args.length == 2) {
                    r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()));
                }
            }
        });
        addCommand(new ISubCommand() {
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
                    explosionForce = (float) parseDouble(args[1]);
                    sender.sendMessage(new TextComponentString("Set force to " + explosionForce));
                } else
                    throw new WrongUsageException(getUsage());
            }
        });
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "testfullgo";
            }

            @Override
            public String getUsage() {
                return "testfullgo <false/true> - ready for speed ?";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2) {
                    boolean testFullGo = parseBoolean(args[1]);
                    CarEnginePhysicsHandler.inTestFullGo = testFullGo;
                    sender.sendMessage(new TextComponentString("Set test full go to " + testFullGo));
                } else
                    throw new WrongUsageException(getUsage());
            }
        });
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "terrain_debug";
            }

            @Override
            public String getUsage() {
                return "terrain_debug <false/true> - enables terrain debug";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2) {
                    boolean testFullGo = parseBoolean(args[1]);
                    DynamXConfig.enableDebugTerrainManager = testFullGo;
                    sender.sendMessage(new TextComponentString("Set terrain debug to " + testFullGo));
                } else
                    throw new WrongUsageException(getUsage());
            }
        });
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "bigdebugterrain";
            }

            @Override
            public String getUsage() {
                return "bigdebugterrain <false/true> - ready for debug and console spam ?";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2) {
                    boolean testFullGo = parseBoolean(args[1]);
                    sender.sendMessage(new TextComponentString("Set bigdebugterrain to " + testFullGo));
                } else
                    throw new WrongUsageException(getUsage());
            }
        });
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
                } else
                    throw new WrongUsageException(getUsage());
            }
        });
        addCommand(new CmdPhysicsMode());
    }

    public void addCommand(ISubCommand command) {
        commands.put(command.getName(), command);
        PermissionAPI.registerNode(command.getPermission(), DefaultPermissionLevel.OP, "/dynamx " + command.getUsage());
    }

    @Override
    public String getName() {
        return "dynamx";
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
            if (!(sender instanceof EntityPlayer) || PermissionAPI.hasPermission((EntityPlayer) sender, command.getPermission())) {
                command.execute(server, sender, args);
            } else {
                throw new CommandException("You don't have permission to use this command !");
            }
        } else
            throw new WrongUsageException(this.getUsage(sender));
    }

    public EntityPlayerMP getPlayer(String name) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(name);
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
