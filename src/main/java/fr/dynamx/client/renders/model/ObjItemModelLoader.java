package fr.dynamx.client.renders.model;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.DynamXItemEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.api.obj.IObjModelRegistry;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.items.DynamXItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class ObjItemModelLoader extends TileEntityItemStackRenderer implements IObjModelRegistry.IObjItemModelRenderer {
    private final Map<ModelResourceLocation, IResourcesOwner> REGISTRY = new HashMap<>();
    private final Map<Item, Map<Byte, ItemObjModel>> ITEM_TO_MODEL = new HashMap<>();

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
        ItemObjModel model = new ItemObjModel(modelLocation, REGISTRY.get(modelLocation).getObjModel());
        Item item = REGISTRY.get(modelLocation).getItem();
        if (!ITEM_TO_MODEL.containsKey(item))
            ITEM_TO_MODEL.put(item, new HashMap<>());
        ITEM_TO_MODEL.get(item)
                .put(Byte.valueOf(((ModelResourceLocation) modelLocation).getVariant().replace("inventory_", "")), model);
        return model;
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        if (ITEM_TO_MODEL.containsKey(stack.getItem())) {
            ItemObjModel model = getModel(stack.getItem(), stack.isItemStackDamageable() ? 0 : (byte) stack.getMetadata());
            if (model == null) {
                RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                renderItem.renderItem(stack, renderItem.getItemModelMesher().getModelManager().getMissingModel());
                return;
                //throw new NoSuchElementException("Item " + stack.getItem().getRegistryName() + " is not registered with metadata " + stack.getMetadata() + " into ObjItemModelLoader");
            }
            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, ItemCameraTransforms.TransformType.NONE))) {
                if (model.getOwner().get3DItemRenderLocation() == Enum3DRenderLocation.NONE || (renderType == ItemCameraTransforms.TransformType.GUI && model.getOwner().get3DItemRenderLocation() == Enum3DRenderLocation.WORLD)) {
                    GlStateManager.translate(0.5F, 0.5F, 0.5F);
                    Minecraft.getMinecraft().getRenderItem().renderItem(stack, model.getGuiBaked());
                } else {
                    switch (renderType) {
                        case NONE:
                            break;
                        case THIRD_PERSON_LEFT_HAND:
                        case THIRD_PERSON_RIGHT_HAND:
                            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, renderType))) {
                                GlStateManager.translate(0.5, 0.3, 0.3);
                                GlStateManager.rotate(-100, 1, 0, 0);
                                GlStateManager.rotate(200, 0, 0, 1);
                            }
                            break;
                        case FIRST_PERSON_LEFT_HAND:
                        case FIRST_PERSON_RIGHT_HAND:
                            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, renderType))) {
                                GlStateManager.translate(0.5, 0.3, -0.3);
                                GlStateManager.rotate(-100, 1, 0, 0);
                                GlStateManager.rotate(200, 0, 0, 1);
                            }
                            break;
                        case HEAD:
                            break;
                        case GUI:
                            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, renderType))) {
                                GlStateManager.translate(0.5, 0.3, 0);

                                String tip = model.getOwner().getItemIcon();
                                if (!StringUtils.isNullOrEmpty(tip)) {
                                    GlStateManager.pushMatrix();
                                    GlStateManager.disableLighting();
                                    GlStateManager.translate(0, 0, 20);
                                    GlStateManager.rotate(-150, 1, 0, 0);
                                    GlStateManager.scale(0.035, 0.035f, 1);
                                    Minecraft.getMinecraft().fontRenderer.drawString(tip, -13, -22, 0xFFFFFFFF);
                                    GlStateManager.popMatrix();
                                }
                                GlStateManager.rotate(-150, 1, 0, 0);
                                GlStateManager.rotate(200, 0, 0, 1);
                                GlStateManager.rotate(-25, 0, 1, 0);
                            }
                            break;
                        case GROUND:
                            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, renderType))) {
                                GlStateManager.translate(0.5, 0.3, 0.5);
                            }
                            break;
                        case FIXED:
                            if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.PRE, renderType))) {
                                GlStateManager.rotate(-100, 1, 0, 0);
                                GlStateManager.rotate(200, 0, 0, 1);
                            }
                            break;
                    }
                    float scale = model.getOwner().getItemScale();
                    Vector3f translate = model.getOwner().getItemTranslate();
                    Vector3f rotate = model.getOwner().getItemRotate();
                    GlStateManager.translate(translate.x, translate.y, translate.z);
                    GlStateManager.scale(scale, scale, scale);
                    GlStateManager.rotate(rotate.x, 1, 0, 0);
                    GlStateManager.rotate(rotate.y, 0, 1, 0);
                    GlStateManager.rotate(rotate.z, 0, 0, 1);
                    if (!MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.RENDER, ItemCameraTransforms.TransformType.FIXED))) {
                        model.renderModel(stack, renderType);
                    }
                    MinecraftForge.EVENT_BUS.post(new DynamXItemEvent.RenderItem(stack.getItem(), EventStage.POST, ItemCameraTransforms.TransformType.NONE));
                }
            }
        } else {
            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            renderItem.renderItem(stack, renderItem.getItemModelMesher().getModelManager().getMissingModel());
            //throw new NoSuchElementException("Item " + stack.getItem() + " is not registered into ObjItemModelLoader");
        }
    }

    @Override
    public void registerItemModel(IResourcesOwner item, int meta, ResourceLocation location) {
        ModelResourceLocation loc = new ModelResourceLocation(new ResourceLocation(location.getNamespace(), location.getPath() + ".obj"), "inventory_" + meta);
        if (!REGISTRY.containsValue(loc)) {
            REGISTRY.put(loc, item);
            item.getItem().setTileEntityItemStackRenderer(this);
        }
        ModelLoader.setCustomModelResourceLocation(item.getItem(), meta, loc);
    }

    @Override
    public ItemObjModel getModel(Item of, byte meta) {
        return ITEM_TO_MODEL.get(of).get(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void refreshItemInfos() {
        REGISTRY.values().forEach(owner -> {
            if (ITEM_TO_MODEL.containsKey(owner.getItem())) {
                ITEM_TO_MODEL.get(owner.getItem()).values().forEach(i -> i.setOwner(owner.getObjModel()));
            }
        });
    }
}