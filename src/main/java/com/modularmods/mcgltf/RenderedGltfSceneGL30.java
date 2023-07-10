package com.modularmods.mcgltf;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

public class RenderedGltfSceneGL30 extends RenderedGltfScene {

	@Override
	public void renderForVanilla() {
		vanillaRenderCommands.forEach(Runnable::run);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

	@Override
	public void renderForShaderMod() {
		shaderModRenderCommands.forEach(Runnable::run);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

}
