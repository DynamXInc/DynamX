package com.modularmods.mcgltf;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

public class RenderedGltfScene {

	public final List<Runnable> skinningCommands = new ArrayList<Runnable>();
	
	public final List<Runnable> vanillaRenderCommands = new ArrayList<Runnable>();
	
	public final List<Runnable> shaderModRenderCommands = new ArrayList<Runnable>();
	
	public void renderForVanilla() {
		if(!skinningCommands.isEmpty()) {
			GL20.glUseProgram(MCglTF.getInstance().getGlProgramSkinnig());
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
			skinningCommands.forEach(Runnable::run);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
			GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
			GL20.glUseProgram(0);
		}
		
		vanillaRenderCommands.forEach(Runnable::run);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}
	
	public void renderForShaderMod() {
		if(!skinningCommands.isEmpty()) {
			int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
			GL20.glUseProgram(MCglTF.getInstance().getGlProgramSkinnig());
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
			skinningCommands.forEach(Runnable::run);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
			GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
			GL20.glUseProgram(currentProgram);
		}
		
		shaderModRenderCommands.forEach(Runnable::run);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

}
