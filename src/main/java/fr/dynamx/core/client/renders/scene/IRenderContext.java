package fr.dynamx.core.client.renders.scene;

import fr.dynamx.core.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.core.client.renders.scene.node.SceneNode;
import fr.dynamx.core.common.contentpack.type.MaterialVariantsInfo;

/**
 * The context used when rendering {@link SceneNode}s of a scene graph
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
     * @return The texture id of the model (determines the {@link MaterialVariantsInfo} being used)
     */
    byte getTextureId();
}
