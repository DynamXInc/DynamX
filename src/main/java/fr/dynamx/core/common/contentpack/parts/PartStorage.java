package fr.dynamx.core.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.type.ObjectInfo;
import fr.dynamx.core.common.entities.IDynamXObject;
import fr.dynamx.core.common.entities.PackPhysicsEntity;
import fr.dynamx.core.common.entities.modules.StorageModule;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.client.ContentPackUtils;
import fr.dynamx.core.utils.debug.DynamXDebugOption;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.forge.DynamXForgeMod;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;

@Getter
@Setter
@RegisteredSubInfoType(name = "storage", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartStorage<T extends ISubInfoTypeOwner<T>> extends InteractivePart<IDynamXObject, T> {
    @PackFileProperty(configNames = "StorageSize")
    protected int storageSize;

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
        if (!modules.hasModuleOfClass(StorageModule.class))
            modules.add(new StorageModule(entity, this));
        else
            modules.getByClass(StorageModule.class).addInventory(entity, this);
    }

    @Override
    public void addBlockModules(TEDynamXBlock blockEntity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(StorageModule.class))
            modules.add(new StorageModule(blockEntity, blockEntity.getPos(), this));
        else
            modules.getByClass(StorageModule.class).addInventory(blockEntity, blockEntity.getPos(), this);
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/storage.png");
    }

    @Override
    public boolean interact(IDynamXObject entity, EntityPlayer player) {
        if (entity instanceof TEDynamXBlock) {
            BlockPos pos = ((TEDynamXBlock) entity).getPos();
            player.openGui(DynamXForgeMod.modInstance, getId() + 2, player.world, pos.getX(), pos.getY(), pos.getZ());
        } else if (entity instanceof PackPhysicsEntity) {
            player.openGui(DynamXForgeMod.modInstance, 1, player.world, ((Entity) entity).getEntityId(), getId(), 0);
        } else {
            throw new IllegalArgumentException("DynamX doesn't know how a storage should be opened on a " + entity);
        }
        return true;
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.SEATS_AND_STORAGE;
    }

    @Override
    public String getName() {
        return "PartStorage named " + getPartName();
    }

    @Override
    public void postLoad(T owner, boolean hot) {
        super.postLoad(owner, hot);

        if(FMLCommonHandler.instance().getSide().isClient()) {
            String ownerName = owner instanceof ObjectInfo ? ((ObjectInfo<?>) owner).getDefaultName() : owner.getName();
            ContentPackUtils.addMissingLangTranslation(DynamXMain.getInstance().getResourcesDirectory(), getPackName(),
                    "part.storage" + owner.getFullName(), ownerName + "'s trunk");
        }
    }
}
