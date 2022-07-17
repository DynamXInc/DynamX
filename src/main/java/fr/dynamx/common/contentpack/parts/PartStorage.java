package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.StorageModule;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class PartStorage extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfoBuilder> //TODO FIX MULTI-STORAGE ON ONE ENTITY
{
    @PackFileProperty(configNames = "StorageSize")
    private int storageSize;

    public PartStorage(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName, 0.5f, 0.5f);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder modulableVehicleInfo) {
        if (storageSize % 9 != 0)
            throw new IllegalArgumentException("StorageSize must be a multiple of 9 !");
        super.appendTo(modulableVehicleInfo);
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        modules.add(new StorageModule(entity, this));
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/storage.png");
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> vehicleEntity, EntityPlayer player) {
        if (player.isSneaking()) {
            player.openGui(DynamXMain.instance, 1, player.world, vehicleEntity.getEntityId(), storageSize, 0);
            return true;
        }
        return false;
    }

    public int getStorageSize() {
        return storageSize;
    }

    @Override
    public String getName() {
        return "PartStorage named " + getPartName() + " in " + getOwner().getName();
    }
}
