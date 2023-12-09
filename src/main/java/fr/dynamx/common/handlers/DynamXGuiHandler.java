package fr.dynamx.common.handlers;

import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.StorageModule;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class DynamXGuiHandler implements IGuiHandler {
    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 1) //entity storage
        {
            PackPhysicsEntity<?, ?> e = (PackPhysicsEntity<?, ?>) world.getEntityByID(x);
            IInventory inventory = e != null && e.hasModuleOfType(StorageModule.class) ? e.getModuleByType(StorageModule.class).getInventory((byte) y) : null;
            System.out.println("Recuit simumlé " + e +" "+inventory);
            return inventory == null ? null : new ContainerChest(player.inventory, inventory, player);
        } else if(ID >= 2) //block storage
        {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if(!(te instanceof TEDynamXBlock))
                return null;
            TEDynamXBlock block = (TEDynamXBlock) te;
            IInventory inventory = block.getStorageModule() != null ? block.getStorageModule().getInventory((byte) (ID-2)) : null;
            return inventory == null ? null : new ContainerChest(player.inventory, inventory, player);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 1) //entity storage
        {
            PackPhysicsEntity<?, ?> e = (PackPhysicsEntity<?, ?>) world.getEntityByID(x);
            IInventory inventory = e != null && e.hasModuleOfType(StorageModule.class) ? e.getModuleByType(StorageModule.class).getInventory((byte) y) : null;
            return inventory == null ? null : new GuiChest(player.inventory, inventory);
        } else if(ID >= 2) //block storage
        {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if(!(te instanceof TEDynamXBlock))
                return null;
            TEDynamXBlock block = (TEDynamXBlock) te;
            IInventory inventory = block.getStorageModule() != null ? block.getStorageModule().getInventory((byte) (ID-2)) : null;
            return inventory == null ? null : new GuiChest(player.inventory, inventory);
        }
        return null;
    }
}
