package com.modularmods.mcgltf.animation;

import java.util.Arrays;

public abstract class InterpolatedChannel {

	/**
	 * The key frame times, in seconds
	 */
	protected final float[] timesS;
	
	public InterpolatedChannel(float[] timesS) {
		this.timesS = timesS;
	}
	
	public float[] getKeys() {
		return timesS;
	}
	
	public abstract void update(float timeS);
	
	protected abstract float[] getListener();
	
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

}
