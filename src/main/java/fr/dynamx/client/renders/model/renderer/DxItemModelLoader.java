package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.events.client.DynamXRenderItemEvent;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.AbstractItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class DxItemModelLoader extends TileEntityItemStackRenderer implements ICustomModelLoader {
    private final Map<ModelResourceLocation, IResourcesOwner> REGISTRY = new HashMap<>();
    private final Map<Item, Map<Byte, ItemDxModel>> ITEM_TO_MODEL = new HashMap<>();
    private final BaseRenderContext.ItemRenderContext renderContext = new BaseRenderContext.ItemRenderContext();

    public static ItemCameraTransforms.TransformType renderType;

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return REGISTRY.containsKey(modelLocation);
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ItemDxModel model = new ItemDxModel(modelLocation, REGISTRY.get(modelLocation).getDxModel());
        Item item = REGISTRY.get(modelLocation).getItem();
        if (!ITEM_TO_MODEL.containsKey(item))
            ITEM_TO_MODEL.put(item, new HashMap<>());
        ITEM_TO_MODEL.get(item).put(Byte.valueOf(((ModelResourceLocation) modelLocation).getVariant().replace("inventory_", "")), model);
        return model;
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        if (ITEM_TO_MODEL.containsKey(stack.getItem())) {
            ItemDxModel model = getModel(stack.getItem(), stack.isItemStackDamageable() ? 0 : (byte) stack.getMetadata());
            DxModelRenderer modelRenderer = model != null ? DynamXContext.getDxModelRegistry().getModel(model.getOwner().getModel()) : null;
            if (modelRenderer == null) {
                RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                renderItem.renderItem(stack, renderItem.getItemModelMesher().getModelManager().getMissingModel());
                return;
            }
            SceneNode<?, ?> sceneGraph = model.getOwner().getSceneGraph();
            if (!(sceneGraph instanceof AbstractItemNode)) {
                throw new IllegalStateException("The scene graph of the item " + stack.getItem() + " is not an IItemNode");
            }
            renderContext.setModelParams(model, stack, modelRenderer, (byte) stack.getMetadata()).setRenderParams(renderType, partialTicks, true);
            if (!MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.PRE))) {
                ((AbstractItemNode<?, IModelPackObject>) sceneGraph).renderAsItemNode(renderContext, model.getOwner());
                MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.POST));
            }
        } else {
            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            renderItem.renderItem(stack, renderItem.getItemModelMesher().getModelManager().getMissingModel());
        }
    }

    /**
     * Registers the obj model of the given item
     *
     * @param item     The item
     * @param meta     The metadata
     * @param location An identifier for the item model - typically modid:itemname
     */
    public void registerItemModel(IResourcesOwner item, int meta, ResourceLocation location) {
        ModelResourceLocation loc = new ModelResourceLocation(new ResourceLocation(location.getNamespace(), location.getPath() + ".obj"), "inventory_" + meta);
        if (!REGISTRY.containsValue(loc)) {
            REGISTRY.put(loc, item);
            item.getItem().setTileEntityItemStackRenderer(this);
        }
        ModelLoader.setCustomModelResourceLocation(item.getItem(), meta, loc);
    }

    /**
     * @return The model corresponding to the given item, or null
     */
    public ItemDxModel getModel(Item of, byte meta) {
        return ITEM_TO_MODEL.get(of).get(meta);
    }

    /**
     * Refreshes the obj models used to render the items <br>
     * Called after packs reload
     */
    @SideOnly(Side.CLIENT)
    public void refreshItemInfos() {
        REGISTRY.values().forEach(owner -> {
            if (ITEM_TO_MODEL.containsKey(owner.getItem())) {
                ITEM_TO_MODEL.get(owner.getItem()).values().forEach(i -> i.setOwner(owner.getDxModel()));
            }
        });
    }
}