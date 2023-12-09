package fr.dynamx.client.renders.model.renderer;

import com.jme3.math.Quaternion;
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
import fr.dynamx.client.renders.animations.DxAnimation;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class GltfModelRenderer extends DxModelRenderer implements IGltfModelReceiver {

    private RenderedGltfScene scene;
    public List<NodeModel> nodeModels;
    public HashMap<String, List<InterpolatedChannel>> animations;
    public List<AnimationModel> animationModels;
    public float time;
    public DxAnimation animation;

    public Map<NodeModel, Transform> initialNodeTransforms = new HashMap<>();


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
        } else if (MCglTF.getInstance().isShaderModActive()) {
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

    public void resetModel(float partialTicks) {
        for (Map.Entry<NodeModel, Transform> entry : initialNodeTransforms.entrySet()) {
            NodeModel node = entry.getKey();
            Transform initialTransform = entry.getValue();

            resetNodeModel(node, initialTransform, partialTicks);
        }
    }

    public void resetNodeModel(NodeModel nodeModel, Transform initialTransform, float partialTicks) {
        blendInitialPose(nodeModel, initialTransform.translation, EnumTransformType.TRANSLATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.rotation, EnumTransformType.ROTATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.scale, EnumTransformType.SCALE, partialTicks);
        blendInitialPose(nodeModel, initialTransform.weights, EnumTransformType.WEIGHTS, partialTicks);
    }

    public void blendInitialPose(NodeModel nodeModel, float[] animation, EnumTransformType type, float partialTicks) {
        float x, y, z, w;
        switch (type) {
            case TRANSLATION:
                if(nodeModel.getTranslation() == null) break;
                x = DynamXMath.interpolateLinear(partialTicks, nodeModel.getTranslation()[0], animation[0]);
                y = DynamXMath.interpolateLinear(partialTicks, nodeModel.getTranslation()[1], animation[1]);
                z = DynamXMath.interpolateLinear(partialTicks, nodeModel.getTranslation()[2], animation[2]);
                nodeModel.getTranslation()[0] = x;
                nodeModel.getTranslation()[1] = y;
                nodeModel.getTranslation()[2] = z;
                break;
            case ROTATION:
                if(nodeModel.getRotation() == null) break;

                x = animation[0];
                y = animation[1];
                z = animation[2];
                w = animation[3];
                Quaternion quaternionInitial = QuaternionPool.get(x, y, z, w);
                Quaternion quaternionCurrent = QuaternionPool.get(nodeModel.getRotation()[0],
                        nodeModel.getRotation()[1], nodeModel.getRotation()[2], nodeModel.getRotation()[3]);
                Quaternion quaternion = DynamXMath.slerp(partialTicks, quaternionCurrent, quaternionInitial);
                nodeModel.getRotation()[0] = quaternion.getX();
                nodeModel.getRotation()[1] = quaternion.getY();
                nodeModel.getRotation()[2] = quaternion.getZ();
                nodeModel.getRotation()[3] = quaternion.getW();
                break;
            case SCALE:
                if(nodeModel.getScale() == null) break;

                x = DynamXMath.interpolateLinear(partialTicks, nodeModel.getScale()[0], animation[0]);
                y = DynamXMath.interpolateLinear(partialTicks, nodeModel.getScale()[1], animation[1]);
                z = DynamXMath.interpolateLinear(partialTicks, nodeModel.getScale()[2], animation[2]);
                nodeModel.getScale()[0] = x;
                nodeModel.getScale()[1] = y;
                nodeModel.getScale()[2] = z;
                break;
            case WEIGHTS:
                if(nodeModel.getWeights() == null) break;

                for (int i = 0; i < nodeModel.getWeights().length; i++) {
                    nodeModel.getWeights()[i] = DynamXMath.interpolateLinear(partialTicks, nodeModel.getWeights()[i], animation[i]);
                }
                break;
        }
    }

    @Override
    public DxModelPath getModelLocation() {
        return location;
    }

    @Override
    public void onReceiveSharedModel(RenderedGltfModel renderedModel) {
        scene = renderedModel.renderedGltfScenes.get(0);
        nodeModels = renderedModel.gltfModel.getNodeModels();
        animationModels = renderedModel.gltfModel.getAnimationModels();
        animations = new HashMap<>(animationModels.size());
        for (AnimationModel animationModel : animationModels) {
            animations.put(animationModel.getName(), GltfAnimationCreator.createGltfAnimation(animationModel));
        }

        for (NodeModel node : nodeModels) {
            Transform transform = new Transform(
                    node.getTranslation(), node.getRotation(), node.getScale(), node.getWeights());
            initialNodeTransforms.put(node, transform);
        }

    }

    public static class Transform {
        public float[] translation;
        public float[] rotation;
        public float[] scale;
        public float[] weights;

        public Transform(float[] translation, float[] rotation, float[] scale, float[] weights) {
            this.translation = translation != null ? translation.clone() : null;
            this.rotation = rotation != null ? rotation.clone() : null;
            this.scale = scale != null ? scale.clone() : new float[]{1, 1, 1};
            this.weights = weights != null ? weights.clone() : null;
        }
    }

    public enum EnumTransformType {
        TRANSLATION, ROTATION, SCALE, WEIGHTS
    }
}
