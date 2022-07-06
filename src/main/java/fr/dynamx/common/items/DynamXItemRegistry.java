package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ContentPackUtils;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = DynamXConstants.ID)
public class DynamXItemRegistry
{
    private static final List<IResourcesOwner> ITEMS = new ArrayList<>();

    public static CreativeTabs vehicleTab = new CreativeTabs(DynamXConstants.ID+"_vehicle") {
        @Override
        public ItemStack createIcon() {
            Optional<IInfoOwner<ModularVehicleInfo<?>>> item = DynamXObjectLoaders.WHEELED_VEHICLES.owners.stream().filter(blockObjectIInfoOwner -> blockObjectIInfoOwner.getInfo().getCreativeTab(null) == null).findFirst();
            return item.map(blockObjectIInfoOwner -> new ItemStack((Item) blockObjectIInfoOwner)).orElseGet(() -> new ItemStack(Items.CARROT));
        }
    };
    public static CreativeTabs objectTab = new CreativeTabs(DynamXConstants.ID+"_object") {
        @Override
        public ItemStack createIcon() {
            Optional<IInfoOwner<BlockObject<?>>> item = DynamXObjectLoaders.BLOCKS.owners.stream().filter(blockObjectIInfoOwner -> blockObjectIInfoOwner.getInfo().getCreativeTab(null) == null).findFirst();
            return item.map(blockObjectIInfoOwner -> new ItemStack((Block) blockObjectIInfoOwner)).orElseGet(() -> new ItemStack(Items.CARROT));
        }
    };

    public static final List<CreativeTabs> creativeTabs = new ArrayList<>();
    public static final Item ITEM_WRENCH = new ItemWrench();

    public static void add(Item item) {
        ITEMS.add(IResourcesOwner.of(item));
    }

    public static void injectItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> items = event.getRegistry();
        ITEMS.stream().map(IResourcesOwner::getItem).forEach(items::register);
    }

    public static void registerItemBlock(DynamXBlock<?> block){
        add(new DynamXItemBlock(block));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerItemModels(ModelRegistryEvent event)
    {
        ITEMS.forEach(items -> {
            for (byte i = 0; i < items.getMaxMeta(); i++) {
                registerModel(items, i);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    public static void registerModel(IResourcesOwner item, byte metadata)
    {
        if(item instanceof IInfoOwner && item.createJson()) {
            ContentPackUtils.addMissingJSONs(item, ((IInfoOwner<?>) item).getInfo(), DynamXMain.resDir, metadata);
        }
        String resourceName = DynamXConstants.ID+":"+item.getJsonName(metadata);
        if(item.getObjModel() != null && item.getObjModel().isModelValid())
            DynamXContext.getObjModelRegistry().getItemRenderer().registerItemModel(item, metadata, new ResourceLocation(resourceName));
        else
            ModelLoader.setCustomModelResourceLocation(item.getItem(), metadata, new ModelResourceLocation(resourceName, "inventory"));
    }
}