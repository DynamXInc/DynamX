package com.modularmods.mcgltf.animation;

import de.javagl.jgltf.model.NodeModel;

public abstract class LinearInterpolatedChannel extends InterpolatedChannel {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][] values;
	
	public LinearInterpolatedChannel(float[] timesS, float[][] values, NodeModel nodeModel) {
		super(timesS, nodeModel);
		this.values = values;
	}
	
	@Override
	public TransformType update(float timeS) {
		float[] output = getListener().copiedValues;
		if(timeS <= timesS[0]) {
			System.arraycopy(values[0], 0, output, 0, output.length);
		}
		else if(timeS >= timesS[timesS.length - 1]) {
			System.arraycopy(values[timesS.length - 1], 0, output, 0, output.length);
		}
		else {
			int previousIndex = computeIndex(timeS, timesS);
			int nextIndex = previousIndex + 1;

			if(nextIndex >= timesS.length) {
				nextIndex = timesS.length - 1;
			}

			float local = timeS - timesS[previousIndex];
			float delta = timesS[nextIndex] - timesS[previousIndex];
			float alpha = local / delta;

			float[] previousPoint = values[previousIndex];
			float[] nextPoint = values[nextIndex];

			for(int i = 0; i < output.length; i++) {
				float p = previousPoint[i];
				float n = nextPoint[i];
				output[i] = p + alpha * (n - p);
			}
		}
		return getListener();
	}

}
