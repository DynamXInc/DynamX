package fr.dynamx.client.renders.scene.node;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.utils.client.DynamXRenderUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.inventory.EntityEquipmentSlot;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A type of root node, corresponding to an armor
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public class ArmorNode<A extends ArmorObject<?>> extends AbstractItemNode<BaseRenderContext.ArmorRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneNode<BaseRenderContext.ArmorRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    @Getter
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.ArmorRenderContext context, A packInfo) {
        //GlStateManager.scale(armorInfo.scale[0],armorInfo.scale[1],armorInfo.scale[2]);
        transform.identity();
        context.getArmorModel().isSneak = context.getEntity() != null && context.getEntity().isSneaking();
        if (context.getArmorModel().isSneak) {
            transform.translate(0.0F, 0.2F, 0.0F);
        }
        context.getArmorModel().renderPart(transform, context.getEquipmentSlot());
        //Render the linked children
        if (!linkedChildren.isEmpty()) {
            linkedChildren.forEach(c -> c.render(context, packInfo));
        }
        DynamXRenderUtils.popGlAllAttribBits();
    }

    @Override
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        EntityEquipmentSlot slot = ((DynamXItemArmor<?>) context.getStack().getItem()).armorType;
        packInfo.getObjArmor().setActivePart(slot, context.getTextureId());
        //restore default rotations (contained in ModelBiped)
        packInfo.getObjArmor().setModelAttributes(packInfo.getObjArmor());
        if (context.getRenderType() != ItemCameraTransforms.TransformType.GUI)
            transform.rotate((float) (Math.PI / 2), 1, 0, 0);
        switch (slot) {
            case FEET:
                transform.translate(0, 1.8f, -0.15f);
                break;
            case LEGS:
                transform.translate(0, 1.5f, -0.15f);
                break;
            case CHEST:
                transform.translate(0, 0.7f, -0.15f);
                break;
            case HEAD:
                transform.translate(0, 0.2f, -0.15f);
                break;
        }
        transform.rotate((float) Math.PI, 0, 0, 1);
        packInfo.getObjArmor().renderPart(transform, slot);
        DynamXRenderUtils.popGlAllAttribBits();
    }

    @Override
    public void renderDebug(BaseRenderContext.ArmorRenderContext context, A packInfo) {
    }

    @Override
    public SceneNode<BaseRenderContext.ArmorRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.ArmorRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
