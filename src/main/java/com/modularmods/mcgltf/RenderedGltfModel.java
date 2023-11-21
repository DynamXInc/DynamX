package com.modularmods.mcgltf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import com.google.gson.Gson;
import com.jme3.util.mikktspace.MikkTSpaceContext;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorShortData;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MathUtils;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.Optionals;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.image.PixelDatas;
import de.javagl.jgltf.model.impl.DefaultNodeModel;

public class RenderedGltfModel {

	/**
	 * ShaderMod attribute location for middle UV coordinates, used for parallax occlusion mapping.</br>
	 * This may change in different Minecraft version.</br>
	 * <a href="https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt">optifine/shaders.txt</a>
	 */
	public static final int mc_midTexCoord = 11;
	
	/**
	 * ShaderMod attribute location for Tangent.</br>
	 * This may change in different Minecraft version.</br>
	 * <a href="https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt">optifine/shaders.txt</a>
	 */
	public static final int at_tangent = 12;
	
	/**
	 * ShaderMod Texture index, this may change in different Minecraft version.</br>
	 * <a href="https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt">optifine/shaders.txt</a>
	 */
	public static final int COLOR_MAP_INDEX = GL13.GL_TEXTURE0;
	public static final int NORMAL_MAP_INDEX = GL13.GL_TEXTURE2;
	public static final int SPECULAR_MAP_INDEX = GL13.GL_TEXTURE3;
	
