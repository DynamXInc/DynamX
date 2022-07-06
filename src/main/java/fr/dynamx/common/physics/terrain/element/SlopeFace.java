package fr.dynamx.common.physics.terrain.element;

import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.Vector3fPool;

import java.io.Serializable;

public class SlopeFace implements Serializable
{
	private Vector3f[] points;

	public void setPoints(Vector3f[] points) {
		this.points = points;
	}

	public SlopeFace() {}

	public SlopeFace buildShape(Vector3f offset, byte X, byte Y, byte type, byte u,byte v, boolean full, long poolId) // type = 1 ou 2
	{
		points = new Vector3f[8];
		if (type == 1) {
		
			if(X==0&&Y==-1) { // SOUTH X=0 Y=-1
				points[0] = Vector3fPool.get(poolId, offset.toString(), 0, 0, 1-u);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 1, 0, 1-u);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 1, full?1:.5f, 1);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 1);
			}
			if(X==1&&Y==0) { // WEST X=1 Y=0
				points[0] = Vector3fPool.get(poolId, offset.toString(), u, 0, 0);
				points[1] = Vector3fPool.get(poolId, offset.toString(), u, 0, 1);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 1);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 0);
			}
			if(X==0&&Y==1) { // NORTH X=0 Y=1
				points[0] = Vector3fPool.get(poolId, offset.toString(), 0, 0, u);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 1, 0, u);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 1, full?1:.5f, 0);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 0);
			}
			if(X==-1&&Y==0) {// EAST X=-1 Y=0
				points[0] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0,0 );
				points[1] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0,1 );
				points[2] = Vector3fPool.get(poolId, offset.toString(), 1, full?1:.5f, 1);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 1, full?1:.5f, 0);
			}

		} else if (type == 2) {
			if(X==-1&&Y==-1) { // SOUTH-EAST X=-1 Y=-1
				points[0] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0, 1-v);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 1, 0, 1-v);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 1,full?1:.5f, 1);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0, 1);
			}
			if(X==1&&Y==-1) { // SOUTH-WEST X=1 Y=-1
				points[0] = Vector3fPool.get(poolId, offset.toString(), u, 0, 1-v);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 0, 0, 1-v);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 1);
				points[3] = Vector3fPool.get(poolId, offset.toString(), u, 0, 1);
			}
			if(X==1&&Y==1) { // NORTH-WEST X=1 Y=1
				points[0] = Vector3fPool.get(poolId, offset.toString(), u, 0, 0);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 0, full?1:.5f, 0);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 0, 0, v);
				points[3] = Vector3fPool.get(poolId, offset.toString(), u, 0, v);
			}
			if(X==-1&&Y==1) {// NORTH-EAST X=-1 Y=1
				points[0] = Vector3fPool.get(poolId, offset.toString(), 1, full?1:.5f, 0);
				points[1] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0, 0);
				points[2] = Vector3fPool.get(poolId, offset.toString(), 1-u, 0, v);
				points[3] = Vector3fPool.get(poolId, offset.toString(), 1, 0, v);
			}
		}

		/*
		 * points = new Vector3f[4]; points[0] = Vector3fPool.get(poolId, 0, 0, 1);
		 * points[1] = Vector3fPool.get(poolId, 1, 0, 1); points[2] =
		 * Vector3fPool.get(poolId, 1, full?1:.5f, 2); points[3] = Vector3fPool.get(poolId, 0,
		 * full?1:.5f, 2); faces.add(new SlopeCollisionFace(points));
		 */

		/*
		 * points = new Vector3f[4]; points[0] = Vector3fPool.get(poolId, 0, full?1:.5f, 1);
		 * points[1] = Vector3fPool.get(poolId, 1, full?1:.5f, 1); points[2] =
		 * Vector3fPool.get(poolId, 1, full?1:.5f, 2); points[3] = Vector3fPool.get(poolId, 0,
		 * .5f, 2); faces.add(new SlopeCollisionFace(points));
		 */

		// FACE PLATE
		points[4] = Vector3fPool.get(poolId, offset.toString(), 0, 0, 0);
		points[5] = Vector3fPool.get(poolId, offset.toString(), 1, 0, 0);
		points[6] = Vector3fPool.get(poolId, offset.toString(), 1, 0, 1);
		points[7] = Vector3fPool.get(poolId, offset.toString(), 0, 0, 1);

		for(Vector3f p : points)
		{
			p.addLocal(offset);
		}
		//System.out.println("Slope offset "+offset);
		return this;
	}

	public Vector3f[] getPoints() {
		return points;
	}
}
