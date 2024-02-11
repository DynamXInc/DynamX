package fr.dynamx.client.renders.scene.node;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A type of root node, corresponding to an item
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public class ItemNode<A extends ItemObject<?>> extends AbstractItemNode<BaseRenderContext.ItemRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneNode<BaseRenderContext.ItemRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    @Getter
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.ItemRenderContext context, A packInfo) {
        renderAsItemNode(context, packInfo);
    }

    @Override
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        super.renderItemModel(context, packInfo, transform);
        //Render the linked children
        if (!linkedChildren.isEmpty()) {
            linkedChildren.forEach(c -> c.render(context, packInfo));
        }
    }

    @Override
    public void renderDebug(BaseRenderContext.ItemRenderContext context, A packInfo) {
    }

    @Override
    public SceneNode<BaseRenderContext.ItemRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.ItemRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
