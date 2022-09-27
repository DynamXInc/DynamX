package fr.dynamx.common.handlers;

import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.StorageModule;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class DynamXGuiHandler implements IGuiHandler {
    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 1) //storage
        {
            BaseVehicleEntity<?> e = (BaseVehicleEntity<?>) world.getEntityByID(x);
            return new ContainerChest(player.inventory, e.getModuleByType(StorageModule.class).getInventory(), player);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 1) //storage
        {
            BaseVehicleEntity<?> e = (BaseVehicleEntity<?>) world.getEntityByID(x);
            return new GuiChest(player.inventory, e.getModuleByType(StorageModule.class).getInventory());
        }
        return null;
    }
}
