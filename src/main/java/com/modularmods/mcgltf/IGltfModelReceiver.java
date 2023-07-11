package com.modularmods.mcgltf;

import java.util.List;

import de.javagl.jgltf.model.GltfModel;
import fr.dynamx.api.dxmodel.DxModelPath;
import net.minecraft.util.ResourceLocation;

public interface IGltfModelReceiver {

	DxModelPath getModelLocation();
	
	default void onReceiveSharedModel(RenderedGltfModel renderedModel) {}
	
	default boolean isReceiveSharedModel(GltfModel gltfModel, List<Runnable> gltfRenderDatas) {
		return true;
	}
}
