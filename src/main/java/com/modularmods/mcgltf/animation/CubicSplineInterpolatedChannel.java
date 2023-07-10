package com.modularmods.mcgltf.animation;

public abstract class CubicSplineInterpolatedChannel extends InterpolatedChannel {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][][] values;
	
	public CubicSplineInterpolatedChannel(float[] timesS, float[][][] values) {
		super(timesS);
		this.values = values;
	}
	
	@Override
	public void update(float timeS) {
		float[] output = getListener();
		if(timeS <= timesS[0]) {
			System.arraycopy(values[0][1], 0, output, 0, output.length);
		}
		else if(timeS >= timesS[timesS.length - 1]) {
			System.arraycopy(values[timesS.length - 1][1], 0, output, 0, output.length);
		}
		else {
			// Adapted from https://github.khronos.org/glTF-Tutorials/gltfTutorial/gltfTutorial_007_Animations.html#cubic-spline-interpolation
			int previousIndex = computeIndex(timeS, timesS);
			int nextIndex = previousIndex + 1;
			
			float local = timeS - timesS[previousIndex];
			float delta = timesS[nextIndex] - timesS[previousIndex];
			float alpha = local / delta;
			float alpha2 =  alpha * alpha;
			float alpha3 =  alpha2 * alpha;
			
			float aa = 2 * alpha3 - 3 * alpha2 + 1;
			float ab = alpha3 - 2 * alpha2 + alpha;
			float ac = -2 * alpha3 + 3 * alpha2;
			float ad = alpha3 - alpha2;
			
			float[][] previous = values[previousIndex];
			float[][] next = values[nextIndex];
			
			float[] previousPoint = previous[1];
			float[] nextPoint = next[1];
			float[] previousOutputTangent = previous[2];
			float[] nextInputTangent = next[0];
			
			for(int i = 0; i < output.length; i++) {
				float p = previousPoint[i];
				float pt = previousOutputTangent[i] * delta;
				float n = nextPoint[i];
				float nt = nextInputTangent[i] * delta;
				output[i] = aa * p + ab * pt + ac * n + ad * nt;
			}
		}
	}

}
