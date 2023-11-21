package com.modularmods.mcgltf.animation;

import com.modularmods.mcgltf.MCglTF;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.AnimationModel.Channel;
import de.javagl.jgltf.model.AnimationModel.Interpolation;
import de.javagl.jgltf.model.AnimationModel.Sampler;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GltfAnimationCreator {

    public static List<InterpolatedChannel> createGltfAnimation(AnimationModel animationModel) {
        List<Channel> channels = animationModel.getChannels();
        List<InterpolatedChannel> interpolatedChannels = new ArrayList<>(channels.size());
        for (Channel channel : channels) {
            Sampler sampler = channel.getSampler();

            AccessorModel input = sampler.getInput();
            AccessorData inputData = input.getAccessorData();
            if (!(inputData instanceof AccessorFloatData)) {
                MCglTF.logger.warn("Input data is not an AccessorFloatData, but "
                        + inputData.getClass());
                continue;
            }
            AccessorFloatData inputFloatData = (AccessorFloatData) inputData;

            AccessorModel output = sampler.getOutput();
            AccessorData outputData = output.getAccessorData();
            if (!(outputData instanceof AccessorFloatData)) {
                MCglTF.logger.warn("Output data is not an AccessorFloatData, but "
                        + outputData.getClass());
                continue;
            }
            AccessorFloatData outputFloatData = (AccessorFloatData) outputData;

            int numKeyElements = inputFloatData.getNumElements();
            float[] keys = new float[numKeyElements];
            for (int e = 0; e < numKeyElements; e++) {
                keys[e] = inputFloatData.get(e);
            }

            int globalIndex = 0;
            int numComponentsPerElement;
            float[][] values;
            float[][][] valuesCubic;

            NodeModel nodeModel = channel.getNodeModel();

            float[] copyTranslate = nodeModel.getTranslation() != null ? Arrays.copyOf(nodeModel.getTranslation(), nodeModel.getTranslation().length) : null;
            float[] copyScale = nodeModel.getScale() != null ? Arrays.copyOf(nodeModel.getScale(), nodeModel.getScale().length) : null;
            float[] copyRotation = nodeModel.getRotation() != null ? Arrays.copyOf(nodeModel.getRotation(), nodeModel.getRotation().length) : null;

            String path = channel.getPath();
            Interpolation interpolation;
            switch (path) {
                case "translation":
                    numComponentsPerElement = outputData.getNumComponentsPerElement();
                    interpolation = sampler.getInterpolation();
                    switch (interpolation) {
                        case STEP:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getTranslation(nodeModel, numComponentsPerElement, copyTranslate);
                                }

                            });
                            break;
                        case LINEAR:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getTranslation(nodeModel, numComponentsPerElement, copyTranslate);
                                }

                            });
                            break;
                        case CUBICSPLINE:
                            valuesCubic = new float[numKeyElements][3][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[][] elements = valuesCubic[e];
                                for (int i = 0; i < 3; i++) {
                                    float[] components = elements[i];
                                    for (int c = 0; c < numComponentsPerElement; c++) {
                                        components[c] = outputFloatData.get(globalIndex++);
                                    }
                                }
                            }
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getTranslation(nodeModel, numComponentsPerElement, copyTranslate);
                                }

                            });
                            break;
                        default:
                            MCglTF.logger.warn("Interpolation type not supported: " + interpolation);
                            break;
                    }
                    break;
                case "rotation":
                    numComponentsPerElement = outputData.getNumComponentsPerElement();
                    interpolation = sampler.getInterpolation();
                    switch (interpolation) {
                        case STEP:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getRotation(nodeModel, numComponentsPerElement, copyRotation);
                                }

                            });
                            break;
                        case LINEAR:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new SphericalLinearInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getRotation(nodeModel, numComponentsPerElement, copyRotation);
                                }

                            });
                            break;
                        case CUBICSPLINE:
                            valuesCubic = new float[numKeyElements][3][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[][] elements = valuesCubic[e];
                                for (int i = 0; i < 3; i++) {
                                    float[] components = elements[i];
                                    for (int c = 0; c < numComponentsPerElement; c++) {
                                        components[c] = outputFloatData.get(globalIndex++);
                                    }
                                }
                            }
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getRotation(nodeModel, numComponentsPerElement, copyRotation);
                                }

                            });
                            break;
                        default:
                            MCglTF.logger.warn("Interpolation type not supported: " + interpolation);
                            break;
                    }
                    break;
                case "scale":
                    numComponentsPerElement = outputData.getNumComponentsPerElement();
                    interpolation = sampler.getInterpolation();
                    switch (interpolation) {
                        case STEP:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getScale(nodeModel, numComponentsPerElement, copyScale);
                                }

                            });
                            break;
                        case LINEAR:
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getScale(nodeModel, numComponentsPerElement, copyScale);
                                }

                            });
                            break;
                        case CUBICSPLINE:
                            valuesCubic = new float[numKeyElements][3][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[][] elements = valuesCubic[e];
                                for (int i = 0; i < 3; i++) {
                                    float[] components = elements[i];
                                    for (int c = 0; c < numComponentsPerElement; c++) {
                                        components[c] = outputFloatData.get(globalIndex++);
                                    }
                                }
                            }
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getScale(nodeModel, numComponentsPerElement, copyScale);
                                }

                            });
                            break;
                        default:
                            MCglTF.logger.warn("Interpolation type not supported: " + interpolation);
                            break;
                    }
                    break;
                case "weights":
                    interpolation = sampler.getInterpolation();
                    switch (interpolation) {
                        case STEP:
                            numComponentsPerElement = outputData.getTotalNumComponents() / numKeyElements;
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getWeights(nodeModel, numComponentsPerElement);
                                }

                            });
                            break;
                        case LINEAR:
                            numComponentsPerElement = outputData.getTotalNumComponents() / numKeyElements;
                            values = new float[numKeyElements][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[] components = values[e];
                                for (int c = 0; c < numComponentsPerElement; c++) {
                                    components[c] = outputFloatData.get(globalIndex++);
                                }
                            }
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getWeights(nodeModel, numComponentsPerElement);
                                }

                            });
                            break;
                        case CUBICSPLINE:
                            numComponentsPerElement = outputData.getTotalNumComponents() / numKeyElements / 3;
                            valuesCubic = new float[numKeyElements][3][numComponentsPerElement];
                            for (int e = 0; e < numKeyElements; e++) {
                                float[][] elements = valuesCubic[e];
                                for (int i = 0; i < 3; i++) {
                                    float[] components = elements[i];
                                    for (int c = 0; c < numComponentsPerElement; c++) {
                                        components[c] = outputFloatData.get(globalIndex++);
                                    }
                                }
                            }
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic, nodeModel) {

                                @Override
                                protected TransformType getListener() {
                                    return getWeights(nodeModel, numComponentsPerElement);
                                }

                            });
                            break;
                        default:
                            MCglTF.logger.warn("Interpolation type not supported: " + interpolation);
                            break;
                    }
                    break;
                default:
                    MCglTF.logger.warn("Animation channel target path must be "
                            + "\"translation\", \"rotation\", \"scale\" or  \"weights\", "
                            + "but is " + path);
                    break;
            }
        }
        return interpolatedChannels;
    }

    private static InterpolatedChannel.TransformType getTranslation(NodeModel nodeModel, int numComponentsPerElement, float[] copyTranslate) {
        float[] translation = copyTranslate;
        if (translation == null) {
            translation = new float[numComponentsPerElement];
            nodeModel.setTranslation(translation);
        }
        return new InterpolatedChannel.TransformType(translation, copyTranslate, GltfModelRenderer.EnumTransformType.TRANSLATION);
    }

    private static InterpolatedChannel.TransformType getRotation(NodeModel nodeModel, int numComponentsPerElement, float[] copyRotation) {
        float[] rotation = copyRotation;
        if (rotation == null) {
            rotation = new float[numComponentsPerElement];
            nodeModel.setRotation(rotation);
        }
        return new InterpolatedChannel.TransformType(rotation, copyRotation, GltfModelRenderer.EnumTransformType.ROTATION);
    }

    private static InterpolatedChannel.TransformType getScale(NodeModel nodeModel, int numComponentsPerElement, float[] copyScale) {
        float[] scale = copyScale;
        if (scale == null) {
            scale = new float[]{1,1,1};
            nodeModel.setScale(scale);
        }
        return new InterpolatedChannel.TransformType(scale, scale.clone(), GltfModelRenderer.EnumTransformType.SCALE);
    }

    private static InterpolatedChannel.TransformType getWeights(NodeModel nodeModel, int numComponentsPerElement) {
        float[] weights = nodeModel.getWeights();
        if (weights == null) {
            weights = new float[numComponentsPerElement];
            nodeModel.setWeights(weights);
        }
        return new InterpolatedChannel.TransformType(weights, weights.clone(), GltfModelRenderer.EnumTransformType.WEIGHTS);
    }
}