	protected static final Runnable vanillaDefaultMaterialCommand = () -> {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, MCglTF.getInstance().getDefaultColorMap());
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_CULL_FACE);
	};
	
	protected static final Runnable shaderModDefaultMaterialCommand = () -> {
		GL13.glActiveTexture(COLOR_MAP_INDEX);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, MCglTF.getInstance().getDefaultColorMap());
		GL13.glActiveTexture(NORMAL_MAP_INDEX);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, MCglTF.getInstance().getDefaultNormalMap());
		GL13.glActiveTexture(SPECULAR_MAP_INDEX);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, MCglTF.getInstance().getDefaultSpecularMap());
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_CULL_FACE);
	};
	
	protected static final int skinning_joint = 0;
	protected static final int skinning_weight = 1;
	protected static final int skinning_position = 2;
	protected static final int skinning_normal = 3;
	protected static final int skinning_tangent = 4;
	
	protected static final int skinning_out_position = 0;
	protected static final int skinning_out_normal = 1;
	protected static final int skinning_out_tangent = 2;
	
	protected static FloatBuffer uniformFloatBuffer = null;
	
	protected static final FloatBuffer BUF_FLOAT_16 = BufferUtils.createFloatBuffer(16);
	
	public static final Map<NodeModel, float[]> NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE = new IdentityHashMap<NodeModel, float[]>();
	
	protected final Map<NodeModel, Triple<List<Runnable>, List<Runnable>, List<Runnable>>> rootNodeModelToCommands = new IdentityHashMap<NodeModel, Triple<List<Runnable>, List<Runnable>, List<Runnable>>>();
	protected final Map<AccessorModel, AccessorModel> positionsAccessorModelToNormalsAccessorModel = new IdentityHashMap<AccessorModel, AccessorModel>();
	protected final Map<AccessorModel, AccessorModel> normalsAccessorModelToTangentsAccessorModel = new IdentityHashMap<AccessorModel, AccessorModel>();
	protected final Map<AccessorModel, AccessorModel> colorsAccessorModelVec3ToVec4 = new IdentityHashMap<AccessorModel, AccessorModel>();
	protected final Map<AccessorModel, AccessorModel> jointsAccessorModelUnsignedLookup = new IdentityHashMap<AccessorModel, AccessorModel>();
	protected final Map<AccessorModel, AccessorModel> weightsAccessorModelDequantizedLookup = new IdentityHashMap<AccessorModel, AccessorModel>();
	protected final Map<AccessorModel, AccessorFloatData> colorsMorphTargetAccessorModelToAccessorData = new IdentityHashMap<AccessorModel, AccessorFloatData>();
	protected final Map<AccessorModel, AccessorFloatData> texcoordsMorphTargetAccessorModelToAccessorData = new IdentityHashMap<AccessorModel, AccessorFloatData>();
	protected final Map<MeshPrimitiveModel, AccessorModel> meshPrimitiveModelToTangentsAccessorModel = new IdentityHashMap<MeshPrimitiveModel, AccessorModel>();
	protected final Map<MeshPrimitiveModel, Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>>> meshPrimitiveModelToUnindexed = new IdentityHashMap<MeshPrimitiveModel, Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>>>();
	protected final Map<BufferViewModel, Integer> bufferViewModelToGlBufferView = new IdentityHashMap<BufferViewModel, Integer>();
	protected final Map<TextureModel, Integer> textureModelToGlTexture = new IdentityHashMap<TextureModel, Integer>();
	protected final Map<String, Material> extrasToMaterial = new IdentityHashMap<>();
	
	public final GltfModel gltfModel;
	
	public final List<RenderedGltfScene> renderedGltfScenes;
	
	protected RenderedGltfModel(GltfModel gltfModel, List<RenderedGltfScene> renderedGltfScenes) {
		this.gltfModel = gltfModel;
		this.renderedGltfScenes = renderedGltfScenes;
	}
	
	public RenderedGltfModel(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		this.gltfModel = gltfModel;
		List<SceneModel> sceneModels = gltfModel.getSceneModels();
		renderedGltfScenes = new ArrayList<>(sceneModels.size());
		processSceneModels(gltfRenderData, sceneModels);
	}
	
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfScene renderedGltfScene = new RenderedGltfScene();
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
				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatrixBuffer);
				GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, jointMatrixSize * Float.BYTES, GL15.GL_STATIC_DRAW);
				
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
					
					GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatrixBuffer);
					GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, putFloatBuffer(jointMatrices));
					
					GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, jointMatrixBuffer);
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
	
	protected void processMeshPrimitiveModel(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> vanillaRenderCommands, List<Runnable> shaderModRenderCommands) {
		Map<String, AccessorModel> attributes = meshPrimitiveModel.getAttributes();
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		if(positionsAccessorModel != null) {
			List<Runnable> renderCommand = new ArrayList<Runnable>();
			AccessorModel normalsAccessorModel = attributes.get("NORMAL");
			if(normalsAccessorModel != null) {
				AccessorModel tangentsAccessorModel = attributes.get("TANGENT");
				if(tangentsAccessorModel != null) {
					processMeshPrimitiveModelIncludedTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, attributes, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel);
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					}
				}
				else {
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
						if(renderedMaterial.normalTexture != null) {
							processMeshPrimitiveModelMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand);
						}
						else {
							processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, attributes, positionsAccessorModel, normalsAccessorModel);
						}
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
						processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, attributes, positionsAccessorModel, normalsAccessorModel);
					}
				}
			}
			else {
				MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
				if(materialModel != null) {
					//DynamX
					Object extras = materialModel.getExtras();
					Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
					vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
					shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					if(renderedMaterial.normalTexture != null) {
						processMeshPrimitiveModelFlatNormalMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand);
					}
					else {
						processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand);
					}
				}
				else {
					vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
					shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand);
				}
			}
			vanillaRenderCommands.addAll(renderCommand);
			shaderModRenderCommands.addAll(renderCommand);
		}
	}
	
	protected void processMeshPrimitiveModelIncludedTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel) {
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL11.glVertexPointer(
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
		}
		GL11.glNormalPointer(
				normalsAccessorModel.getComponentType(),
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "TANGENT")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
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
			int count = positionsAccessorModel.getCount();
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, count);
			});
		}
	}
	
	protected void processMeshPrimitiveModelSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel) {
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		List<Map<String, AccessorModel>> morphTargets = meshPrimitiveModel.getTargets();
		
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL11.glVertexPointer(
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> tangentTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createNormalTangentMorphTarget(morphTargets, normalsAccessorModel, tangentsAccessorModel, targetAccessorDatas, tangentTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		}
		
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
			int count = positionsAccessorModel.getCount();
			renderCommand.add(() -> {
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, count);
			});
		}
	}
	
	protected void processMeshPrimitiveModelMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "POSITION")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
		}
		GL11.glVertexPointer(
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		AccessorModel normalsAccessorModel = attributes.get("NORMAL");
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createMorphTarget(morphTargets, targetAccessorDatas, "NORMAL")) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
		}
		GL11.glNormalPointer(
				normalsAccessorModel.getComponentType(),
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(meshPrimitiveModel, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
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
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, count);
		});
	}
	
	protected void processMeshPrimitiveModelFlatNormalSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
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
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, normalTargetAccessorDatas);
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, tangentTargetAccessorDatas);
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					at_tangent,
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(at_tangent);
		}

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
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, count);
		});
	}
	
	protected void processMeshPrimitiveModelFlatNormalMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand) {
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed = obtainUnindexed(meshPrimitiveModel);
		Map<String, AccessorModel> attributes = unindexed.getLeft();
		List<Map<String, AccessorModel>> morphTargets = unindexed.getRight();
		
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		AccessorModel normalsAccessorModel = obtainNormalsAccessorModel(positionsAccessorModel);
		List<AccessorFloatData> targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		List<AccessorFloatData> normalTargetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createPositionNormalMorphTarget(morphTargets, positionsAccessorModel, normalsAccessorModel, targetAccessorDatas, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, positionsAccessorModel, targetAccessorDatas);
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, normalsAccessorModel, normalTargetAccessorDatas);
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, positionsAccessorModel.getBufferViewModel());
			GL11.glVertexPointer(
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			
			bindArrayBufferViewModel(gltfRenderData, normalsAccessorModel.getBufferViewModel());
			GL11.glNormalPointer(
					normalsAccessorModel.getComponentType(),
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		}
		
		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		AccessorModel tangentsAccessorModel = obtainTangentsAccessorModel(normalsAccessorModel);
		targetAccessorDatas = new ArrayList<AccessorFloatData>(morphTargets.size());
		if(createTangentMorphTarget(morphTargets, targetAccessorDatas, positionsAccessorModel, normalsAccessorModel, texcoordsAccessorModel, "TEXCOORD_0", tangentsAccessorModel, normalTargetAccessorDatas)) {
			bindVec3FloatMorphed(gltfRenderData, nodeModel, meshModel, renderCommand, tangentsAccessorModel, targetAccessorDatas);
		}
		else {
			bindArrayBufferViewModel(gltfRenderData, tangentsAccessorModel.getBufferViewModel());
		}
		GL20.glVertexAttribPointer(
				at_tangent,
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
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
		int count = positionsAccessorModel.getCount();
		renderCommand.add(() -> {
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, count);
		});
	}
	
	protected void processMeshPrimitiveModel(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, float[][] jointMatrices, List<Runnable> skinningCommand, List<Runnable> vanillaRenderCommands, List<Runnable> shaderModRenderCommands) {
		Map<String, AccessorModel> attributes = meshPrimitiveModel.getAttributes();
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		if(positionsAccessorModel != null) {
			List<Runnable> renderCommand = new ArrayList<Runnable>();
			AccessorModel normalsAccessorModel = attributes.get("NORMAL");
			if(normalsAccessorModel != null) {
				AccessorModel tangentsAccessorModel = attributes.get("TANGENT");
				if(tangentsAccessorModel != null) {
					if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelIncludedTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel);
					else processMeshPrimitiveModelIncludedTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand, attributes, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel);
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					}
				}
				else {
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
						if(renderedMaterial.normalTexture != null) {
							if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
							else processMeshPrimitiveModelMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand);
						}
						else {
							if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel);
							else processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand, attributes, positionsAccessorModel, normalsAccessorModel);
						}
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
						if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel);
						else processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand, attributes, positionsAccessorModel, normalsAccessorModel);
					}
				}
			}
			else {
				MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
				if(materialModel != null) {
					//DynamX
					Object extras = materialModel.getExtras();
					Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
					vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
					shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					if(renderedMaterial.normalTexture != null) {
						if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelFlatNormalMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
						else processMeshPrimitiveModelFlatNormalMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand);
					}
					else {
						if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
						else processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand);
					}
				}
				else {
					vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
					shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					if(attributes.containsKey("JOINTS_1")) processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
					else processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, skinningCommand);
				}
			}
			vanillaRenderCommands.addAll(renderCommand);
			shaderModRenderCommands.addAll(renderCommand);
		}
	}
	
	protected void processMeshPrimitiveModelIncludedTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel) {
		int glTransformFeedback = GL40.glGenTransformFeedbacks();
		gltfRenderData.add(() -> GL40.glDeleteTransformFeedbacks(glTransformFeedback));
		GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
		
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
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
			
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
				GL40.glDrawTransformFeedback(mode, glTransformFeedback);
			});
		}
	}
	
	protected void processMeshPrimitiveModelSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand, Map<String, AccessorModel> attributes, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel) {
		int glTransformFeedback = GL40.glGenTransformFeedbacks();
		gltfRenderData.add(() -> GL40.glDeleteTransformFeedbacks(glTransformFeedback));
		GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
		
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
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
			
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
				GL40.glDrawTransformFeedback(mode, glTransformFeedback);
			});
		}
	}
	
	protected void processMeshPrimitiveModelMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glTransformFeedback = GL40.glGenTransformFeedbacks();
		gltfRenderData.add(() -> GL40.glDeleteTransformFeedbacks(glTransformFeedback));
		GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
		
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
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
			
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
			GL40.glDrawTransformFeedback(mode, glTransformFeedback);
		});
	}
	
	protected void processMeshPrimitiveModelFlatNormalSimpleTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glTransformFeedback = GL40.glGenTransformFeedbacks();
		gltfRenderData.add(() -> GL40.glDeleteTransformFeedbacks(glTransformFeedback));
		GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
		
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
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
			
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
			GL40.glDrawTransformFeedback(mode, glTransformFeedback);
		});
	}
	
	protected void processMeshPrimitiveModelFlatNormalMikkTangent(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, List<Runnable> renderCommand, List<Runnable> skinningCommand) {
		int glTransformFeedback = GL40.glGenTransformFeedbacks();
		gltfRenderData.add(() -> GL40.glDeleteTransformFeedbacks(glTransformFeedback));
		GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
		
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
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_position, positionBuffer);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, normalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_normal, normalBuffer);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentBuffer);
		GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, tangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, skinning_out_tangent, tangentBuffer);
		
		int pointCount = positionsAccessorModel.getCount();
		skinningCommand.add(() -> {
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, glTransformFeedback);
			
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
			GL40.glDrawTransformFeedback(mode, glTransformFeedback);
		});
	}
	
	protected void processMeshPrimitiveModel(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel, float[][] jointMatrices, List<Runnable> vanillaRenderCommands, List<Runnable> shaderModRenderCommands) {
		Map<String, AccessorModel> attributes = meshPrimitiveModel.getAttributes();
		AccessorModel positionsAccessorModel = attributes.get("POSITION");
		if(positionsAccessorModel != null) {
			List<Runnable> renderCommand = new ArrayList<Runnable>();
			AccessorModel normalsAccessorModel = attributes.get("NORMAL");
			if(normalsAccessorModel != null) {
				AccessorModel tangentsAccessorModel = attributes.get("TANGENT");
				if(tangentsAccessorModel != null) {
					processMeshPrimitiveModelIncludedTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel, tangentsAccessorModel);
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					}
				}
				else {
					MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
					if(materialModel != null) {
						//DynamX
						Object extras = materialModel.getExtras();
						Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
						vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
						shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
						if(renderedMaterial.normalTexture != null) {
							processMeshPrimitiveModelMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
						}
						else {
							processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel);
						}
					}
					else {
						vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
						shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
						processMeshPrimitiveModelSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices, attributes, positionsAccessorModel, normalsAccessorModel);
					}
				}
			}
			else {
				MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
				if(materialModel != null) {
					//DynamX
					Object extras = materialModel.getExtras();
					Material renderedMaterial = obtainMaterial(gltfRenderData, extras, materialModel);
					vanillaRenderCommands.add(renderedMaterial.vanillaMaterialCommand);
					shaderModRenderCommands.add(renderedMaterial.shaderModMaterialCommand);
					if(renderedMaterial.normalTexture != null) {
						processMeshPrimitiveModelFlatNormalMikkTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
					}
					else {
						processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
					}
				}
				else {
					vanillaRenderCommands.add(vanillaDefaultMaterialCommand);
					shaderModRenderCommands.add(shaderModDefaultMaterialCommand);
					processMeshPrimitiveModelFlatNormalSimpleTangent(gltfRenderData, nodeModel, meshModel, meshPrimitiveModel, renderCommand, jointMatrices);
				}
			}
			vanillaRenderCommands.addAll(renderCommand);
			shaderModRenderCommands.addAll(renderCommand);
		}
	}
	
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
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				outputPositionsAccessorModel.getElementType().getNumComponents(),
				outputPositionsAccessorModel.getComponentType(),
				outputPositionsAccessorModel.getByteStride(),
				outputPositionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				outputNormalsAccessorModel.getComponentType(),
				outputNormalsAccessorModel.getByteStride(),
				outputNormalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				at_tangent,
				outputTangentsAccessorModel.getElementType().getNumComponents(),
				outputTangentsAccessorModel.getComponentType(),
				false,
				outputTangentsAccessorModel.getByteStride(),
				outputTangentsAccessorModel.getByteOffset());
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
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				skinningCommands.parallelStream().forEach(Runnable::run);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
				
				GL30.glBindVertexArray(glVertexArray);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> {
				skinningCommands.parallelStream().forEach(Runnable::run);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
				
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, pointCount);
			});
		}
	}
	
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
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				outputPositionsAccessorModel.getElementType().getNumComponents(),
				outputPositionsAccessorModel.getComponentType(),
				outputPositionsAccessorModel.getByteStride(),
				outputPositionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				outputNormalsAccessorModel.getComponentType(),
				outputNormalsAccessorModel.getByteStride(),
				outputNormalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				at_tangent,
				outputTangentsAccessorModel.getElementType().getNumComponents(),
				outputTangentsAccessorModel.getComponentType(),
				false,
				outputTangentsAccessorModel.getByteStride(),
				outputTangentsAccessorModel.getByteOffset());
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
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		int mode = meshPrimitiveModel.getMode();
		AccessorModel indices = meshPrimitiveModel.getIndices();
		if(indices != null) {
			int glIndicesBufferView = obtainElementArrayBuffer(gltfRenderData, indices.getBufferViewModel());
			int count = indices.getCount();
			int type = indices.getComponentType();
			int offset = indices.getByteOffset();
			renderCommand.add(() -> {
				skinningCommands.parallelStream().forEach(Runnable::run);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
				
				GL30.glBindVertexArray(glVertexArray);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBufferView);
				GL11.glDrawElements(mode, count, type, offset);
			});
		}
		else {
			renderCommand.add(() -> {
				skinningCommands.parallelStream().forEach(Runnable::run);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
				
				GL30.glBindVertexArray(glVertexArray);
				GL11.glDrawArrays(mode, 0, pointCount);
			});
		}
	}
	
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
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				outputPositionsAccessorModel.getElementType().getNumComponents(),
				outputPositionsAccessorModel.getComponentType(),
				outputPositionsAccessorModel.getByteStride(),
				outputPositionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				outputNormalsAccessorModel.getComponentType(),
				outputNormalsAccessorModel.getByteStride(),
				outputNormalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				at_tangent,
				outputTangentsAccessorModel.getElementType().getNumComponents(),
				outputTangentsAccessorModel.getComponentType(),
				false,
				outputTangentsAccessorModel.getByteStride(),
				outputTangentsAccessorModel.getByteOffset());
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
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}
	
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
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				outputPositionsAccessorModel.getElementType().getNumComponents(),
				outputPositionsAccessorModel.getComponentType(),
				outputPositionsAccessorModel.getByteStride(),
				outputPositionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				outputNormalsAccessorModel.getComponentType(),
				outputNormalsAccessorModel.getByteStride(),
				outputNormalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				at_tangent,
				outputTangentsAccessorModel.getElementType().getNumComponents(),
				outputTangentsAccessorModel.getComponentType(),
				false,
				outputTangentsAccessorModel.getByteStride(),
				outputTangentsAccessorModel.getByteOffset());
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
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}
	
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
		
		int glVertexArray = GL30.glGenVertexArrays();
		gltfRenderData.add(() -> GL30.glDeleteVertexArrays(glVertexArray));
		GL30.glBindVertexArray(glVertexArray);
		
		int positionBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(positionBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputPositionsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				outputPositionsAccessorModel.getElementType().getNumComponents(),
				outputPositionsAccessorModel.getComponentType(),
				outputPositionsAccessorModel.getByteStride(),
				outputPositionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		int normalBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(normalBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputNormalsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				outputNormalsAccessorModel.getComponentType(),
				outputNormalsAccessorModel.getByteStride(),
				outputNormalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		
		int tangentBuffer = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(tangentBuffer));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outputTangentsAccessorModel.getBufferViewModel().getByteLength(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				at_tangent,
				outputTangentsAccessorModel.getElementType().getNumComponents(),
				outputTangentsAccessorModel.getComponentType(),
				false,
				outputTangentsAccessorModel.getByteStride(),
				outputTangentsAccessorModel.getByteOffset());
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
		
		ByteBuffer positionsBufferViewData = outputPositionsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer normalsBufferViewData = outputNormalsAccessorModel.getBufferViewModel().getBufferViewData();
		ByteBuffer tangentsBufferViewData = outputTangentsAccessorModel.getBufferViewModel().getBufferViewData();
		
		int mode = meshPrimitiveModel.getMode();
		renderCommand.add(() -> {
			skinningCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, normalsBufferViewData);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentBuffer);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, tangentsBufferViewData);
			
			GL30.glBindVertexArray(glVertexArray);
			GL11.glDrawArrays(mode, 0, pointCount);
		});
	}
	
	public static class Material {
		
		public static class TextureInfo {
			public int index;
		}
		
		public TextureInfo baseColorTexture;
		public TextureInfo normalTexture;
		public TextureInfo specularTexture;
		public float[] baseColorFactor = {1.0F, 1.0F, 1.0F, 1.0F};
		public boolean doubleSided;
		
		public Runnable vanillaMaterialCommand;
		public Runnable shaderModMaterialCommand;
		
		public void initMaterialCommand(List<Runnable> gltfRenderData, RenderedGltfModel renderedModel) {
			List<TextureModel> textureModels = renderedModel.gltfModel.getTextureModels();
			int colorMap = baseColorTexture == null ? MCglTF.getInstance().getDefaultColorMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(baseColorTexture.index));
			int normalMap = normalTexture == null ? MCglTF.getInstance().getDefaultNormalMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(normalTexture.index));
			int specularMap = specularTexture == null ? MCglTF.getInstance().getDefaultSpecularMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(specularTexture.index));
			if(doubleSided) {
				vanillaMaterialCommand = () -> {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL11.glColor4f(baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);
					GL11.glDisable(GL11.GL_CULL_FACE);
				};
				shaderModMaterialCommand = () -> {
					GL13.glActiveTexture(COLOR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL13.glActiveTexture(NORMAL_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap);
					GL13.glActiveTexture(SPECULAR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularMap);
					GL11.glColor4f(baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);
					GL11.glDisable(GL11.GL_CULL_FACE);
				};
			}
			else {
				vanillaMaterialCommand = () -> {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL11.glColor4f(baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);
					GL11.glEnable(GL11.GL_CULL_FACE);
				};
				shaderModMaterialCommand = () -> {
					GL13.glActiveTexture(COLOR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL13.glActiveTexture(NORMAL_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap);
					GL13.glActiveTexture(SPECULAR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularMap);
					GL11.glColor4f(baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);
					GL11.glEnable(GL11.GL_CULL_FACE);
				};
			}
		}
	}
	
	public Runnable createTransformCommand(NodeModel nodeModel) {
		return () -> {
			GL11.glPushMatrix();
			BUF_FLOAT_16.clear();
			BUF_FLOAT_16.put(findGlobalTransform(nodeModel));
			BUF_FLOAT_16.rewind();
			GL11.glMultMatrix(BUF_FLOAT_16);
		};
	}
	
	public void bindArrayBufferViewModel(List<Runnable> gltfRenderData, BufferViewModel bufferViewModel) {
		Integer glBufferView = bufferViewModelToGlBufferView.get(bufferViewModel);
		if(glBufferView == null) {
			Integer glBufferViewNew = GL15.glGenBuffers();
			gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferViewNew));
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferViewNew);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			bufferViewModelToGlBufferView.put(bufferViewModel, glBufferViewNew);
		}
		else GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
	}
	
	public int obtainElementArrayBuffer(List<Runnable> gltfRenderData, BufferViewModel bufferViewModel) {
		Integer glBufferView = bufferViewModelToGlBufferView.get(bufferViewModel);
		if(glBufferView == null) {
			Integer glBufferViewNew = GL15.glGenBuffers();
			gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferViewNew));
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBufferViewNew);
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			bufferViewModelToGlBufferView.put(bufferViewModel, glBufferViewNew);
			return glBufferViewNew;
		}
		else {
			return glBufferView;
		}
	}
	
	public Material obtainMaterial(List<Runnable> gltfRenderData, Object extras, MaterialModel materialModel) {
		Material material = extrasToMaterial.get(materialModel.getName());
		if(material == null) {
			if(extras != null) {
				Gson gson = new Gson();
				material = gson.fromJson(gson.toJsonTree(extras), Material.class);
			}else{
				material = new Material();
				Material.TextureInfo baseTexture = new Material.TextureInfo();
				Material.TextureInfo normalTexture = new Material.TextureInfo();
				Material.TextureInfo specularTexture = new Material.TextureInfo();
				material.baseColorTexture = baseTexture;
				material.normalTexture = normalTexture;
				material.specularTexture = specularTexture;
				baseTexture.index = normalTexture.index = specularTexture.index = 0;
				material.doubleSided = false;
				if(materialModel instanceof MaterialModelV2){
					MaterialModelV2 model = (MaterialModelV2) materialModel;
					material.baseColorFactor = model.getBaseColorFactor();
					if(model.getBaseColorTextureIndex() == -1){
						material.baseColorTexture = null;
					}else{
						material.baseColorTexture.index = model.getBaseColorTextureIndex();
					}
					if(model.getNormalTextureIndex() == -1){
						material.normalTexture = null;
					}else{
						material.normalTexture.index = model.getNormalTextureIndex();
					}
					if(model.getMetallicRoughnessTextureIndex() == -1){
						material.specularTexture = null;
					}else{
						material.specularTexture.index = model.getMetallicRoughnessTextureIndex();
					}
					material.doubleSided = model.isDoubleSided();
				}

			}
			material.initMaterialCommand(gltfRenderData, this);
			extrasToMaterial.put(materialModel.getName(), material);
		}
		return material;
	}
	
	public int obtainGlTexture(List<Runnable> gltfRenderData, TextureModel textureModel) {
		Integer glTexture = textureModelToGlTexture.get(textureModel);
		if(glTexture == null) {
			PixelData pixelData = PixelDatas.create(textureModel.getImageModel().getImageData());
			if (pixelData == null)
			{
				MCglTF.logger.warn("Could not extract pixel data from image");
				pixelData = PixelDatas.createErrorPixelData();
			}
			
			Integer glTextureNew = GL11.glGenTextures();
			gltfRenderData.add(() -> GL11.glDeleteTextures(glTextureNew));
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureNew);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, pixelData.getWidth(), pixelData.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelData.getPixelsRGBA());
			
			int minFilter = Optionals.of(
				textureModel.getMinFilter(), 
				GL11.GL_NEAREST_MIPMAP_LINEAR);
			int magFilter = Optionals.of(
				textureModel.getMagFilter(),
				GL11.GL_LINEAR);
			int wrapS = Optionals.of(
				textureModel.getWrapS(),
				GL11.GL_REPEAT);
			int wrapT = Optionals.of(
				textureModel.getWrapT(),
				GL11.GL_REPEAT);
			
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT);
			
			textureModelToGlTexture.put(textureModel, glTextureNew);
			
			return glTextureNew;
		}
		else {
			return glTexture;
		}
	}
	
	public AccessorModel obtainNormalsAccessorModel(AccessorModel positionsAccessorModel) {
		AccessorModel normalsAccessorModel = positionsAccessorModelToNormalsAccessorModel.get(positionsAccessorModel);
		if(normalsAccessorModel == null) {
			int count = positionsAccessorModel.getCount();
			int numTriangles = count / 3;
			normalsAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, "");
			positionsAccessorModelToNormalsAccessorModel.put(positionsAccessorModel, normalsAccessorModel);
			AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
			AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
			float vertex0[] = new float[3];
			float vertex1[] = new float[3];
			float vertex2[] = new float[3];
			float edge01[] = new float[3];
			float edge02[] = new float[3];
			float cross[] = new float[3];
			float normal[] = new float[3];
			for(int i = 0; i < numTriangles; i++) {
				int index0 = i * 3;
				int index1 = index0 + 1;
				int index2 = index0 + 2;
				
				vertex0[0] = positionsAccessorData.get(index0, 0);
				vertex0[1] = positionsAccessorData.get(index0, 1);
				vertex0[2] = positionsAccessorData.get(index0, 2);
				
				vertex1[0] = positionsAccessorData.get(index1, 0);
				vertex1[1] = positionsAccessorData.get(index1, 1);
				vertex1[2] = positionsAccessorData.get(index1, 2);
				
				vertex2[0] = positionsAccessorData.get(index2, 0);
				vertex2[1] = positionsAccessorData.get(index2, 1);
				vertex2[2] = positionsAccessorData.get(index2, 2);
				
				MathUtils.subtract(vertex1, vertex0, edge01);
				MathUtils.subtract(vertex2, vertex0, edge02);
				MathUtils.cross(edge01, edge02, cross);
				MathUtils.normalize(cross, normal);
				
				normalsAccessorData.set(index0, 0, normal[0]);
				normalsAccessorData.set(index0, 1, normal[1]);
				normalsAccessorData.set(index0, 2, normal[2]);
				
				normalsAccessorData.set(index1, 0, normal[0]);
				normalsAccessorData.set(index1, 1, normal[1]);
				normalsAccessorData.set(index1, 2, normal[2]);
				
				normalsAccessorData.set(index2, 0, normal[0]);
				normalsAccessorData.set(index2, 1, normal[1]);
				normalsAccessorData.set(index2, 2, normal[2]);
			}
		}
		return normalsAccessorModel;
	}
	
	/**
	 * Found this simple normals to tangent algorithm here:</br>
	 * <a href="https://stackoverflow.com/questions/55464852/how-to-find-a-randomic-vector-orthogonal-to-a-given-vector">How to find a randomic Vector orthogonal to a given Vector</a>
	 */
	public AccessorModel obtainTangentsAccessorModel(AccessorModel normalsAccessorModel) {
		AccessorModel tangentsAccessorModel = normalsAccessorModelToTangentsAccessorModel.get(normalsAccessorModel);
		if(tangentsAccessorModel == null) {
			int count = normalsAccessorModel.getCount();
			tangentsAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, "");
			normalsAccessorModelToTangentsAccessorModel.put(normalsAccessorModel, tangentsAccessorModel);
			AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
			AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
			float[] normal0 = new float[3];
			float[] normal1 = new float[3];
			float[] cross = new float[3];
			float[] tangent = new float[3];
			
			for(int i = 0; i < count; i++) {
				normal0[0] = normalsAccessorData.get(i, 0);
				normal0[1] = normalsAccessorData.get(i, 1);
				normal0[2] = normalsAccessorData.get(i, 2);
				
				normal1[0] = -normal0[2];
				normal1[1] = normal0[0];
				normal1[2] = normal0[1];
				
				MathUtils.cross(normal0, normal1, cross);
				MathUtils.normalize(cross, tangent);
				
				tangentsAccessorData.set(i, 0, tangent[0]);
				tangentsAccessorData.set(i, 1, tangent[1]);
				tangentsAccessorData.set(i, 2, tangent[2]);
				tangentsAccessorData.set(i, 3, 1.0F);
			}
		}
		return tangentsAccessorModel;
	}
	
	public AccessorModel obtainTangentsAccessorModel(MeshPrimitiveModel meshPrimitiveModel, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel texcoordsAccessorModel) {
		AccessorModel tangentsAccessorModel = meshPrimitiveModelToTangentsAccessorModel.get(meshPrimitiveModel);
		if(tangentsAccessorModel == null) {
			int count = positionsAccessorModel.getCount();
			int numFaces = count / 3;
			tangentsAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, "");
			meshPrimitiveModelToTangentsAccessorModel.put(meshPrimitiveModel, tangentsAccessorModel);
			AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
			AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
			AccessorData texcoordsAccessorData = AccessorDatas.create(texcoordsAccessorModel);
			AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
			
			MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

				@Override
				public int getNumFaces() {
					return numFaces;
				}

				@Override
				public int getNumVerticesOfFace(int face) {
					return 3;
				}

				@Override
				public void getPosition(float[] posOut, int face, int vert) {
					int index = (face * 3) + vert;
					posOut[0] = positionsAccessorData.get(index, 0);
					posOut[1] = positionsAccessorData.get(index, 1);
					posOut[2] = positionsAccessorData.get(index, 2);
				}

				@Override
				public void getNormal(float[] normOut, int face, int vert) {
					int index = (face * 3) + vert;
					normOut[0] = normalsAccessorData.get(index, 0);
					normOut[1] = normalsAccessorData.get(index, 1);
					normOut[2] = normalsAccessorData.get(index, 2);
				}

				@Override
				public void getTexCoord(float[] texOut, int face, int vert) {
					int index = (face * 3) + vert;
					texOut[0] = texcoordsAccessorData.getFloat(index, 0);
					texOut[1] = texcoordsAccessorData.getFloat(index, 1);
				}

				@Override
				public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
					int index = (face * 3) + vert;
					tangentsAccessorData.set(index, 0, tangent[0]);
					tangentsAccessorData.set(index, 1, tangent[1]);
					tangentsAccessorData.set(index, 2, tangent[2]);
					tangentsAccessorData.set(index, 3, -sign);
				}

				@Override
				public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
					//Do nothing
				}
				
			});
		}
		return tangentsAccessorModel;
	}
	
	public AccessorModel obtainVec4ColorsAccessorModel(AccessorModel colorsAccessorModel) {
		if(colorsAccessorModel.getElementType() == ElementType.VEC3) {
			AccessorModel colorsVec4AccessorModel = colorsAccessorModelVec3ToVec4.get(colorsAccessorModel);
			if(colorsVec4AccessorModel == null) {
				int count = colorsAccessorModel.getCount();
				colorsVec4AccessorModel = AccessorModelCreation.createAccessorModel(colorsAccessorModel.getComponentType(), count, ElementType.VEC4, "");
				colorsAccessorModelVec3ToVec4.put(colorsAccessorModel, colorsVec4AccessorModel);
				AccessorData accessorData = AccessorDatas.create(colorsVec4AccessorModel);
				if(accessorData instanceof AccessorByteData) {
					AccessorByteData colorsVec4AccessorData = (AccessorByteData) accessorData;
					AccessorByteData colorsAccessorData = AccessorDatas.createByte(colorsAccessorModel);
					if(colorsAccessorData.isUnsigned()) {
						for(int i = 0; i < count; i++) {
							colorsVec4AccessorData.set(i, 0, colorsAccessorData.get(i, 0));
							colorsVec4AccessorData.set(i, 1, colorsAccessorData.get(i, 1));
							colorsVec4AccessorData.set(i, 2, colorsAccessorData.get(i, 2));
							colorsVec4AccessorData.set(i, 3, (byte) -1);
						}
					}
					else {
						for(int i = 0; i < count; i++) {
							colorsVec4AccessorData.set(i, 0, colorsAccessorData.get(i, 0));
							colorsVec4AccessorData.set(i, 1, colorsAccessorData.get(i, 1));
							colorsVec4AccessorData.set(i, 2, colorsAccessorData.get(i, 2));
							colorsVec4AccessorData.set(i, 3, Byte.MAX_VALUE);
						}
					}
				}
				else if(accessorData instanceof AccessorShortData) {
					AccessorShortData colorsVec4AccessorData = (AccessorShortData) accessorData;
					AccessorShortData colorsAccessorData = AccessorDatas.createShort(colorsAccessorModel);
					if(colorsAccessorData.isUnsigned()) {
						for(int i = 0; i < count; i++) {
							colorsVec4AccessorData.set(i, 0, colorsAccessorData.get(i, 0));
							colorsVec4AccessorData.set(i, 1, colorsAccessorData.get(i, 1));
							colorsVec4AccessorData.set(i, 2, colorsAccessorData.get(i, 2));
							colorsVec4AccessorData.set(i, 3, (short) -1);
						}
					}
					else {
						for(int i = 0; i < count; i++) {
							colorsVec4AccessorData.set(i, 0, colorsAccessorData.get(i, 0));
							colorsVec4AccessorData.set(i, 1, colorsAccessorData.get(i, 1));
							colorsVec4AccessorData.set(i, 2, colorsAccessorData.get(i, 2));
							colorsVec4AccessorData.set(i, 3, Short.MAX_VALUE);
						}
					}
				}
				else if(accessorData instanceof AccessorFloatData) {
					AccessorFloatData colorsVec4AccessorData = (AccessorFloatData) accessorData;
					AccessorFloatData colorsAccessorData = AccessorDatas.createFloat(colorsAccessorModel);
					for(int i = 0; i < count; i++) {
						colorsVec4AccessorData.set(i, 0, colorsAccessorData.get(i, 0));
						colorsVec4AccessorData.set(i, 1, colorsAccessorData.get(i, 1));
						colorsVec4AccessorData.set(i, 2, colorsAccessorData.get(i, 2));
						colorsVec4AccessorData.set(i, 3, 1.0F);
					}
				}
			}
			return colorsVec4AccessorModel;
		}
		return colorsAccessorModel;
	}
	
	public AccessorModel obtainUnsignedJointsModel(AccessorModel accessorModel) {
		AccessorModel unsignedAccessorModel = jointsAccessorModelUnsignedLookup.get(accessorModel);
		if(unsignedAccessorModel == null) {
			int count = accessorModel.getCount();
			unsignedAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_INT, count, ElementType.VEC4, "");
			AccessorIntData unsignedAccessorData = AccessorDatas.createInt(unsignedAccessorModel);
			if(accessorModel.getComponentDataType() == short.class) {
				AccessorShortData accessorData = AccessorDatas.createShort(accessorModel);
				for(int i = 0; i < count; i++) {
					unsignedAccessorData.set(i, 0, Short.toUnsignedInt(accessorData.get(i, 0)));
					unsignedAccessorData.set(i, 1, Short.toUnsignedInt(accessorData.get(i, 1)));
					unsignedAccessorData.set(i, 2, Short.toUnsignedInt(accessorData.get(i, 2)));
					unsignedAccessorData.set(i, 3, Short.toUnsignedInt(accessorData.get(i, 3)));
				}
			}
			else {
				AccessorByteData accessorData = AccessorDatas.createByte(accessorModel);
				for(int i = 0; i < count; i++) {
					unsignedAccessorData.set(i, 0, Byte.toUnsignedInt(accessorData.get(i, 0)));
					unsignedAccessorData.set(i, 1, Byte.toUnsignedInt(accessorData.get(i, 1)));
					unsignedAccessorData.set(i, 2, Byte.toUnsignedInt(accessorData.get(i, 2)));
					unsignedAccessorData.set(i, 3, Byte.toUnsignedInt(accessorData.get(i, 3)));
				}
			}
			jointsAccessorModelUnsignedLookup.put(accessorModel, unsignedAccessorModel);
		}
		return unsignedAccessorModel;
	}
	
	public AccessorModel obtainDequantizedWeightsModel(AccessorModel accessorModel) {
		AccessorModel dequantizedAccessorModel = weightsAccessorModelDequantizedLookup.get(accessorModel);
		if(dequantizedAccessorModel == null) {
			if(accessorModel.getComponentDataType() != float.class) {
				AccessorData accessorData = AccessorDatas.create(accessorModel);
				int count = accessorModel.getCount();
				dequantizedAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, "");
				AccessorFloatData dequantizedAccessorData = AccessorDatas.createFloat(dequantizedAccessorModel);
				for(int i = 0; i < count; i++) {
					dequantizedAccessorData.set(i, 0, accessorData.getFloat(i, 0));
					dequantizedAccessorData.set(i, 1, accessorData.getFloat(i, 1));
					dequantizedAccessorData.set(i, 2, accessorData.getFloat(i, 2));
					dequantizedAccessorData.set(i, 3, accessorData.getFloat(i, 3));
				}
				weightsAccessorModelDequantizedLookup.put(accessorModel, dequantizedAccessorModel);
			}
			else {
				return accessorModel;
			}
		}
		return dequantizedAccessorModel;
	}
	
	public AccessorModel obtainVec3FloatMorphedModel(NodeModel nodeModel, MeshModel meshModel, List<Runnable> command, AccessorModel baseAccessorModel, List<AccessorFloatData> targetAccessorDatas) {
		AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
		AccessorFloatData baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
		AccessorFloatData morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
		
		float weights[] = new float[targetAccessorDatas.size()];
		int numComponents = 3;
		int numElements = morphedAccessorData.getNumElements();
		
		List<Runnable> morphingCommands = new ArrayList<Runnable>(numElements * numComponents);
		for(int element = 0; element < numElements; element++) {
			for(int component = 0; component < numComponents; component++) {
				int e = element;
				int c = component;
				morphingCommands.add(() -> {
					float r = baseAccessorData.get(e, c);
					for(int i = 0; i < weights.length; i++) {
						AccessorFloatData target = targetAccessorDatas.get(i);
						if(target != null) {
							r += weights[i] * target.get(e, c);
						}
					}
					morphedAccessorData.set(e, c, r);
				});
			}
		}
		
		command.add(() -> {
			if(nodeModel.getWeights() != null) System.arraycopy(nodeModel.getWeights(), 0, weights, 0, weights.length);
			else if(meshModel.getWeights() != null) System.arraycopy(meshModel.getWeights(), 0, weights, 0, weights.length);
			
			morphingCommands.parallelStream().forEach(Runnable::run);
		});
		return morphedAccessorModel;
	}
	
	public Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> obtainUnindexed(MeshPrimitiveModel meshPrimitiveModel) {
		Pair<Map<String, AccessorModel>, List<Map<String, AccessorModel>>> unindexed;
		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if(indicesAccessorModel != null) {
			unindexed = meshPrimitiveModelToUnindexed.get(meshPrimitiveModel);
			if(unindexed == null) {
				int indices[] = AccessorDataUtils.readInts(AccessorDatas.create(indicesAccessorModel));
				Map<String, AccessorModel> attributes = meshPrimitiveModel.getAttributes();
				Map<String, AccessorModel> attributesUnindexed = new LinkedHashMap<String, AccessorModel>(attributes.size());
				attributes.forEach((name, attribute) -> {
					ElementType elementType = attribute.getElementType();
					int size = elementType.getNumComponents();
					AccessorModel accessorModel = AccessorModelCreation.createAccessorModel(attribute.getComponentType(), indices.length, elementType, "");
					attributesUnindexed.put(name, accessorModel);
					AccessorData accessorData = AccessorDatas.create(accessorModel);
					if(accessorData instanceof AccessorByteData) {
						AccessorByteData accessorDataUnindexed = (AccessorByteData) accessorData;
						AccessorByteData accessorDataIndexed = AccessorDatas.createByte(attribute);
						for(int i = 0; i < indices.length; i++) {
							int index = indices[i];
							for(int j = 0; j < size; j++) {
								accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
							}
						}
					}
					else if(accessorData instanceof AccessorShortData) {
						AccessorShortData accessorDataUnindexed = (AccessorShortData) accessorData;
						AccessorShortData accessorDataIndexed = AccessorDatas.createShort(attribute);
						for(int i = 0; i < indices.length; i++) {
							int index = indices[i];
							for(int j = 0; j < size; j++) {
								accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
							}
						}
					}
					else if(accessorData instanceof AccessorIntData) {
						AccessorIntData accessorDataUnindexed = (AccessorIntData) accessorData;
						AccessorIntData accessorDataIndexed = AccessorDatas.createInt(attribute);
						for(int i = 0; i < indices.length; i++) {
							int index = indices[i];
							for(int j = 0; j < size; j++) {
								accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
							}
						}
					}
					else if(accessorData instanceof AccessorFloatData) {
						AccessorFloatData accessorDataUnindexed = (AccessorFloatData) accessorData;
						AccessorFloatData accessorDataIndexed = AccessorDatas.createFloat(attribute);
						for(int i = 0; i < indices.length; i++) {
							int index = indices[i];
							for(int j = 0; j < size; j++) {
								accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
							}
						}
					}
				});
				
				List<Map<String, AccessorModel>> targets = meshPrimitiveModel.getTargets();
				List<Map<String, AccessorModel>> targetsUnindexed = new ArrayList<Map<String, AccessorModel>>(targets.size());
				targets.forEach((target) -> {
					Map<String, AccessorModel> targetUnindexed = new LinkedHashMap<String, AccessorModel>(target.size());
					targetsUnindexed.add(targetUnindexed);
					target.forEach((name, attribute) -> {
						ElementType elementType = attribute.getElementType();
						int size = elementType.getNumComponents();
						AccessorModel accessorModel = AccessorModelCreation.createAccessorModel(attribute.getComponentType(), indices.length, elementType, "");
						targetUnindexed.put(name, accessorModel);
						AccessorData accessorData = AccessorDatas.create(accessorModel);
						if(accessorData instanceof AccessorByteData) {
							AccessorByteData accessorDataUnindexed = (AccessorByteData) accessorData;
							AccessorByteData accessorDataIndexed = AccessorDatas.createByte(attribute);
							for(int i = 0; i < indices.length; i++) {
								int index = indices[i];
								for(int j = 0; j < size; j++) {
									accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
								}
							}
						}
						else if(accessorData instanceof AccessorShortData) {
							AccessorShortData accessorDataUnindexed = (AccessorShortData) accessorData;
							AccessorShortData accessorDataIndexed = AccessorDatas.createShort(attribute);
							for(int i = 0; i < indices.length; i++) {
								int index = indices[i];
								for(int j = 0; j < size; j++) {
									accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
								}
							}
						}
						else if(accessorData instanceof AccessorIntData) {
							AccessorIntData accessorDataUnindexed = (AccessorIntData) accessorData;
							AccessorIntData accessorDataIndexed = AccessorDatas.createInt(attribute);
							for(int i = 0; i < indices.length; i++) {
								int index = indices[i];
								for(int j = 0; j < size; j++) {
									accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
								}
							}
						}
						else if(accessorData instanceof AccessorFloatData) {
							AccessorFloatData accessorDataUnindexed = (AccessorFloatData) accessorData;
							AccessorFloatData accessorDataIndexed = AccessorDatas.createFloat(attribute);
							for(int i = 0; i < indices.length; i++) {
								int index = indices[i];
								for(int j = 0; j < size; j++) {
									accessorDataUnindexed.set(i, j, accessorDataIndexed.get(index, j));
								}
							}
						}
					});
				});
				unindexed = Pair.of(attributesUnindexed, targetsUnindexed);
				meshPrimitiveModelToUnindexed.put(meshPrimitiveModel, unindexed);
			}
		}
		else unindexed = Pair.of(meshPrimitiveModel.getAttributes(), meshPrimitiveModel.getTargets());
		return unindexed;
	}
	
	public List<Runnable> createSoftwareSkinningCommands(int pointCount, float[][] jointMatrices, Map<String, AccessorModel> attributes, AccessorFloatData inputPositionsAccessorData, AccessorFloatData inputNormalsAccessorData, AccessorFloatData inputTangentsAccessorData, AccessorFloatData outputPositionsAccessorData, AccessorFloatData outputNormalsAccessorData, AccessorFloatData outputTangentsAccessorData) {
		int skinningAttributeCount = 0;
		for(String name : attributes.keySet()) {
			if(name.startsWith("JOINTS_")) {
				skinningAttributeCount += 1;
			}
		}
		AccessorIntData[] jointsAccessorDatas = new AccessorIntData[skinningAttributeCount];
		AccessorFloatData[] weightsAccessorDatas = new AccessorFloatData[skinningAttributeCount];
		attributes.forEach((name, attribute) -> {
			if(name.startsWith("JOINTS_")) jointsAccessorDatas[Integer.parseInt(name.substring("JOINTS_".length()))] = AccessorDatas.createInt(obtainUnsignedJointsModel(attribute));
			else if(name.startsWith("WEIGHTS_")) weightsAccessorDatas[Integer.parseInt(name.substring("WEIGHTS_".length()))] = AccessorDatas.createFloat(obtainDequantizedWeightsModel(attribute));
		});
		
		List<Runnable> commands = new ArrayList<Runnable>(pointCount);
		for(int point = 0; point < pointCount; point++) {
			int p = point;
			commands.add(() -> {
				float sm00 = 0;
				float sm01 = 0;
				float sm02 = 0;
				float sm03 = 0;
				float sm10 = 0;
				float sm11 = 0;
				float sm12 = 0;
				float sm13 = 0;
				float sm20 = 0;
				float sm21 = 0;
				float sm22 = 0;
				float sm23 = 0;
				
				for(int i = 0; i < jointsAccessorDatas.length; i++) {
					AccessorIntData jointsAccessorData = jointsAccessorDatas[i];
					float[] jmx = jointMatrices[jointsAccessorData.get(p, 0)];
					float[] jmy = jointMatrices[jointsAccessorData.get(p, 1)];
					float[] jmz = jointMatrices[jointsAccessorData.get(p, 2)];
					float[] jmw = jointMatrices[jointsAccessorData.get(p, 3)];
					
					AccessorFloatData weightsAccessorData = weightsAccessorDatas[i];
					float wx = weightsAccessorData.get(p, 0);
					float wy = weightsAccessorData.get(p, 1);
					float wz = weightsAccessorData.get(p, 2);
					float ww = weightsAccessorData.get(p, 3);
					
					sm00 += wx * jmx[ 0] + wy * jmy[ 0] + wz * jmz[ 0] + ww * jmw[ 0];
					sm01 += wx * jmx[ 4] + wy * jmy[ 4] + wz * jmz[ 4] + ww * jmw[ 4];
					sm02 += wx * jmx[ 8] + wy * jmy[ 8] + wz * jmz[ 8] + ww * jmw[ 8];
					sm03 += wx * jmx[12] + wy * jmy[12] + wz * jmz[12] + ww * jmw[12];
					sm10 += wx * jmx[ 1] + wy * jmy[ 1] + wz * jmz[ 1] + ww * jmw[ 1];
					sm11 += wx * jmx[ 5] + wy * jmy[ 5] + wz * jmz[ 5] + ww * jmw[ 5];
					sm12 += wx * jmx[ 9] + wy * jmy[ 9] + wz * jmz[ 9] + ww * jmw[ 9];
					sm13 += wx * jmx[13] + wy * jmy[13] + wz * jmz[13] + ww * jmw[13];
					sm20 += wx * jmx[ 2] + wy * jmy[ 2] + wz * jmz[ 2] + ww * jmw[ 2];
					sm21 += wx * jmx[ 6] + wy * jmy[ 6] + wz * jmz[ 6] + ww * jmw[ 6];
					sm22 += wx * jmx[10] + wy * jmy[10] + wz * jmz[10] + ww * jmw[10];
					sm23 += wx * jmx[14] + wy * jmy[14] + wz * jmz[14] + ww * jmw[14];
				}
				
				float px = inputPositionsAccessorData.get(p, 0);
				float py = inputPositionsAccessorData.get(p, 1);
				float pz = inputPositionsAccessorData.get(p, 2);
				
				outputPositionsAccessorData.set(p, 0, sm00 * px + sm01 * py + sm02 * pz + sm03);
				outputPositionsAccessorData.set(p, 1, sm10 * px + sm11 * py + sm12 * pz + sm13);
				outputPositionsAccessorData.set(p, 2, sm20 * px + sm21 * py + sm22 * pz + sm23);
				
				float nx = inputNormalsAccessorData.get(p, 0);
				float ny = inputNormalsAccessorData.get(p, 1);
				float nz = inputNormalsAccessorData.get(p, 2);
				
				outputNormalsAccessorData.set(p, 0, sm00 * nx + sm01 * ny + sm02 * nz);
				outputNormalsAccessorData.set(p, 1, sm10 * nx + sm11 * ny + sm12 * nz);
				outputNormalsAccessorData.set(p, 2, sm20 * nx + sm21 * ny + sm22 * nz);
				
				float tx = inputTangentsAccessorData.get(p, 0);
				float ty = inputTangentsAccessorData.get(p, 1);
				float tz = inputTangentsAccessorData.get(p, 2);
				
				outputTangentsAccessorData.set(p, 0, sm00 * tx + sm01 * ty + sm02 * tz);
				outputTangentsAccessorData.set(p, 1, sm10 * tx + sm11 * ty + sm12 * tz);
				outputTangentsAccessorData.set(p, 2, sm20 * tx + sm21 * ty + sm22 * tz);
			});
		}
		return commands;
	}
	
	public boolean createMorphTarget(List<Map<String, AccessorModel>> morphTargets, List<AccessorFloatData> targetAccessorDatas, String attributeName) {
		boolean isMorphableAttribute = false;
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get(attributeName);
			if(accessorModel != null) {
				isMorphableAttribute = true;
				targetAccessorDatas.add(AccessorDatas.createFloat(accessorModel));
			}
			else targetAccessorDatas.add(null);
		}
		return isMorphableAttribute;
	}
	
	public boolean createPositionNormalMorphTarget(List<Map<String, AccessorModel>> morphTargets, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, List<AccessorFloatData> positionTargetAccessorDatas, List<AccessorFloatData> normalTargetAccessorDatas) {
		boolean isMorphableAttribute = false;
		int count = positionsAccessorModel.getCount();
		int numTriangles = count / 3;
		AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
		AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get("POSITION");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData deltaPositionsAccessorData = AccessorDatas.createFloat(accessorModel);
				positionTargetAccessorDatas.add(deltaPositionsAccessorData);
				AccessorFloatData normalTargetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				normalTargetAccessorDatas.add(normalTargetAccessorData);
				float[] vertex0 = new float[3];
				float[] vertex1 = new float[3];
				float[] vertex2 = new float[3];
				float[] edge01 = new float[3];
				float[] edge02 = new float[3];
				float[] cross = new float[3];
				float[] normal0 = new float[3];
				float[] normal1 = new float[3];
				for(int i = 0; i < numTriangles; i++) {
					int index0 = i * 3;
					int index1 = index0 + 1;
					int index2 = index0 + 2;
					
					vertex0[0] = positionsAccessorData.get(index0, 0) + deltaPositionsAccessorData.get(index0, 0);
					vertex0[1] = positionsAccessorData.get(index0, 1) + deltaPositionsAccessorData.get(index0, 1);
					vertex0[2] = positionsAccessorData.get(index0, 2) + deltaPositionsAccessorData.get(index0, 2);
					
					vertex1[0] = positionsAccessorData.get(index1, 0) + deltaPositionsAccessorData.get(index1, 0);
					vertex1[1] = positionsAccessorData.get(index1, 1) + deltaPositionsAccessorData.get(index1, 1);
					vertex1[2] = positionsAccessorData.get(index1, 2) + deltaPositionsAccessorData.get(index1, 2);
					
					vertex2[0] = positionsAccessorData.get(index2, 0) + deltaPositionsAccessorData.get(index2, 0);
					vertex2[1] = positionsAccessorData.get(index2, 1) + deltaPositionsAccessorData.get(index2, 1);
					vertex2[2] = positionsAccessorData.get(index2, 2) + deltaPositionsAccessorData.get(index2, 2);
					
					normal0[0] = normalsAccessorData.get(index0, 0);
					normal0[1] = normalsAccessorData.get(index0, 1);
					normal0[2] = normalsAccessorData.get(index0, 2);
					
					MathUtils.subtract(vertex1, vertex0, edge01);
					MathUtils.subtract(vertex2, vertex0, edge02);
					MathUtils.cross(edge01, edge02, cross);
					MathUtils.normalize(cross, normal1);
					
					MathUtils.subtract(normal1, normal0, normal1);
					
					normalTargetAccessorData.set(index0, 0, normal1[0]);
					normalTargetAccessorData.set(index0, 1, normal1[1]);
					normalTargetAccessorData.set(index0, 2, normal1[2]);
					
					normalTargetAccessorData.set(index1, 0, normal1[0]);
					normalTargetAccessorData.set(index1, 1, normal1[1]);
					normalTargetAccessorData.set(index1, 2, normal1[2]);
					
					normalTargetAccessorData.set(index2, 0, normal1[0]);
					normalTargetAccessorData.set(index2, 1, normal1[1]);
					normalTargetAccessorData.set(index2, 2, normal1[2]);
				}
			}
			else {
				positionTargetAccessorDatas.add(null);
				normalTargetAccessorDatas.add(null);
			}
		}
		return isMorphableAttribute;
	}
	
	public boolean createPositionNormalTangentMorphTarget(List<Map<String, AccessorModel>> morphTargets, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel, List<AccessorFloatData> positionTargetAccessorDatas, List<AccessorFloatData> normalTargetAccessorDatas, List<AccessorFloatData> tangentTargetAccessorDatas) {
		boolean isMorphableAttribute = false;
		int count = positionsAccessorModel.getCount();
		int numTriangles = count / 3;
		AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
		AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
		AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get("POSITION");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData deltaPositionsAccessorData = AccessorDatas.createFloat(accessorModel);
				positionTargetAccessorDatas.add(deltaPositionsAccessorData);
				AccessorFloatData normalTargetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				normalTargetAccessorDatas.add(normalTargetAccessorData);
				AccessorFloatData tangentTargetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				tangentTargetAccessorDatas.add(tangentTargetAccessorData);
				float[] vertex0 = new float[3];
				float[] vertex1 = new float[3];
				float[] vertex2 = new float[3];
				float[] edge01 = new float[3];
				float[] edge02 = new float[3];
				float[] cross = new float[3];
				float[] normal0 = new float[3];
				float[] normal1 = new float[3];
				float[] normal2 = new float[3];
				float[] tangent0 = new float[3];
				float[] tangent1 = new float[3];
				for(int i = 0; i < numTriangles; i++) {
					int index0 = i * 3;
					int index1 = index0 + 1;
					int index2 = index0 + 2;
					
					vertex0[0] = positionsAccessorData.get(index0, 0) + deltaPositionsAccessorData.get(index0, 0);
					vertex0[1] = positionsAccessorData.get(index0, 1) + deltaPositionsAccessorData.get(index0, 1);
					vertex0[2] = positionsAccessorData.get(index0, 2) + deltaPositionsAccessorData.get(index0, 2);
					
					vertex1[0] = positionsAccessorData.get(index1, 0) + deltaPositionsAccessorData.get(index1, 0);
					vertex1[1] = positionsAccessorData.get(index1, 1) + deltaPositionsAccessorData.get(index1, 1);
					vertex1[2] = positionsAccessorData.get(index1, 2) + deltaPositionsAccessorData.get(index1, 2);
					
					vertex2[0] = positionsAccessorData.get(index2, 0) + deltaPositionsAccessorData.get(index2, 0);
					vertex2[1] = positionsAccessorData.get(index2, 1) + deltaPositionsAccessorData.get(index2, 1);
					vertex2[2] = positionsAccessorData.get(index2, 2) + deltaPositionsAccessorData.get(index2, 2);
					
					normal0[0] = normalsAccessorData.get(index0, 0);
					normal0[1] = normalsAccessorData.get(index0, 1);
					normal0[2] = normalsAccessorData.get(index0, 2);
					
					tangent0[0] = tangentsAccessorData.get(index0, 0);
					tangent0[1] = tangentsAccessorData.get(index0, 1);
					tangent0[2] = tangentsAccessorData.get(index0, 2);
					
					MathUtils.subtract(vertex1, vertex0, edge01);
					MathUtils.subtract(vertex2, vertex0, edge02);
					MathUtils.cross(edge01, edge02, cross);
					MathUtils.normalize(cross, normal1);
					
					normal2[0] = -normal1[2];
					normal2[1] = normal1[0];
					normal2[2] = normal1[1];
					
					MathUtils.cross(normal1, normal2, cross);
					MathUtils.normalize(cross, tangent1);
					
					MathUtils.subtract(normal1, normal0, normal1);
					MathUtils.subtract(tangent1, tangent0, tangent1);
					
					normalTargetAccessorData.set(index0, 0, normal1[0]);
					normalTargetAccessorData.set(index0, 1, normal1[1]);
					normalTargetAccessorData.set(index0, 2, normal1[2]);
					
					tangentTargetAccessorData.set(index0, 0, tangent1[0]);
					tangentTargetAccessorData.set(index0, 1, tangent1[1]);
					tangentTargetAccessorData.set(index0, 2, tangent1[2]);
					
					normalTargetAccessorData.set(index1, 0, normal1[0]);
					normalTargetAccessorData.set(index1, 1, normal1[1]);
					normalTargetAccessorData.set(index1, 2, normal1[2]);
					
					tangentTargetAccessorData.set(index1, 0, tangent1[0]);
					tangentTargetAccessorData.set(index1, 1, tangent1[1]);
					tangentTargetAccessorData.set(index1, 2, tangent1[2]);
					
					normalTargetAccessorData.set(index2, 0, normal1[0]);
					normalTargetAccessorData.set(index2, 1, normal1[1]);
					normalTargetAccessorData.set(index2, 2, normal1[2]);
					
					tangentTargetAccessorData.set(index2, 0, tangent1[0]);
					tangentTargetAccessorData.set(index2, 1, tangent1[1]);
					tangentTargetAccessorData.set(index2, 2, tangent1[2]);
				}
			}
			else {
				positionTargetAccessorDatas.add(null);
				normalTargetAccessorDatas.add(null);
				tangentTargetAccessorDatas.add(null);
			}
		}
		return isMorphableAttribute;
	}
	
	public boolean createNormalTangentMorphTarget(List<Map<String, AccessorModel>> morphTargets, AccessorModel normalsAccessorModel, AccessorModel tangentsAccessorModel, List<AccessorFloatData> normalTargetAccessorDatas, List<AccessorFloatData> tangentTargetAccessorDatas) {
		boolean isMorphableAttribute = false;
		int count = normalsAccessorModel.getCount();
		AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
		AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get("NORMAL");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData deltaNormalsAccessorData = AccessorDatas.createFloat(accessorModel);
				normalTargetAccessorDatas.add(deltaNormalsAccessorData);
				AccessorFloatData tangentTargetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				tangentTargetAccessorDatas.add(tangentTargetAccessorData);
				float[] normal0 = new float[3];
				float[] normal1 = new float[3];
				float[] cross = new float[3];
				float[] tangent = new float[3];
				
				for(int i = 0; i < count; i++) {
					normal0[0] = normalsAccessorData.get(i, 0) + deltaNormalsAccessorData.get(i, 0);
					normal0[1] = normalsAccessorData.get(i, 1) + deltaNormalsAccessorData.get(i, 1);
					normal0[2] = normalsAccessorData.get(i, 2) + deltaNormalsAccessorData.get(i, 2);
					
					normal1[0] = -normal0[2];
					normal1[1] = normal0[0];
					normal1[2] = normal0[1];
					
					MathUtils.cross(normal0, normal1, cross);
					MathUtils.normalize(cross, tangent);
					
					tangentTargetAccessorData.set(i, 0, tangent[0] - tangentsAccessorData.get(i, 0));
					tangentTargetAccessorData.set(i, 1, tangent[1] - tangentsAccessorData.get(i, 1));
					tangentTargetAccessorData.set(i, 2, tangent[2] - tangentsAccessorData.get(i, 2));
				}
			}
			else {
				normalTargetAccessorDatas.add(null);
				tangentTargetAccessorDatas.add(null);
			}
		}
		return isMorphableAttribute;
	}
	
	public boolean createTangentMorphTarget(List<Map<String, AccessorModel>> morphTargets, List<AccessorFloatData> targetAccessorDatas, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel texcoordsAccessorModel, String texcoordsAccessorName, AccessorModel tangentsAccessorModel) {
		boolean isMorphableAttribute = false;
		int count = positionsAccessorModel.getCount();
		int numFaces = count / 3;
		AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
		AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
		AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
		AccessorData texcoordsAccessorData = AccessorDatas.create(texcoordsAccessorModel);
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get("POSITION");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData targetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				targetAccessorDatas.add(targetAccessorData);
				AccessorFloatData deltaPositionsAccessorData = AccessorDatas.createFloat(accessorModel);
				accessorModel = morphTarget.get("NORMAL");
				if(accessorModel != null) {
					AccessorFloatData deltaNormalsAccessorData = AccessorDatas.createFloat(accessorModel);
					accessorModel = morphTarget.get(texcoordsAccessorName);
					if(accessorModel != null) {
						AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
						MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

							@Override
							public int getNumFaces() {
								return numFaces;
							}

							@Override
							public int getNumVerticesOfFace(int face) {
								return 3;
							}

							@Override
							public void getPosition(float[] posOut, int face, int vert) {
								int index = (face * 3) + vert;
								posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
								posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
								posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
							}

							@Override
							public void getNormal(float[] normOut, int face, int vert) {
								int index = (face * 3) + vert;
								normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
								normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
								normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
							}

							@Override
							public void getTexCoord(float[] texOut, int face, int vert) {
								int index = (face * 3) + vert;
								texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
								texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
							}

							@Override
							public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
								int index = (face * 3) + vert;
								tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
								tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
								tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
							}

							@Override
							public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
								//Do nothing
							}
							
						});
					}
					else {
						MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

							@Override
							public int getNumFaces() {
								return numFaces;
							}

							@Override
							public int getNumVerticesOfFace(int face) {
								return 3;
							}

							@Override
							public void getPosition(float[] posOut, int face, int vert) {
								int index = (face * 3) + vert;
								posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
								posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
								posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
							}

							@Override
							public void getNormal(float[] normOut, int face, int vert) {
								int index = (face * 3) + vert;
								normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
								normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
								normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
							}

							@Override
							public void getTexCoord(float[] texOut, int face, int vert) {
								int index = (face * 3) + vert;
								texOut[0] = texcoordsAccessorData.getFloat(index, 0);
								texOut[1] = texcoordsAccessorData.getFloat(index, 1);
							}

							@Override
							public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
								int index = (face * 3) + vert;
								tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
								tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
								tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
							}

							@Override
							public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
								//Do nothing
							}
							
						});
					}
				}
				else {
					accessorModel = morphTarget.get(texcoordsAccessorName);
					if(accessorModel != null) {
						AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
						MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

							@Override
							public int getNumFaces() {
								return numFaces;
							}

							@Override
							public int getNumVerticesOfFace(int face) {
								return 3;
							}

							@Override
							public void getPosition(float[] posOut, int face, int vert) {
								int index = (face * 3) + vert;
								posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
								posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
								posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
							}

							@Override
							public void getNormal(float[] normOut, int face, int vert) {
								int index = (face * 3) + vert;
								normOut[0] = normalsAccessorData.get(index, 0);
								normOut[1] = normalsAccessorData.get(index, 1);
								normOut[2] = normalsAccessorData.get(index, 2);
							}

							@Override
							public void getTexCoord(float[] texOut, int face, int vert) {
								int index = (face * 3) + vert;
								texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
								texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
							}

							@Override
							public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
								int index = (face * 3) + vert;
								tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
								tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
								tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
							}

							@Override
							public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
								//Do nothing
							}
							
						});
					}
					else {
						MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

							@Override
							public int getNumFaces() {
								return numFaces;
							}

							@Override
							public int getNumVerticesOfFace(int face) {
								return 3;
							}

							@Override
							public void getPosition(float[] posOut, int face, int vert) {
								int index = (face * 3) + vert;
								posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
								posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
								posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
							}

							@Override
							public void getNormal(float[] normOut, int face, int vert) {
								int index = (face * 3) + vert;
								normOut[0] = normalsAccessorData.get(index, 0);
								normOut[1] = normalsAccessorData.get(index, 1);
								normOut[2] = normalsAccessorData.get(index, 2);
							}

							@Override
							public void getTexCoord(float[] texOut, int face, int vert) {
								int index = (face * 3) + vert;
								texOut[0] = texcoordsAccessorData.getFloat(index, 0);
								texOut[1] = texcoordsAccessorData.getFloat(index, 1);
							}

							@Override
							public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
								int index = (face * 3) + vert;
								tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
								tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
								tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
							}

							@Override
							public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
								//Do nothing
							}
							
						});
					}
				}
				continue;
			}
			accessorModel = morphTarget.get("NORMAL");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData targetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				targetAccessorDatas.add(targetAccessorData);
				AccessorFloatData deltaNormalsAccessorData = AccessorDatas.createFloat(accessorModel);
				accessorModel = morphTarget.get(texcoordsAccessorName);
				if(accessorModel != null) {
					AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
					MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

						@Override
						public int getNumFaces() {
							return numFaces;
						}

						@Override
						public int getNumVerticesOfFace(int face) {
							return 3;
						}

						@Override
						public void getPosition(float[] posOut, int face, int vert) {
							int index = (face * 3) + vert;
							posOut[0] = positionsAccessorData.get(index, 0);
							posOut[1] = positionsAccessorData.get(index, 1);
							posOut[2] = positionsAccessorData.get(index, 2);
						}

						@Override
						public void getNormal(float[] normOut, int face, int vert) {
							int index = (face * 3) + vert;
							normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
							normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
							normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
						}

						@Override
						public void getTexCoord(float[] texOut, int face, int vert) {
							int index = (face * 3) + vert;
							texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
							texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
						}

						@Override
						public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
							int index = (face * 3) + vert;
							tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
							tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
							tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
						}

						@Override
						public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
							//Do nothing
						}
						
					});
				}
				else {
					MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

						@Override
						public int getNumFaces() {
							return numFaces;
						}

						@Override
						public int getNumVerticesOfFace(int face) {
							return 3;
						}

						@Override
						public void getPosition(float[] posOut, int face, int vert) {
							int index = (face * 3) + vert;
							posOut[0] = positionsAccessorData.get(index, 0);
							posOut[1] = positionsAccessorData.get(index, 1);
							posOut[2] = positionsAccessorData.get(index, 2);
						}

						@Override
						public void getNormal(float[] normOut, int face, int vert) {
							int index = (face * 3) + vert;
							normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
							normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
							normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
						}

						@Override
						public void getTexCoord(float[] texOut, int face, int vert) {
							int index = (face * 3) + vert;
							texOut[0] = texcoordsAccessorData.getFloat(index, 0);
							texOut[1] = texcoordsAccessorData.getFloat(index, 1);
						}

						@Override
						public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
							int index = (face * 3) + vert;
							tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
							tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
							tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
						}

						@Override
						public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
							//Do nothing
						}
						
					});
				}
				continue;
			}
			accessorModel = morphTarget.get(texcoordsAccessorName);
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData targetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				targetAccessorDatas.add(targetAccessorData);
				AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
				MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

					@Override
					public int getNumFaces() {
						return numFaces;
					}

					@Override
					public int getNumVerticesOfFace(int face) {
						return 3;
					}

					@Override
					public void getPosition(float[] posOut, int face, int vert) {
						int index = (face * 3) + vert;
						posOut[0] = positionsAccessorData.get(index, 0);
						posOut[1] = positionsAccessorData.get(index, 1);
						posOut[2] = positionsAccessorData.get(index, 2);
					}

					@Override
					public void getNormal(float[] normOut, int face, int vert) {
						int index = (face * 3) + vert;
						normOut[0] = normalsAccessorData.get(index, 0);
						normOut[1] = normalsAccessorData.get(index, 1);
						normOut[2] = normalsAccessorData.get(index, 2);
					}

					@Override
					public void getTexCoord(float[] texOut, int face, int vert) {
						int index = (face * 3) + vert;
						texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
						texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
					}

					@Override
					public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
						int index = (face * 3) + vert;
						tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
						tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
						tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
					}

					@Override
					public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
						//Do nothing
					}
					
				});
				continue;
			}
			targetAccessorDatas.add(null);
		}
		return isMorphableAttribute;
	}
	
	public boolean createTangentMorphTarget(List<Map<String, AccessorModel>> morphTargets, List<AccessorFloatData> targetAccessorDatas, AccessorModel positionsAccessorModel, AccessorModel normalsAccessorModel, AccessorModel texcoordsAccessorModel, String texcoordsAccessorName, AccessorModel tangentsAccessorModel, List<AccessorFloatData> normalTargetAccessorDatas) {
		boolean isMorphableAttribute = false;
		int count = positionsAccessorModel.getCount();
		int numFaces = count / 3;
		AccessorFloatData positionsAccessorData = AccessorDatas.createFloat(positionsAccessorModel);
		AccessorFloatData normalsAccessorData = AccessorDatas.createFloat(normalsAccessorModel);
		AccessorFloatData tangentsAccessorData = AccessorDatas.createFloat(tangentsAccessorModel);
		AccessorData texcoordsAccessorData = AccessorDatas.create(texcoordsAccessorModel);
		Iterator<AccessorFloatData> iterator = normalTargetAccessorDatas.iterator();
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorFloatData deltaNormalsAccessorData = iterator.next();
			AccessorModel accessorModel = morphTarget.get("POSITION");
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData targetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				targetAccessorDatas.add(targetAccessorData);
				AccessorFloatData deltaPositionsAccessorData = AccessorDatas.createFloat(accessorModel);
				accessorModel = morphTarget.get(texcoordsAccessorName);
				if(accessorModel != null) {
					AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
					MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

						@Override
						public int getNumFaces() {
							return numFaces;
						}

						@Override
						public int getNumVerticesOfFace(int face) {
							return 3;
						}

						@Override
						public void getPosition(float[] posOut, int face, int vert) {
							int index = (face * 3) + vert;
							posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
							posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
							posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
						}

						@Override
						public void getNormal(float[] normOut, int face, int vert) {
							int index = (face * 3) + vert;
							normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
							normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
							normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
						}

						@Override
						public void getTexCoord(float[] texOut, int face, int vert) {
							int index = (face * 3) + vert;
							texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
							texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
						}

						@Override
						public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
							int index = (face * 3) + vert;
							tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
							tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
							tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
						}

						@Override
						public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
							//Do nothing
						}
						
					});
				}
				else {
					MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

						@Override
						public int getNumFaces() {
							return numFaces;
						}

						@Override
						public int getNumVerticesOfFace(int face) {
							return 3;
						}

						@Override
						public void getPosition(float[] posOut, int face, int vert) {
							int index = (face * 3) + vert;
							posOut[0] = positionsAccessorData.get(index, 0) + deltaPositionsAccessorData.get(index, 0);
							posOut[1] = positionsAccessorData.get(index, 1) + deltaPositionsAccessorData.get(index, 1);
							posOut[2] = positionsAccessorData.get(index, 2) + deltaPositionsAccessorData.get(index, 2);
						}

						@Override
						public void getNormal(float[] normOut, int face, int vert) {
							int index = (face * 3) + vert;
							normOut[0] = normalsAccessorData.get(index, 0) + deltaNormalsAccessorData.get(index, 0);
							normOut[1] = normalsAccessorData.get(index, 1) + deltaNormalsAccessorData.get(index, 1);
							normOut[2] = normalsAccessorData.get(index, 2) + deltaNormalsAccessorData.get(index, 2);
						}

						@Override
						public void getTexCoord(float[] texOut, int face, int vert) {
							int index = (face * 3) + vert;
							texOut[0] = texcoordsAccessorData.getFloat(index, 0);
							texOut[1] = texcoordsAccessorData.getFloat(index, 1);
						}

						@Override
						public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
							int index = (face * 3) + vert;
							tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
							tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
							tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
						}

						@Override
						public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
							//Do nothing
						}
						
					});
				}
				continue;
			}
			accessorModel = morphTarget.get(texcoordsAccessorName);
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData targetAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC3, ""));
				targetAccessorDatas.add(targetAccessorData);
				AccessorData deltaTexcoordsAccessorData = AccessorDatas.create(accessorModel);
				MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

					@Override
					public int getNumFaces() {
						return numFaces;
					}

					@Override
					public int getNumVerticesOfFace(int face) {
						return 3;
					}

					@Override
					public void getPosition(float[] posOut, int face, int vert) {
						int index = (face * 3) + vert;
						posOut[0] = positionsAccessorData.get(index, 0);
						posOut[1] = positionsAccessorData.get(index, 1);
						posOut[2] = positionsAccessorData.get(index, 2);
					}

					@Override
					public void getNormal(float[] normOut, int face, int vert) {
						int index = (face * 3) + vert;
						normOut[0] = normalsAccessorData.get(index, 0);
						normOut[1] = normalsAccessorData.get(index, 1);
						normOut[2] = normalsAccessorData.get(index, 2);
					}

					@Override
					public void getTexCoord(float[] texOut, int face, int vert) {
						int index = (face * 3) + vert;
						texOut[0] = texcoordsAccessorData.getFloat(index, 0) + deltaTexcoordsAccessorData.getFloat(index, 0);
						texOut[1] = texcoordsAccessorData.getFloat(index, 1) + deltaTexcoordsAccessorData.getFloat(index, 1);
					}

					@Override
					public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
						int index = (face * 3) + vert;
						tangentsAccessorData.set(index, 0, tangent[0] - tangentsAccessorData.get(index, 0));
						tangentsAccessorData.set(index, 1, tangent[1] - tangentsAccessorData.get(index, 1));
						tangentsAccessorData.set(index, 2, tangent[2] - tangentsAccessorData.get(index, 2));
					}

					@Override
					public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
						//Do nothing
					}
					
				});
				continue;
			}
			targetAccessorDatas.add(null);
		}
		return isMorphableAttribute;
	}
	
	public boolean createColorMorphTarget(List<Map<String, AccessorModel>> morphTargets, List<AccessorFloatData> targetAccessorDatas, String attributeName) {
		boolean isMorphableAttribute = false;
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get(attributeName);
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData morphAccessorData = colorsMorphTargetAccessorModelToAccessorData.get(accessorModel);
				if(morphAccessorData == null) {
					if(accessorModel.getElementType() == ElementType.VEC3) {
						int count = accessorModel.getCount();
						morphAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, ""));
						AccessorData accessorData = AccessorDatas.create(accessorModel);
						for(int i = 0; i < count; i++) {
							morphAccessorData.set(i, 0, accessorData.getFloat(i, 0));
							morphAccessorData.set(i, 1, accessorData.getFloat(i, 1));
							morphAccessorData.set(i, 2, accessorData.getFloat(i, 2));
							morphAccessorData.set(i, 3, 0.0F);
						}
					}
					else if(accessorModel.getComponentDataType() != float.class) {
						int count = accessorModel.getCount();
						morphAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, ""));
						AccessorData accessorData = AccessorDatas.create(accessorModel);
						for(int i = 0; i < count; i++) {
							morphAccessorData.set(i, 0, accessorData.getFloat(i, 0));
							morphAccessorData.set(i, 1, accessorData.getFloat(i, 1));
							morphAccessorData.set(i, 2, accessorData.getFloat(i, 2));
							morphAccessorData.set(i, 3, accessorData.getFloat(i, 3));
						}
					}
					else {
						morphAccessorData = AccessorDatas.createFloat(accessorModel);
					}
					colorsMorphTargetAccessorModelToAccessorData.put(accessorModel, morphAccessorData);
				}
				targetAccessorDatas.add(morphAccessorData);
			}
			else targetAccessorDatas.add(null);
		}
		return isMorphableAttribute;
	}
	
	public boolean createTexcoordMorphTarget(List<Map<String, AccessorModel>> morphTargets, List<AccessorFloatData> targetAccessorDatas, String attributeName) {
		boolean isMorphableAttribute = false;
		for(Map<String, AccessorModel> morphTarget : morphTargets) {
			AccessorModel accessorModel = morphTarget.get(attributeName);
			if(accessorModel != null) {
				isMorphableAttribute = true;
				AccessorFloatData morphAccessorData = texcoordsMorphTargetAccessorModelToAccessorData.get(accessorModel);
				if(morphAccessorData == null) {
					if(accessorModel.getComponentDataType() != float.class) {
						int count = accessorModel.getCount();
						morphAccessorData = AccessorDatas.createFloat(AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC2, ""));
						AccessorData accessorData = AccessorDatas.create(accessorModel);
						for(int i = 0; i < count; i++) {
							morphAccessorData.set(i, 0, accessorData.getFloat(i, 0));
							morphAccessorData.set(i, 1, accessorData.getFloat(i, 1));
						}
					}
					else {
						morphAccessorData = AccessorDatas.createFloat(accessorModel);
					}
					texcoordsMorphTargetAccessorModelToAccessorData.put(accessorModel, morphAccessorData);
				}
				targetAccessorDatas.add(morphAccessorData);
			}
			else targetAccessorDatas.add(null);
		}
		return isMorphableAttribute;
	}
	
	public void bindVec3FloatMorphed(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, List<Runnable> command, AccessorModel baseAccessorModel, List<AccessorFloatData> targetAccessorDatas) {
		AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
		AccessorFloatData baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
		AccessorFloatData morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
		ByteBuffer morphedBufferViewData = morphedAccessorModel.getBufferViewModel().getBufferViewData();
		
		int glBufferView = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferView));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphedBufferViewData, GL15.GL_STATIC_DRAW);
		
		float weights[] = new float[targetAccessorDatas.size()];
		int numComponents = 3;
		int numElements = morphedAccessorData.getNumElements();
		
		List<Runnable> morphingCommands = new ArrayList<Runnable>(numElements * numComponents);
		for(int element = 0; element < numElements; element++) {
			for(int component = 0; component < numComponents; component++) {
				int e = element;
				int c = component;
				morphingCommands.add(() -> {
					float r = baseAccessorData.get(e, c);
					for(int i = 0; i < weights.length; i++) {
						AccessorFloatData target = targetAccessorDatas.get(i);
						if(target != null) {
							r += weights[i] * target.get(e, c);
						}
					}
					morphedAccessorData.set(e, c, r);
				});
			}
		}
		
		command.add(() -> {
			if(nodeModel.getWeights() != null) System.arraycopy(nodeModel.getWeights(), 0, weights, 0, weights.length);
			else if(meshModel.getWeights() != null) System.arraycopy(meshModel.getWeights(), 0, weights, 0, weights.length);
			
			morphingCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, morphedBufferViewData);
		});
	}
	
	public AccessorModel bindColorMorphed(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, List<Runnable> command, AccessorModel baseAccessorModel, List<AccessorFloatData> targetAccessorDatas) {
		AccessorFloatData baseAccessorData;
		AccessorFloatData morphedAccessorData;
		ByteBuffer morphedBufferViewData;
		
		if(baseAccessorModel.getComponentDataType() != float.class) {
			int count = baseAccessorModel.getCount();
			AccessorData accessorData = AccessorDatas.create(baseAccessorModel);
			baseAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC4, "");
			baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
			for(int i = 0; i < count; i++) {
				baseAccessorData.set(i, 0, accessorData.getFloat(i, 0));
				baseAccessorData.set(i, 1, accessorData.getFloat(i, 1));
				baseAccessorData.set(i, 2, accessorData.getFloat(i, 2));
				baseAccessorData.set(i, 3, accessorData.getFloat(i, 3));
			}
			AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
			morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
			morphedBufferViewData = morphedAccessorModel.getBufferViewModel().getBufferViewData();
		}
		else {
			baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
			AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
			morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
			morphedBufferViewData = morphedAccessorModel.getBufferViewModel().getBufferViewData();
		}
		
		int glBufferView = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferView));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphedBufferViewData, GL15.GL_STATIC_DRAW);
		
		float weights[] = new float[targetAccessorDatas.size()];
		int numComponents = 4;
		int numElements = morphedAccessorData.getNumElements();
		
		List<Runnable> morphingCommands = new ArrayList<Runnable>(numElements * numComponents);
		for(int element = 0; element < numElements; element++) {
			for(int component = 0; component < numComponents; component++) {
				int e = element;
				int c = component;
				morphingCommands.add(() -> {
					float r = baseAccessorData.get(e, c);
					for(int i = 0; i < weights.length; i++) {
						AccessorFloatData target = targetAccessorDatas.get(i);
						if(target != null) {
							r += weights[i] * target.get(e, c);
						}
					}
					morphedAccessorData.set(e, c, r);
				});
			}
		}
		
		command.add(() -> {
			if(nodeModel.getWeights() != null) System.arraycopy(nodeModel.getWeights(), 0, weights, 0, weights.length);
			else if(meshModel.getWeights() != null) System.arraycopy(meshModel.getWeights(), 0, weights, 0, weights.length);
			
			morphingCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, morphedBufferViewData);
		});
		return baseAccessorModel;
	}
	
	public AccessorModel bindTexcoordMorphed(List<Runnable> gltfRenderData, NodeModel nodeModel, MeshModel meshModel, List<Runnable> command, AccessorModel baseAccessorModel, List<AccessorFloatData> targetAccessorDatas) {
		AccessorFloatData baseAccessorData;
		AccessorFloatData morphedAccessorData;
		ByteBuffer morphedBufferViewData;
		
		if(baseAccessorModel.getComponentDataType() != float.class) {
			int count = baseAccessorModel.getCount();
			AccessorData accessorData = AccessorDatas.create(baseAccessorModel);
			baseAccessorModel = AccessorModelCreation.createAccessorModel(GL11.GL_FLOAT, count, ElementType.VEC2, "");
			baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
			for(int i = 0; i < count; i++) {
				baseAccessorData.set(i, 0, accessorData.getFloat(i, 0));
				baseAccessorData.set(i, 1, accessorData.getFloat(i, 1));
			}
			AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
			morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
			morphedBufferViewData = morphedAccessorModel.getBufferViewModel().getBufferViewData();
		}
		else {
			baseAccessorData = AccessorDatas.createFloat(baseAccessorModel);
			AccessorModel morphedAccessorModel = AccessorModelCreation.instantiate(baseAccessorModel, "");
			morphedAccessorData = AccessorDatas.createFloat(morphedAccessorModel);
			morphedBufferViewData = morphedAccessorModel.getBufferViewModel().getBufferViewData();
		}
		
		int glBufferView = GL15.glGenBuffers();
		gltfRenderData.add(() -> GL15.glDeleteBuffers(glBufferView));
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphedBufferViewData, GL15.GL_STATIC_DRAW);
		
		float weights[] = new float[targetAccessorDatas.size()];
		int numComponents = 2;
		int numElements = morphedAccessorData.getNumElements();
		
		List<Runnable> morphingCommands = new ArrayList<Runnable>(numElements * numComponents);
		for(int element = 0; element < numElements; element++) {
			for(int component = 0; component < numComponents; component++) {
				int e = element;
				int c = component;
				morphingCommands.add(() -> {
					float r = baseAccessorData.get(e, c);
					for(int i = 0; i < weights.length; i++) {
						AccessorFloatData target = targetAccessorDatas.get(i);
						if(target != null) {
							r += weights[i] * target.get(e, c);
						}
					}
					morphedAccessorData.set(e, c, r);
				});
			}
		}
		
		command.add(() -> {
			if(nodeModel.getWeights() != null) System.arraycopy(nodeModel.getWeights(), 0, weights, 0, weights.length);
			else if(meshModel.getWeights() != null) System.arraycopy(meshModel.getWeights(), 0, weights, 0, weights.length);
			
			morphingCommands.parallelStream().forEach(Runnable::run);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferView);
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, morphedBufferViewData);
		});
		return baseAccessorModel;
	}
	
	public static float[] findGlobalTransform(NodeModel nodeModel) {
		float[] found = NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.get(nodeModel);
		if(found != null) {
			return found;
		}
		else {
			List<NodeModel> pathToNode = new ArrayList<NodeModel>();
			pathToNode.add(nodeModel);
			nodeModel = nodeModel.getParent();
			while(nodeModel != null) {
				found = NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.get(nodeModel);
				if(found != null) {
					int i = pathToNode.size() - 1;
					do {
						nodeModel = pathToNode.get(i);
						float[] transform = DefaultNodeModel.computeLocalTransform(nodeModel, null);
						MathUtils.mul4x4(found, transform, transform);
						NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.put(nodeModel, transform);
						found = transform;
					}
					while(--i >= 0);
					return found;
				}
				else {
					pathToNode.add(nodeModel);
					nodeModel = nodeModel.getParent();
				}
			}
			int i = pathToNode.size() - 1;
			nodeModel = pathToNode.get(i);
			found = DefaultNodeModel.computeLocalTransform(nodeModel, null);
			while(--i >= 0) {
				nodeModel = pathToNode.get(i);
				float[] transform = DefaultNodeModel.computeLocalTransform(nodeModel, null);
				MathUtils.mul4x4(found, transform, transform);
				NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.put(nodeModel, transform);
				found = transform;
			}
			return found;
		}
	}
	
	/**
	 * Put the given values into a direct FloatBuffer and return it.
	 * The returned buffer may always be a slice of the same instance.
	 * This method is supposed to be called only from the OpenGL thread.
	 *
	 * @param value The value
	 * @return The FloatBuffer
	 */
	public static FloatBuffer putFloatBuffer(float value[])
	{
		int total = value.length;
		if (uniformFloatBuffer == null || uniformFloatBuffer.capacity() < total)
		{
			uniformFloatBuffer = BufferUtils.createFloatBuffer(total);
		}
		uniformFloatBuffer.position(0);
		uniformFloatBuffer.limit(uniformFloatBuffer.capacity());
		uniformFloatBuffer.put(value);
		uniformFloatBuffer.flip();
		return uniformFloatBuffer;
	}

}
