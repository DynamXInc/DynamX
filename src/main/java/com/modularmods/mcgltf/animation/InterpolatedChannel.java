package com.modularmods.mcgltf.animation;

import de.javagl.jgltf.model.NodeModel;
import fr.dynamx.client.renders.animations.DxAnimation;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;

import java.util.Arrays;

public abstract class InterpolatedChannel {

	/**
	 * The key frame times, in seconds
	 */
	protected final float[] timesS;

	public NodeModel nodeModel;

	public DxAnimation.Timer timer = new DxAnimation.Timer();
	
	public InterpolatedChannel(float[] timesS, NodeModel nodeModel) {
		this.timesS = timesS;
		this.nodeModel = nodeModel;
	}
	
	public float[] getKeys() {
		return timesS;
	}
	
	public abstract TransformType update(float timeS);
	
	protected abstract TransformType getListener();
	
	/**
	 * Compute the index of the segment that the given key belongs to.
	 * If the given key is smaller than the smallest or larger than
	 * the largest key, then 0 or <code>keys.length-1<code> will be returned, 
	 * respectively.
	 * 
	 * @param key The key
	 * @param keys The sorted keys
	 * @return The index for the key
	 */
	public static int computeIndex(float key, float keys[])
	{
		int index = Arrays.binarySearch(keys, key);
		if (index >= 0)
		{
		    return index;
		}
		return Math.max(0, -index - 2);
	}

	public static class TransformType{

		public float[] copiedValues;
		public float[] initialValues;
		public GltfModelRenderer.EnumTransformType type;

		public TransformType(float[] copiedValues, float[] initialValues, GltfModelRenderer.EnumTransformType type) {
			this.copiedValues = copiedValues;
			this.initialValues = initialValues != null ? initialValues.clone() : null;
			this.type = type;
		}
	}

}
