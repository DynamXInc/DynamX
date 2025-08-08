package com.modularmods.mcgltf.dynamx;

import java.util.List;

import de.javagl.jgltf.dynamx.model.GltfModel;
import fr.dynamx.api.dxmodel.DxModelPath;

public interface IGltfModelReceiver {

	DxModelPath getModelLocation();
	
	default void onReceiveSharedModel(RenderedGltfModel renderedModel) {}
	
	default boolean isReceiveSharedModel(GltfModel gltfModel, List<Runnable> gltfRenderDatas) {
		return true;
	}
}
