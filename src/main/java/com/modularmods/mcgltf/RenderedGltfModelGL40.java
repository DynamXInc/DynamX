package com.modularmods.mcgltf;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MathUtils;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;

public class RenderedGltfModelGL40 extends RenderedGltfModel {

	public RenderedGltfModelGL40(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		super(gltfRenderData, gltfModel);
	}
	
	@Override
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfScene renderedGltfScene = new RenderedGltfSceneGL40();
			renderedGltfScenes.add(renderedGltfScene);
			
			for(NodeModel nodeModel : sceneModel.getNodeModels()) {
				Triple<List<Runnable>, List<Runnable>, List<Runnable>> commands = rootNodeModelToCommands.get(nodeModel);
				List<Runnable> rootSkinningCommands;
				List<Runnable> vanillaRootRenderCommands;
				List<Runnable> shaderModRootRenderCommands;
				if(commands == null) {
					rootSkinningCommands = new ArrayList<Runnable>();
					vanillaRootRenderCommands = new ArrayList<Runnable>();
					shaderModRootRenderCommands = new ArrayList<Runnable>();
					processNodeModel(gltfRenderData, nodeModel, rootSkinningCommands, vanillaRootRenderCommands, shaderModRootRenderCommands);
					rootNodeModelToCommands.put(nodeModel, Triple.of(rootSkinningCommands, vanillaRootRenderCommands, shaderModRootRenderCommands));
				}
				else {
					rootSkinningCommands = commands.getLeft();
					vanillaRootRenderCommands = commands.getMiddle();
					shaderModRootRenderCommands = commands.getRight();
				}
				renderedGltfScene.skinningCommands.addAll(rootSkinningCommands);
				renderedGltfScene.vanillaRenderCommands.addAll(vanillaRootRenderCommands);
				renderedGltfScene.shaderModRenderCommands.addAll(shaderModRootRenderCommands);
			}
		}
	}
	
	@Override
	protected void processNodeModel(List<Runnable> gltfRenderData, NodeModel nodeModel, List<Runnable> skinningCommands, List<Runnable> vanillaRenderCommands, List<Runnable> shaderModRenderCommands) {
		ArrayList<Runnable> nodeSkinningCommands = new ArrayList<Runnable>();
		ArrayList<Runnable> vanillaNodeRenderCommands = new ArrayList<Runnable>();
		ArrayList<Runnable> shaderModNodeRenderCommands = new ArrayList<Runnable>();
		SkinModel skinModel = nodeModel.getSkinModel();
		if(skinModel != null) {
			boolean canHaveHardwareSkinning;
			checkHardwareSkinning: {
				for(MeshModel meshModel : nodeModel.getMeshModels()) {
					for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
						if(!meshPrimitiveModel.getAttributes().containsKey("JOINTS_1")) {
							canHaveHardwareSkinning = true;
							break checkHardwareSkinning;
						}
					}
				}
				canHaveHardwareSkinning = false;
			}
			
			int jointCount = skinModel.getJoints().size();
			
			float[][] transforms = new float[jointCount][];
			float[] invertNodeTransform = new float[16];
			float[] bindShapeMatrix = new float[16];
			
			if(canHaveHardwareSkinning) {
				int jointMatrixSize = jointCount * 16;
				float[] jointMatrices = new float[jointMatrixSize];
				
				int jointMatrixBuffer = GL15.glGenBuffers();
				gltfRenderData.add(() -> GL15.glDeleteBuffers(jointMatrixBuffer));
				GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, jointMatrixBuffer);
				GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, jointMatrixSize * Float.BYTES, GL15.GL_STATIC_DRAW);
				int glTexture = GL11.glGenTextures();
				gltfRenderData.add(() -> GL11.glDeleteTextures(glTexture));
				GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, glTexture);
				GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, jointMatrixBuffer);
				
				List<Runnable> jointMatricesTransformCommands = new ArrayList<Runnable>(jointCount);
				for(int joint = 0; joint < jointCount; joint++) {
					int i = joint;
					float[] transform = transforms[i] = new float[16];
					float[] inverseBindMatrix = new float[16];
					jointMatricesTransformCommands.add(() -> {
						MathUtils.mul4x4(invertNodeTransform, transform, transform);
						skinModel.getInverseBindMatrix(i, inverseBindMatrix);
						MathUtils.mul4x4(transform, inverseBindMatrix, transform);
						MathUtils.mul4x4(transform, bindShapeMatrix, transform);
						System.arraycopy(transform, 0, jointMatrices, i * 16, 16);
					});
				}
				
				nodeSkinningCommands.add(() -> {
					for(int i = 0; i < transforms.length; i++) {
						System.arraycopy(findGlobalTransform(skinModel.getJoints().get(i)), 0, transforms[i], 0, 16);
					}
					MathUtils.invert4x4(findGlobalTransform(nodeModel), invertNodeTransform);
					skinModel.getBindShapeMatrix(bindShapeMatrix);
					jointMatricesTransformCommands.parallelStream().forEach(Runnable::run);
					
					GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, jointMatrixBuffer);
					GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, putFloatBuffer(jointMatrices));
					
					GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, glTexture);
				});
				
				Runnable transformCommand = createTransformCommand(nodeModel);
				vanillaNodeRenderCommands.add(transformCommand);
				shaderModNodeRenderCommands.add(transformCommand);
				for(MeshModel meshModel : nodeModel.getMeshModels()) {
					for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
						processMeshPrimitiveModel(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, transforms, nodeSkinningCommands, vanillaNodeRenderCommands, shaderModNodeRenderCommands);
					}
				}
				vanillaNodeRenderCommands.add(GL11::glPopMatrix);
				shaderModNodeRenderCommands.add(GL11::glPopMatrix);
			}
			else {
				List<Runnable> jointMatricesTransformCommands = new ArrayList<Runnable>(jointCount);
				for(int joint = 0; joint < jointCount; joint++) {
					int i = joint;
					float[] transform = transforms[i] = new float[16];
					float[] inverseBindMatrix = new float[16];
					jointMatricesTransformCommands.add(() -> {
						MathUtils.mul4x4(invertNodeTransform, transform, transform);
						skinModel.getInverseBindMatrix(i, inverseBindMatrix);
						MathUtils.mul4x4(transform, inverseBindMatrix, transform);
						MathUtils.mul4x4(transform, bindShapeMatrix, transform);
					});
				}
				
				Runnable jointMatricesTransformCommand = () -> {
					for(int i = 0; i < transforms.length; i++) {
						System.arraycopy(findGlobalTransform(skinModel.getJoints().get(i)), 0, transforms[i], 0, 16);
					}
					MathUtils.invert4x4(findGlobalTransform(nodeModel), invertNodeTransform);
					skinModel.getBindShapeMatrix(bindShapeMatrix);
					jointMatricesTransformCommands.parallelStream().forEach(Runnable::run);
				};
				vanillaNodeRenderCommands.add(jointMatricesTransformCommand);
				shaderModNodeRenderCommands.add(jointMatricesTransformCommand);
				
				Runnable transformCommand = createTransformCommand(nodeModel);
				vanillaNodeRenderCommands.add(transformCommand);
				shaderModNodeRenderCommands.add(transformCommand);
				for(MeshModel meshModel : nodeModel.getMeshModels()) {
					for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
						processMeshPrimitiveModel(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, transforms, vanillaNodeRenderCommands, shaderModNodeRenderCommands);
					}
				}
				vanillaNodeRenderCommands.add(GL11::glPopMatrix);
				shaderModNodeRenderCommands.add(GL11::glPopMatrix);
			}
		}
		else {
			if(!nodeModel.getMeshModels().isEmpty()) {
				Runnable transformCommand = createTransformCommand(nodeModel);
				vanillaNodeRenderCommands.add(transformCommand);
				shaderModNodeRenderCommands.add(transformCommand);
				for(MeshModel meshModel : nodeModel.getMeshModels()) {
					for(MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
						processMeshPrimitiveModel(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, vanillaNodeRenderCommands, shaderModNodeRenderCommands);
					}
				}
				vanillaNodeRenderCommands.add(GL11::glPopMatrix);
				shaderModNodeRenderCommands.add(GL11::glPopMatrix);
			}
		}
		nodeModel.getChildren().forEach((childNode) -> processNodeModel(gltfRenderData, childNode, nodeSkinningCommands, vanillaNodeRenderCommands, shaderModNodeRenderCommands));
		if(!nodeSkinningCommands.isEmpty()) {
			// Zero-scale meshes visibility optimization
			// https://github.com/KhronosGroup/glTF/pull/2059
			skinningCommands.add(() -> {
				float[] scale = nodeModel.getScale();
				if(scale == null || scale[0] != 0.0F || scale[1] != 0.0F || scale[2] != 0.0F) {
					nodeSkinningCommands.forEach(Runnable::run);
				}
			});
		}
		if(!vanillaNodeRenderCommands.isEmpty()) {
			vanillaRenderCommands.add(() -> {
				float[] scale = nodeModel.getScale();
				if(scale == null || scale[0] != 0.0F || scale[1] != 0.0F || scale[2] != 0.0F) {
					vanillaNodeRenderCommands.forEach(Runnable::run);
				}
			});
			shaderModRenderCommands.add(() -> {
				float[] scale = nodeModel.getScale();
				if(scale == null || scale[0] != 0.0F || scale[1] != 0.0F || scale[2] != 0.0F) {
					shaderModNodeRenderCommands.forEach(Runnable::run);
				}
			});
		}
	}

}
