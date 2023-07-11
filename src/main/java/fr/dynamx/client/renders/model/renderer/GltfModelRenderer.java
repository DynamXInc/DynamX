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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        if (scene == null) return;
        renderVanillaOrShader(-1, forceVanillaRender);
    }

    @Override
    public void renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        if (scene == null) return;
        int nodeModelIndex = getNodeModelIndex(group);
        renderVanillaOrShader(nodeModelIndex, forceVanillaRender);
    }

    @Override
    public boolean renderGroups(String group, byte textureDataId, boolean forceVanillaRender) {
        if (scene == null) return false;
        int nodeModelIndex = getNodeModelIndex(group);
        renderVanillaOrShader(nodeModelIndex, forceVanillaRender);
        return nodeModelIndex != -1;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        boolean drawn = false;
        for (NodeModel object : nodeModels) {
            if (getTextureVariants().canRenderPart(object.getName())) {
                renderGroup(object.getName(), textureDataId, forceVanillaRender);
                drawn = true;
            }
        }
        return drawn;
    }

    public void renderVanillaOrShader(int nodeModelIndex, boolean forceVanillaRender) {

        if (forceVanillaRender) {
            scene.renderForVanilla(nodeModelIndex);
        } else if (MCglTF.getInstance().isShaderModActive()){
            scene.renderForShaderMod(nodeModelIndex);
        }

    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return nodeModels.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
    }

    public int getNodeModelIndex(String objectName) {
        return IntStream.range(0, nodeModels.size())
                .filter(i -> nodeModels.get(i).getName().equalsIgnoreCase(objectName))
                .findFirst()
                .orElse(-1);
    }

    @Override
    public boolean isEmpty() {
        return nodeModels.isEmpty();
    }

    @Override
    public DxModelPath getModelLocation() {
        return location;
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
