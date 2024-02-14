package com.modularmods.mcgltf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;

public class RenderedGltfModelGL33 extends RenderedGltfModelGL40 {

	public RenderedGltfModelGL33(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		super(gltfRenderData, gltfModel);
	}
	
	@Override
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfScene renderedGltfScene = new RenderedGltfSceneGL33();
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
	protected void processMeshPrimitiveModelIncludedTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel) {
		int glVertexArraySkinning = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArraySkinning));
		GL30.glBindVertexArray(glVertexArraySkinning);
		
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		bindArrayBufferViewModel(gltfRenderData, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_joint,
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_joint);
		
		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		bindArrayBufferViewModel(gltfRenderData, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_weight,
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_weight);
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_position,
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_position);
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_normal,
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_normal);
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "TANGENT")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_tangent);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
			
			GL30.glBeginTransformFeedback(GL11.GL_POINTS);
			GL30.glBindVertexArray(glVertexArraySkinning);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
			GL30.glEndTransformFeedback();
		});
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL11.glVertexPointer(positionsAccessorModel.getElementType().getNumComponents(), positionsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL11.glNormalPointer(normalsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(at_tangent);
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel());
			}
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
			}
			GL11.glTexCoordPointer(
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			
			AccessorModel texcoords1AccessorModel = attributes.get("TEXCOORD_1");
			if(texcoords1AccessorModel != null) {
				texcoordsAccessorModel = texcoords1AccessorModel;
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
				}
			}
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, pointCount);
			});
		}
	}
	
	protected void processMeshPrimitiveModelSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel) {
		int glVertexArraySkinning = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArraySkinning));
		GL30.glBindVertexArray(glVertexArraySkinning);
		
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		bindArrayBufferViewModel(gltfRenderData, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_joint,
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_joint);
		
		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		bindArrayBufferViewModel(gltfRenderData, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_weight,
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_weight);
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_position,
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_position);
		
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createNormalTangentMorphTarget(morphTargets, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, tangentTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, normalsAccessorModel, targetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_tangent);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_tangent);
		}
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
			
			GL30.glBeginTransformFeedback(GL11.GL_POINTS);
			GL30.glBindVertexArray(glVertexArraySkinning);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
			GL30.glEndTransformFeedback();
		});
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL11.glVertexPointer(positionsAccessorModel.getElementType().getNumComponents(), positionsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL11.glNormalPointer(normalsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(at_tangent);
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel());
			}
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
			}
			GL11.glTexCoordPointer(
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			
			AccessorModel texcoords1AccessorModel = attributes.get("TEXCOORD_1");
			if(texcoords1AccessorModel != null) {
				texcoordsAccessorModel = texcoords1AccessorModel;
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
				}
			}
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, pointCount);
			});
		}
	}
	
	@Override
	protected void processMeshPrimitiveModelMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glVertexArraySkinning = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArraySkinning));
		GL30.glBindVertexArray(glVertexArraySkinning);
		
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		bindArrayBufferViewModel(gltfRenderData, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_joint,
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_joint);
		
		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		bindArrayBufferViewModel(gltfRenderData, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_weight,
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_weight);
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_position,
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_position);
		
		AccessorModel normalsAccessorModel = attributes.get("NORMAL");
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_normal,
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_normal);
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(meshPrimitiveModel, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_tangent);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
			
			GL30.glBeginTransformFeedback(GL11.GL_POINTS);
			GL30.glBindVertexArray(glVertexArraySkinning);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
			GL30.glEndTransformFeedback();
		});
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL11.glVertexPointer(positionsAccessorModel.getElementType().getNumComponents(), positionsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL11.glNormalPointer(normalsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(at_tangent);
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel());
			}
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
		}
		GL11.glTexCoordPointer(
				texcoordsAccessorModel.getElementType().getNumComponents(),
				texcoordsAccessorModel.getComponentType(),
				texcoordsAccessorModel.getByteStride(),
				texcoordsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		
		AccessorModel texcoords1AccessorModel = attributes.get("TEXCOORD_1");
		if(texcoords1AccessorModel != null) {
			texcoordsAccessorModel = texcoords1AccessorModel;
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
			}
		}
		GL20.glVertexAttribPointer(
				mc_midTexCoord,
				texcoordsAccessorModel.getElementType().getNumComponents(),
				texcoordsAccessorModel.getComponentType(),
				false,
				texcoordsAccessorModel.getByteStride(),
				texcoordsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(mc_midTexCoord);
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glVertexArraySkinning = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArraySkinning));
		GL30.glBindVertexArray(glVertexArraySkinning);
		
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		bindArrayBufferViewModel(gltfRenderData, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_joint,
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_joint);
		
		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		bindArrayBufferViewModel(gltfRenderData, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_weight,
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_weight);
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalTangentMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas, tangentTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, positionsAccessorModel, targetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_position,
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					false,
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_position);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, normalsAccessorModel, normalTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_tangent);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_position,
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					false,
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_position);
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_tangent);
		}
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
			
			GL30.glBeginTransformFeedback(GL11.GL_POINTS);
			GL30.glBindVertexArray(glVertexArraySkinning);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
			GL30.glEndTransformFeedback();
		});
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL11.glVertexPointer(positionsAccessorModel.getElementType().getNumComponents(), positionsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL11.glNormalPointer(normalsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(at_tangent);
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel());
			}
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
			}
			GL11.glTexCoordPointer(
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			
			AccessorModel texcoords1AccessorModel = attributes.get("TEXCOORD_1");
			if(texcoords1AccessorModel != null) {
				texcoordsAccessorModel = texcoords1AccessorModel;
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
				}
			}
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		}
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glVertexArraySkinning = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArraySkinning));
		GL30.glBindVertexArray(glVertexArraySkinning);
		
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		bindArrayBufferViewModel(gltfRenderData, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_joint,
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_joint);
		
		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		bindArrayBufferViewModel(gltfRenderData, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				skinning_weight,
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_weight);
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, positionsAccessorModel, targetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_position,
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					false,
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_position);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, normalsAccessorModel, normalTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_position,
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					false,
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_position);
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					skinning_normal,
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(skinning_normal);
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, skinningCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				skinning_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(skinning_tangent);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, positionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
			GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
			
			GL30.glBeginTransformFeedback(GL11.GL_POINTS);
			GL30.glBindVertexArray(glVertexArraySkinning);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
			GL30.glEndTransformFeedback();
		});
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL11.glVertexPointer(positionsAccessorModel.getElementType().getNumComponents(), positionsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL11.glNormalPointer(normalsAccessorModel.getComponentType(), 0, 0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(at_tangent);
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel());
			}
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
		}
		GL11.glTexCoordPointer(
				texcoordsAccessorModel.getElementType().getNumComponents(),
				texcoordsAccessorModel.getComponentType(),
				texcoordsAccessorModel.getByteStride(),
				texcoordsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		
		AccessorModel texcoords1AccessorModel = attributes.get("TEXCOORD_1");
		if(texcoords1AccessorModel != null) {
			texcoordsAccessorModel = texcoords1AccessorModel;
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel());
			}
		}
		GL20.glVertexAttribPointer(
				mc_midTexCoord,
				texcoordsAccessorModel.getElementType().getNumComponents(),
				texcoordsAccessorModel.getComponentType(),
				false,
				texcoordsAccessorModel.getByteStride(),
				texcoordsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(mc_midTexCoord);
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}

}
