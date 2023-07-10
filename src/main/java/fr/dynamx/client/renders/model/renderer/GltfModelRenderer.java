package fr.dynamx.client.renders.model.renderer;

import com.modularmods.mcgltf.IGltfModelReceiver;
import com.modularmods.mcgltf.MCglTF;
import com.modularmods.mcgltf.RenderedGltfModel;
import com.modularmods.mcgltf.RenderedGltfScene;
import com.modularmods.mcgltf.animation.GltfAnimationCreator;
import com.modularmods.mcgltf.animation.InterpolatedChannel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.NodeModel;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfModelRenderer extends DxModelRenderer implements IGltfModelReceiver {

    private RenderedGltfScene scene;
    private List<NodeModel> nodeModels;
    public Map<String, List<InterpolatedChannel>> animations;
    public float time;


    public GltfModelRenderer(DxModelPath location, IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        MCglTF.getInstance().addGltfModelReceiver(this);
    }

    @Override
    public void renderModel(byte textureDataId) {
        if (scene == null) return;
        if (MCglTF.getInstance().isShaderModActive()) {
            scene.renderForShaderMod();
        } else {
            scene.renderForVanilla();
        }
    }

    public void renderModelVanilla(byte textureDataId) {
        if (scene == null) return;
        scene.renderForVanilla();
    }

    @Override
    public void renderGroup(String group, byte textureDataId) {

    }

    @Override
    public boolean renderGroups(String group, byte textureDataId) {
        return false;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId) {
        return false;
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return nodeModels.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
    }

    @Override
    public boolean isEmpty() {
        return nodeModels.isEmpty();
    }

    @Override
    public ResourceLocation getModelLocation() {
        return location.getModelPath();
    }

    @Override
    public void onReceiveSharedModel(RenderedGltfModel renderedModel) {
        scene = renderedModel.renderedGltfScenes.get(0);
        nodeModels = renderedModel.gltfModel.getNodeModels();
        List<AnimationModel> animationModels = renderedModel.gltfModel.getAnimationModels();
        animations = new HashMap<>(animationModels.size());
        for (AnimationModel animationModel : animationModels) {
            animations.put(animationModel.getName(), GltfAnimationCreator.createGltfAnimation(animationModel));
        }

    }

}
