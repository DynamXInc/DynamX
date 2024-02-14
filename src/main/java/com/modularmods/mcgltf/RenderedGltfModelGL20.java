package com.modularmods.mcgltf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;

public class RenderedGltfModelGL20 extends RenderedGltfModelGL30 {

	public RenderedGltfModelGL20(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		super(gltfRenderData, gltfModel);
	}
	
	@Override
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfScene renderedGltfScene = new RenderedGltfSceneGL20();
			renderedGltfScenes.add(renderedGltfScene);
			
			for(NodeModel nodeModel : sceneModel.getNodeModels()) {
				Triple<List<Runnable>, List<Runnable>, List<Runnable>> commands = rootNodeModelToCommands.get(nodeModel);
				List<Runnable> vanillaRootRenderCommands;
				List<Runnable> shaderModRootRenderCommands;
				if(commands == null) {
					vanillaRootRenderCommands = new ArrayList<Runnable>();
					shaderModRootRenderCommands = new ArrayList<Runnable>();
					processNodeModel(gltfRenderData, nodeModel, vanillaRootRenderCommands, shaderModRootRenderCommands);
					rootNodeModelToCommands.put(nodeModel, Triple.of(null, vanillaRootRenderCommands, shaderModRootRenderCommands));
				}
				else {
					vanillaRootRenderCommands = commands.getMiddle();
					shaderModRootRenderCommands = commands.getRight();
				}
				renderedGltfScene.vanillaRenderCommands.addAll(vanillaRootRenderCommands);
				renderedGltfScene.shaderModRenderCommands.addAll(shaderModRootRenderCommands);
			}
		}
	}
	
	@Override
	protected void processMeshPrimitiveModelIncludedTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel) {
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		});
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		});
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "TANGENT")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});

		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			int count = positionsAccessorModel.getCount();
			renderCommand.add(() -> GL11.glDrawArrays(mode, 0, count));
		}
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel) {
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		});
		
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createNormalTangentMorphTarget(morphTargets, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, tangentTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						at_tangent,
						tangentsAccessorModel.getElementType().getNumComponents(),
						tangentsAccessorModel.getComponentType(),
						false,
						tangentsAccessorModel.getByteStride(),
						tangentsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(at_tangent);
			});
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						at_tangent,
						tangentsAccessorModel.getElementType().getNumComponents(),
						tangentsAccessorModel.getComponentType(),
						false,
						tangentsAccessorModel.getByteStride(),
						tangentsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(at_tangent);
			});
		}
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			int count = positionsAccessorModel.getCount();
			renderCommand.add(() -> GL11.glDrawArrays(mode, 0, count));
		}
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		});
		
		AccessorModel normalsAccessorModel = attributes.get("NORMAL");
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		});
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(meshPrimitiveModel, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
		}
		AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
		renderCommand.add(() -> {
			GL11.glTexCoordPointer(
					texcoordsAccessorModelFinal.getElementType().getNumComponents(),
					texcoordsAccessorModelFinal.getComponentType(),
					texcoordsAccessorModelFinal.getByteStride(),
					texcoordsAccessorModelFinal.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		});
		
		AccessorModel texcoords1AccessorModelFinal;
		texcoordsAccessorModel = attributes.get("TEXCOORD_1");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			texcoords1AccessorModelFinal = texcoordsAccessorModel;
		}
		else {
			texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoords1AccessorModelFinal.getElementType().getNumComponents(),
					texcoords1AccessorModelFinal.getComponentType(),
					false,
					texcoords1AccessorModelFinal.getByteStride(),
					texcoords1AccessorModelFinal.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		});
		
		int mode = meshPrimitiveModel.getMode();
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, count));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalTangentMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas, tangentTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
			renderCommand.add(() -> {
				GL11.glVertexPointer(
						positionsAccessorModel.getElementType().getNumComponents(),
						positionsAccessorModel.getComponentType(),
						positionsAccessorModel.getByteStride(),
						positionsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			});
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, normalTargetAccessorDatas);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						at_tangent,
						tangentsAccessorModel.getElementType().getNumComponents(),
						tangentsAccessorModel.getComponentType(),
						false,
						tangentsAccessorModel.getByteStride(),
						tangentsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(at_tangent);
			});
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL11.glVertexPointer(
						positionsAccessorModel.getElementType().getNumComponents(),
						positionsAccessorModel.getComponentType(),
						positionsAccessorModel.getByteStride(),
						positionsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			});
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						at_tangent,
						tangentsAccessorModel.getElementType().getNumComponents(),
						tangentsAccessorModel.getComponentType(),
						false,
						tangentsAccessorModel.getByteStride(),
						tangentsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(at_tangent);
			});
		}

		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, count));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
			renderCommand.add(() -> {
				GL11.glVertexPointer(
						positionsAccessorModel.getElementType().getNumComponents(),
						positionsAccessorModel.getComponentType(),
						positionsAccessorModel.getByteStride(),
						positionsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			});
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, normalTargetAccessorDatas);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL11.glVertexPointer(
						positionsAccessorModel.getElementType().getNumComponents(),
						positionsAccessorModel.getComponentType(),
						positionsAccessorModel.getByteStride(),
						positionsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			});
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel(), renderCommand);
			renderCommand.add(() -> {
				GL11.glNormalPointer(
						normalsAccessorModel.getComponentType(),
						normalsAccessorModel.getByteStride(),
						normalsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel(), renderCommand);
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
		}
		AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
		renderCommand.add(() -> {
			GL11.glTexCoordPointer(
					texcoordsAccessorModelFinal.getElementType().getNumComponents(),
					texcoordsAccessorModelFinal.getComponentType(),
					texcoordsAccessorModelFinal.getByteStride(),
					texcoordsAccessorModelFinal.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		});
		
		AccessorModel texcoords1AccessorModelFinal;
		texcoordsAccessorModel = attributes.get("TEXCOORD_1");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			texcoords1AccessorModelFinal = texcoordsAccessorModel;
		}
		else {
			texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoords1AccessorModelFinal.getElementType().getNumComponents(),
					texcoords1AccessorModelFinal.getComponentType(),
					false,
					texcoords1AccessorModelFinal.getByteStride(),
					texcoords1AccessorModelFinal.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		});
		
		int mode = meshPrimitiveModel.getMode();
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, count));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelIncludedTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, float[][] jointMatrices, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel) {
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		AccessorModel outputPositionsAccessorModel;
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			outputPositionsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			outputPositionsAccessorModel = AccessorModelCreation.instantiate(positionsAccessorModel, "");
		}
		
		AccessorModel outputNormalsAccessorModel;
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			outputNormalsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			outputNormalsAccessorModel = AccessorModelCreation.instantiate(normalsAccessorModel, "");
		}
		
		AccessorModel outputTangentsAccessorModel;
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "TANGENT")) {
			outputTangentsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			outputTangentsAccessorModel = AccessorModelCreation.instantiate(tangentsAccessorModel, "");
		}
		
		int pointCount = positionsAccessorModel.getCount();
		List<Runnable> skinningCommands = createSoftwareSkinningCommands(pointCount, jointMatrices, attributes,
				AccessorDatas.createFloat(positionsAccessorModel),
				AccessorDatas.createFloat(normalsAccessorModel),
				AccessorDatas.createFloat(tangentsAccessorModel),
				AccessorDatas.createFloat(outputPositionsAccessorModel),
				AccessorDatas.createFloat(outputNormalsAccessorModel),
				AccessorDatas.createFloat(outputTangentsAccessorModel));
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			GL11.glVertexPointer(
					outputPositionsAccessorModel.getElementType().getNumComponents(),
					outputPositionsAccessorModel.getComponentType(),
					outputPositionsAccessorModel.getByteStride(),
					outputPositionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			GL11.glNormalPointer(
					outputNormalsAccessorModel.getComponentType(),
					outputNormalsAccessorModel.getByteStride(),
					outputNormalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			GL20.glVertexAttribPointer(
					at_tangent,
					outputTangentsAccessorModel.getElementType().getNumComponents(),
					outputTangentsAccessorModel.getComponentType(),
					false,
					outputTangentsAccessorModel.getByteStride(),
					outputTangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> GL11.glDrawArrays(mode, 0, pointCount));
		}
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, float[][] jointMatrices, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel) {
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		AccessorModel outputPositionsAccessorModel;
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			outputPositionsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			outputPositionsAccessorModel = AccessorModelCreation.instantiate(positionsAccessorModel, "");
		}
		
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		AccessorModel outputNormalsAccessorModel;
		AccessorModel outputTangentsAccessorModel;
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createNormalTangentMorphTarget(morphTargets, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, tangentTargetAccessorDatas)) {
			outputNormalsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
			outputTangentsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			outputNormalsAccessorModel = AccessorModelCreation.instantiate(normalsAccessorModel, "");
			outputTangentsAccessorModel = AccessorModelCreation.instantiate(tangentsAccessorModel, "");
		}
		
		int pointCount = positionsAccessorModel.getCount();
		List<Runnable> skinningCommands = createSoftwareSkinningCommands(pointCount, jointMatrices, attributes,
				AccessorDatas.createFloat(positionsAccessorModel),
				AccessorDatas.createFloat(normalsAccessorModel),
				AccessorDatas.createFloat(tangentsAccessorModel),
				AccessorDatas.createFloat(outputPositionsAccessorModel),
				AccessorDatas.createFloat(outputNormalsAccessorModel),
				AccessorDatas.createFloat(outputTangentsAccessorModel));
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			GL11.glVertexPointer(
					outputPositionsAccessorModel.getElementType().getNumComponents(),
					outputPositionsAccessorModel.getComponentType(),
					outputPositionsAccessorModel.getByteStride(),
					outputPositionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			GL11.glNormalPointer(
					outputNormalsAccessorModel.getComponentType(),
					outputNormalsAccessorModel.getByteStride(),
					outputNormalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			GL20.glVertexAttribPointer(
					at_tangent,
					outputTangentsAccessorModel.getElementType().getNumComponents(),
					outputTangentsAccessorModel.getComponentType(),
					false,
					outputTangentsAccessorModel.getByteStride(),
					outputTangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> GL11.glDrawArrays(mode, 0, pointCount));
		}
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, float[][] jointMatrices) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel outputPositionsAccessorModel;
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			outputPositionsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			outputPositionsAccessorModel = AccessorModelCreation.instantiate(positionsAccessorModel, "");
		}
		
		AccessorModel normalsAccessorModel = attributes.get("NORMAL");
		AccessorModel outputNormalsAccessorModel;
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			outputNormalsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			outputNormalsAccessorModel = AccessorModelCreation.instantiate(normalsAccessorModel, "");
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel outputTangentsAccessorModel;
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(meshPrimitiveModel, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel);
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel)) {
			outputTangentsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			outputTangentsAccessorModel = AccessorModelCreation.instantiate(tangentsAccessorModel, "");
		}
		
		int pointCount = positionsAccessorModel.getCount();
		List<Runnable> skinningCommands = createSoftwareSkinningCommands(pointCount, jointMatrices, attributes,
				AccessorDatas.createFloat(positionsAccessorModel),
				AccessorDatas.createFloat(normalsAccessorModel),
				AccessorDatas.createFloat(tangentsAccessorModel),
				AccessorDatas.createFloat(outputPositionsAccessorModel),
				AccessorDatas.createFloat(outputNormalsAccessorModel),
				AccessorDatas.createFloat(outputTangentsAccessorModel));
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			GL11.glVertexPointer(
					outputPositionsAccessorModel.getElementType().getNumComponents(),
					outputPositionsAccessorModel.getComponentType(),
					outputPositionsAccessorModel.getByteStride(),
					outputPositionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			GL11.glNormalPointer(
					outputNormalsAccessorModel.getComponentType(),
					outputNormalsAccessorModel.getByteStride(),
					outputNormalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			GL20.glVertexAttribPointer(
					at_tangent,
					outputTangentsAccessorModel.getElementType().getNumComponents(),
					outputTangentsAccessorModel.getComponentType(),
					false,
					outputTangentsAccessorModel.getByteStride(),
					outputTangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
		}
		AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
		renderCommand.add(() -> {
			GL11.glTexCoordPointer(
					texcoordsAccessorModelFinal.getElementType().getNumComponents(),
					texcoordsAccessorModelFinal.getComponentType(),
					texcoordsAccessorModelFinal.getByteStride(),
					texcoordsAccessorModelFinal.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		});
		
		AccessorModel texcoords1AccessorModelFinal;
		texcoordsAccessorModel = attributes.get("TEXCOORD_1");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			texcoords1AccessorModelFinal = texcoordsAccessorModel;
		}
		else {
			texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoords1AccessorModelFinal.getElementType().getNumComponents(),
					texcoords1AccessorModelFinal.getComponentType(),
					false,
					texcoords1AccessorModelFinal.getByteStride(),
					texcoords1AccessorModelFinal.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		});
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, pointCount));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, float[][] jointMatrices) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		AccessorModel outputPositionsAccessorModel;
		AccessorModel outputNormalsAccessorModel;
		AccessorModel outputTangentsAccessorModel;
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalTangentMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas, tangentTargetAccessorDatas)) {
			outputPositionsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
			outputNormalsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
			outputTangentsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			outputPositionsAccessorModel = AccessorModelCreation.instantiate(positionsAccessorModel, "");
			outputNormalsAccessorModel = AccessorModelCreation.instantiate(normalsAccessorModel, "");
			outputTangentsAccessorModel = AccessorModelCreation.instantiate(tangentsAccessorModel, "");
		}
		
		int pointCount = positionsAccessorModel.getCount();
		List<Runnable> skinningCommands = createSoftwareSkinningCommands(pointCount, jointMatrices, attributes,
				AccessorDatas.createFloat(positionsAccessorModel),
				AccessorDatas.createFloat(normalsAccessorModel),
				AccessorDatas.createFloat(tangentsAccessorModel),
				AccessorDatas.createFloat(outputPositionsAccessorModel),
				AccessorDatas.createFloat(outputNormalsAccessorModel),
				AccessorDatas.createFloat(outputTangentsAccessorModel));
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			GL11.glVertexPointer(
					outputPositionsAccessorModel.getElementType().getNumComponents(),
					outputPositionsAccessorModel.getComponentType(),
					outputPositionsAccessorModel.getByteStride(),
					outputPositionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			GL11.glNormalPointer(
					outputNormalsAccessorModel.getComponentType(),
					outputNormalsAccessorModel.getByteStride(),
					outputNormalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			GL20.glVertexAttribPointer(
					at_tangent,
					outputTangentsAccessorModel.getElementType().getNumComponents(),
					outputTangentsAccessorModel.getComponentType(),
					false,
					outputTangentsAccessorModel.getByteStride(),
					outputTangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
			renderCommand.add(() -> {
				GL11.glTexCoordPointer(
						texcoordsAccessorModelFinal.getElementType().getNumComponents(),
						texcoordsAccessorModelFinal.getComponentType(),
						texcoordsAccessorModelFinal.getByteStride(),
						texcoordsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			});
			
			AccessorModel texcoords1AccessorModelFinal;
			texcoordsAccessorModel = attributes.get("TEXCOORD_1");
			if(texcoordsAccessorModel != null) {
				targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
				if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
					texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
				}
				else {
					bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
				}
				texcoords1AccessorModelFinal = texcoordsAccessorModel;
			}
			else {
				texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
			}
			renderCommand.add(() -> {
				GL20.glVertexAttribPointer(
						mc_midTexCoord,
						texcoords1AccessorModelFinal.getElementType().getNumComponents(),
						texcoords1AccessorModelFinal.getComponentType(),
						false,
						texcoords1AccessorModelFinal.getByteStride(),
						texcoords1AccessorModelFinal.getByteOffset());
				GL20.glEnableVertexAttribArray(mc_midTexCoord);
			});
		}
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, pointCount));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		if(attributes.containsKey("TEXCOORD_0")) renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	@Override
	protected void processMeshPrimitiveModelFlatNormalMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, float[][] jointMatrices) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		AccessorModel outputPositionsAccessorModel;
		AccessorModel outputNormalsAccessorModel;
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas)) {
			outputPositionsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
			outputNormalsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			outputPositionsAccessorModel = AccessorModelCreation.instantiate(positionsAccessorModel, "");
			outputNormalsAccessorModel = AccessorModelCreation.instantiate(normalsAccessorModel, "");
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		AccessorModel outputTangentsAccessorModel;
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel, normalTargetAccessorDatas)) {
			outputTangentsAccessorModel = obtainVec3FloatMorphedModel(nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			outputTangentsAccessorModel = AccessorModelCreation.instantiate(tangentsAccessorModel, "");
		}
		
		int pointCount = positionsAccessorModel.getCount();
		List<Runnable> skinningCommands = createSoftwareSkinningCommands(pointCount, jointMatrices, attributes,
				AccessorDatas.createFloat(positionsAccessorModel),
				AccessorDatas.createFloat(normalsAccessorModel),
				AccessorDatas.createFloat(tangentsAccessorModel),
				AccessorDatas.createFloat(outputPositionsAccessorModel),
				AccessorDatas.createFloat(outputNormalsAccessorModel),
				AccessorDatas.createFloat(outputTangentsAccessorModel));
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			GL11.glVertexPointer(
					outputPositionsAccessorModel.getElementType().getNumComponents(),
					outputPositionsAccessorModel.getComponentType(),
					outputPositionsAccessorModel.getByteStride(),
					outputPositionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			GL11.glNormalPointer(
					outputNormalsAccessorModel.getComponentType(),
					outputNormalsAccessorModel.getByteStride(),
					outputNormalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			GL20.glVertexAttribPointer(
					at_tangent,
					outputTangentsAccessorModel.getElementType().getNumComponents(),
					outputTangentsAccessorModel.getComponentType(),
					false,
					outputTangentsAccessorModel.getByteStride(),
					outputTangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		});
		
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if(colorsAccessorModel != null) {
			colorsAccessorModel = obtainVec4ColorsAccessorModel(colorsAccessorModel);
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createColorMorphTarget(morphTargets, targetAccessorDatas, "COLOR_0")) {
				colorsAccessorModel = bindColorMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, colorsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, colorsAccessorModel.getBufferViewModel(), renderCommand);
			}
			AccessorModel colorsAccessorModelFinal = colorsAccessorModel;
			renderCommand.add(() -> {
				GL11.glColorPointer(
						colorsAccessorModelFinal.getElementType().getNumComponents(),
						colorsAccessorModelFinal.getComponentType(),
						colorsAccessorModelFinal.getByteStride(),
						colorsAccessorModelFinal.getByteOffset());
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			});
		}
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_0")) {
			texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
		}
		AccessorModel texcoordsAccessorModelFinal = texcoordsAccessorModel;
		renderCommand.add(() -> {
			GL11.glTexCoordPointer(
					texcoordsAccessorModelFinal.getElementType().getNumComponents(),
					texcoordsAccessorModelFinal.getComponentType(),
					texcoordsAccessorModelFinal.getByteStride(),
					texcoordsAccessorModelFinal.getByteOffset());
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		});
		
		AccessorModel texcoords1AccessorModelFinal;
		texcoordsAccessorModel = attributes.get("TEXCOORD_1");
		if(texcoordsAccessorModel != null) {
			targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
			if(createTexcoordMorphTarget(morphTargets, targetAccessorDatas, "TEXCOORD_1")) {
				texcoordsAccessorModel = bindTexcoordMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, texcoordsAccessorModel, targetAccessorDatas);
			}
			else {
				bindArrayBufferViewModel(gltfRenderData, texcoordsAccessorModel.getBufferViewModel(), renderCommand);
			}
			texcoords1AccessorModelFinal = texcoordsAccessorModel;
		}
		else {
			texcoords1AccessorModelFinal = texcoordsAccessorModelFinal;
		}
		renderCommand.add(() -> {
			GL20.glVertexAttribPointer(
					mc_midTexCoord,
					texcoords1AccessorModelFinal.getElementType().getNumComponents(),
					texcoords1AccessorModelFinal.getComponentType(),
					false,
					texcoords1AccessorModelFinal.getByteStride(),
					texcoords1AccessorModelFinal.getByteOffset());
			GL20.glEnableVertexAttribArray(mc_midTexCoord);
		});
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> GL11.glDrawArrays(mode, 0, pointCount));
		if(attributes.containsKey("COLOR_0")) renderCommand.add(() -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY));
		renderCommand.add(() -> {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			GL20.glDisableVertexAttribArray(RenderedGltfModel.mc_midTexCoord);
		});
	}
	
	public void bindArrayBufferViewModel(List<Runnable> gltfRenderData, BufferViewModel bufferViewModel, List<Runnable> renderCommand) {
		Integer glBufferView = bufferViewModelToGlBufferView.get(bufferViewModel);
		if(glBufferView == null) {
			Integer glBufferViewNew = GL15.glGenBuffers();
			gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferViewNew));
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferViewNew);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			bufferViewModelToGlBufferView.put(bufferViewModel, glBufferViewNew);
			renderCommand.add(() -> GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferViewNew));
		}
		else {
			renderCommand.add(() -> GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView));
		}
	}

}
