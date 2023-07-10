package com.modularmods.mcgltf.animation;

import com.modularmods.mcgltf.MCglTF;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.AnimationModel.Channel;
import de.javagl.jgltf.model.AnimationModel.Interpolation;
import de.javagl.jgltf.model.AnimationModel.Sampler;

import java.util.ArrayList;
import java.util.List;

public final class GltfAnimationCreator {

    public static List<InterpolatedChannel> createGltfAnimation(AnimationModel animationModel) {
        List<Channel> channels = animationModel.getChannels();
        List<InterpolatedChannel> interpolatedChannels = new ArrayList<InterpolatedChannel>(channels.size());
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
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] translation = nodeModel.getTranslation();
                                    if (translation == null) {
                                        translation = new float[numComponentsPerElement];
                                        nodeModel.setTranslation(translation);
                                    }
                                    return translation;
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
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] translation = nodeModel.getTranslation();
                                    if (translation == null) {
                                        translation = new float[numComponentsPerElement];
                                        nodeModel.setTranslation(translation);
                                    }
                                    return translation;
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
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic) {

                                @Override
                                protected float[] getListener() {
                                    float[] translation = nodeModel.getTranslation();
                                    if (translation == null) {
                                        translation = new float[numComponentsPerElement];
                                        nodeModel.setTranslation(translation);
                                    }
                                    return translation;
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
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] rotation = nodeModel.getRotation();
                                    if (rotation == null) {
                                        rotation = new float[numComponentsPerElement];
                                        nodeModel.setRotation(rotation);
                                    }
                                    return rotation;
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
                            interpolatedChannels.add(new SphericalLinearInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] rotation = nodeModel.getRotation();
                                    if (rotation == null) {
                                        rotation = new float[numComponentsPerElement];
                                        nodeModel.setRotation(rotation);
                                    }
                                    return rotation;
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
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic) {

                                @Override
                                protected float[] getListener() {
                                    float[] rotation = nodeModel.getRotation();
                                    if (rotation == null) {
                                        rotation = new float[numComponentsPerElement];
                                        nodeModel.setRotation(rotation);
                                    }
                                    return rotation;
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
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] scale = nodeModel.getScale();
                                    if (scale == null) {
                                        scale = new float[numComponentsPerElement];
                                        nodeModel.setScale(scale);
                                    }
                                    return scale;
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
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] scale = nodeModel.getScale();
                                    if (scale == null) {
                                        scale = new float[numComponentsPerElement];
                                        nodeModel.setScale(scale);
                                    }
                                    return scale;
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
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic) {

                                @Override
                                protected float[] getListener() {
                                    float[] scale = nodeModel.getScale();
                                    if (scale == null) {
                                        scale = new float[numComponentsPerElement];
                                        nodeModel.setScale(scale);
                                    }
                                    return scale;
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
                            interpolatedChannels.add(new StepInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] weights = nodeModel.getWeights();
                                    if (weights == null) {
                                        weights = new float[numComponentsPerElement];
                                        nodeModel.setWeights(weights);
                                    }
                                    return weights;
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
                            interpolatedChannels.add(new LinearInterpolatedChannel(keys, values) {

                                @Override
                                protected float[] getListener() {
                                    float[] weights = nodeModel.getWeights();
                                    if (weights == null) {
                                        weights = new float[numComponentsPerElement];
                                        nodeModel.setWeights(weights);
                                    }
                                    return weights;
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
                            interpolatedChannels.add(new CubicSplineInterpolatedChannel(keys, valuesCubic) {

                                @Override
                                protected float[] getListener() {
                                    float[] weights = nodeModel.getWeights();
                                    if (weights == null) {
                                        weights = new float[numComponentsPerElement];
                                        nodeModel.setWeights(weights);
                                    }
                                    return weights;
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
}
