package com.modularmods.mcgltf.animation;

import de.javagl.jgltf.model.NodeModel;

public abstract class CubicSplineInterpolatedChannel extends InterpolatedChannel {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][][] values;
	
	public CubicSplineInterpolatedChannel(float[] timesS, float[][][] values, NodeModel nodeModel) {
		super(timesS, nodeModel);
		this.values = values;
	}
	
	@Override
	public TransformType update(float timeS) {
		float[] output = getListener().copiedValues;
		System.arraycopy(values[0][1], 0, output, 0, output.length);

		return getListener();
	}

}
