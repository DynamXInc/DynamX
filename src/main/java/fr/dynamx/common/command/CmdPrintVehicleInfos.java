package fr.dynamx.common.command;

import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;

public class CmdPrintVehicleInfos implements ISubCommand {
    @Override
    public String getName() {
        return "vehicle_debug";
    }

    @Override
    public String getUsage() {
        return "/[client_]dynamx vehicle_debug";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException("This command can only be executed by a player");
        }
        EntityPlayer player = (EntityPlayer) sender;
        if (!(player.getRidingEntity() instanceof PhysicsEntity)) {
            throw new CommandException("You should riding a DynamX entity to execute this command");
        }
        PhysicsEntity<?> target = (PhysicsEntity<?>) player.getRidingEntity();
        IPartContainer<?> packInfo = null;
        StringBuilder builder = new StringBuilder();
        builder.append(TextFormatting.GOLD).append("=== DynamX entity status report ===").append("\n");
        builder.append(TextFormatting.GREEN).append("Entity: ").append(target.toString()).append("\n");
        builder.append(TextFormatting.DARK_GRAY).append("IsClient: ").append(target.world.isRemote).append("\n");
        builder.append(TextFormatting.GRAY).append("State: a=").append(target.addedToChunk).append(" i=").append(target.initialized).append(" r=").append(target.isRegistered).append(" has_phy=").append(target.physicsHandler != null).append("\n");
        if (target instanceof ModularPhysicsEntity) {
            builder.append("Modules: ").append(((ModularPhysicsEntity<?>) target).getModules().size()).append("\n");
            if (target.hasModuleOfType(WheelsModule.class)) {
                WheelsModule wheels = target.getModuleByType(WheelsModule.class);
                builder.append("Wheel states: ").append(Arrays.toString(wheels.getWheelsStates())).append("\n");
                builder.append("Wheel infos:").append(wheels.getWheelInfos()).append("\n");
                if (wheels.getPhysicsHandler() != null) {
                    builder.append("Phy wheels: ").append(wheels.getPhysicsHandler().getVehicleWheelData()).append("\n");
                }
            }
        }
        if (target instanceof PackPhysicsEntity) {
            packInfo = target.getPackInfo();
            builder.append(TextFormatting.LIGHT_PURPLE).append("PackInfo: ").append(packInfo == null ? "null" : packInfo.toString()).append(" // ").append(((PackPhysicsEntity<?, ?>) target).getInfoName()).append("\n");
            if (packInfo != null) {
                builder.append(TextFormatting.LIGHT_PURPLE).append("as=").append(packInfo.getAllParts().size()).append(" sbs=").append(packInfo.getSubProperties().size()).append("\n");
            }
        }

        System.out.println(builder);
        System.out.println("Additional data:");
        if (target instanceof ModularPhysicsEntity) {
            System.out.println("Modules: " + ((ModularPhysicsEntity<?>) target).getModules());
        }
        NBTTagCompound tagCompound = new NBTTagCompound();
        target.writeToNBT(tagCompound);
        System.out.println("NBT " + tagCompound);
        System.out.println("===================================");

        builder.append(TextFormatting.GOLD).append("===================================");
        sender.sendMessage(new TextComponentString(builder.toString()));
    }
}
