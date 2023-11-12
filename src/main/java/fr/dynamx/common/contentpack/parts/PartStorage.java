package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.StorageModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

@RegisteredSubInfoType(name = "storage", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartStorage<T extends ISubInfoTypeOwner<T>> extends InteractivePart<PackPhysicsEntity<?, ?>, T>
{
    @PackFileProperty(configNames = "StorageSize")
    private int storageSize;

    public PartStorage(T owner, String partName) {
        super(owner, partName, 0.5f, 0.5f);
    }

    @Override
    public void appendTo(T owner) {
        if (storageSize % 9 != 0)
            throw new IllegalArgumentException("StorageSize must be a multiple of 9 !");
        super.appendTo(owner);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if(!modules.hasModuleOfClass(StorageModule.class))
            modules.add(new StorageModule(entity, this));
        else
            modules.getByClass(StorageModule.class).addInventory(entity, this);
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/storage.png");
    }

    @Override
    public boolean interact(PackPhysicsEntity<?, ?> vehicleEntity, EntityPlayer player) {
        if (player.isSneaking()) {
            player.openGui(DynamXMain.instance, 1, player.world, vehicleEntity.getEntityId(), getId(), 0);
            return true;
        }
        return false;
    }

    public int getStorageSize() {
        return storageSize;
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.SEATS_AND_STORAGE;
    }

    @Override
    public String getName() {
        return "PartStorage named " + getPartName();
    }
}
