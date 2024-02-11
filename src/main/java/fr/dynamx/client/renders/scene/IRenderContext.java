package fr.dynamx.client.renders.scene;

import fr.dynamx.client.renders.model.renderer.DxModelRenderer;

/**
 * The context used when rendering {@link fr.dynamx.client.renders.scene.node.SceneNode}s of a scene graph
 */
public interface IRenderContext {
    /**
     * @return The model being rendered
     */
    DxModelRenderer getModel();

    /**
     * @return False to render the model in the game world, true otherwise
     */
    boolean isUseVanillaRender();

    /**
     * @return The partial ticks of the render
     */
    float getPartialTicks();

    /**
     * @return The texture id of the model (determines the {@link fr.dynamx.common.contentpack.type.MaterialVariantsInfo} being used)
     */
    byte getTextureId();
}
