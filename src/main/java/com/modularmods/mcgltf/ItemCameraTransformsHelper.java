package com.modularmods.mcgltf;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

public final class ItemCameraTransformsHelper {

	private static ItemCameraTransforms.TransformType transformType;
	
	private static final IBakedModel dummyBakedModel = new IBakedModel() {

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			return Collections.emptyList();
		}

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return true;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return ModelLoader.White.INSTANCE;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
			transformType = cameraTransformType;
			return IBakedModel.super.handlePerspective(cameraTransformType);
		}
		
	};
	
	private static final IModel dummyModel = new IModel() {

		@Override
		public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
			return dummyBakedModel;
		}
		
	};
	
	private static final ICustomModelLoader dummyModelLoader = new ICustomModelLoader() {

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {}

		@Override
		public boolean accepts(ResourceLocation modelLocation) {
			return modelLocation.getNamespace().equals("mcgltf") && modelLocation.getPath().equals("dummy_model");
		}

		@Override
		public IModel loadModel(ResourceLocation modelLocation) throws Exception {
			return dummyModel;
		}
		
	};
	
	private static final ModelResourceLocation dummyModelLocation = new ModelResourceLocation("mcgltf:dummy_model#inventory");
	
	static {
		ModelLoaderRegistry.registerLoader(dummyModelLoader);
	}
	
	public static void registerDummyModelToAccessCurrentTransformTypeForTEISR(Item item) {
		ModelLoader.setCustomModelResourceLocation(item, 0, dummyModelLocation);
	}
	
	public static ItemCameraTransforms.TransformType getCurrentTransformType() {
		return transformType;
	}

}
