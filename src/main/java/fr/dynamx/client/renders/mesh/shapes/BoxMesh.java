/*
 Copyright (c) 2022, Stephen Gold and Yanis Boudiaf
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fr.dynamx.client.renders.mesh.shapes;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;
import org.lwjgl.opengl.GL11;

/**
 * A GL_TRIANGLES mesh that renders an axis-aligned box.
 * <p>
 * The box extends from (x1,y1,z1) to (x2,y2,z2). All triangles face outward
 * with right-handed winding.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoxMesh extends GLMesh {
    // *************************************************************************
    // constants

    /**
     * vertex indices of the 6 square faces in a cube (12 outward-facing
     * triangles with right-handed winding)
     */
    final private static int[] cubeIndices = {
        0, 1, 2, 1, 3, 2, // -X face
        4, 7, 5, 4, 6, 7, // +X face
        0, 4, 1, 1, 4, 5, // -Y face
        2, 3, 7, 2, 7, 6, // +Y face
        0, 6, 4, 0, 2, 6, // -Z face
        1, 5, 3, 3, 5, 7 //  +Z face
    };
    /**
     * vertex locations in an axis-aligned unit cube centered at (0.5,0.5,0.5)
     */
    final private static Vector3f[] cubeLocations = {
        new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 1f),
        new Vector3f(0f, 1f, 0f), new Vector3f(0f, 1f, 1f),
        new Vector3f(1f, 0f, 0f), new Vector3f(1f, 0f, 1f),
        new Vector3f(1f, 1f, 0f), new Vector3f(1f, 1f, 1f)
    };
    // *************************************************************************
    // fields

    /**
     * shared mesh for a box extending from (-1,-1,-1) to (+1,+1,+1) with
     * normals
     */
    private static BoxMesh bm111;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an axis-aligned box.
     *
     * @param x1 X coordinate of the first vertex
     * @param y1 Y coordinate of the first vertex
     * @param z1 Z coordinate of the first vertex
     * @param x2 X coordinate of the vertex diagonally opposite the first vertex
     * @param y2 Y coordinate of the vertex diagonally opposite the first vertex
     * @param z2 Z coordinate of the vertex diagonally opposite the first vertex
     */
    public BoxMesh(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
        super(GL11.GL_TRIANGLES, 36);

        VertexBuffer posBuffer = super.createPositions();
        for (int vertexIndex : cubeIndices) {
            Vector3f loc = cubeLocations[vertexIndex]; // alias
            float x = x1 + loc.x * (x2 - x1);
            float y = y1 + loc.y * (y2 - y1);
            float z = z1 + loc.z * (z2 - z1);
            posBuffer.put(x).put(y).put(z);
        }
        posBuffer.flip();
        assert posBuffer.limit() == posBuffer.capacity();
    }
    /**
     * Instantiate an axis-aligned box.
     *
     * @param halfExtent The extent used to generate the axis-aligned box.
     */
    public BoxMesh(float halfExtent) {
        this(new Vector3f(halfExtent, halfExtent, halfExtent));
    }
    /**
     * Instantiate an axis-aligned box.
     *
     * @param halfExtent The extent used to generate the axis-aligned box.
     */
    public BoxMesh(Vector3f halfExtent) {
        this(halfExtent.x, halfExtent.y, halfExtent.z, -halfExtent.x, -halfExtent.y, -halfExtent.z);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Return the shared mesh for a box extending from (-1,-1,-1) to (+1,+1,+1)
     * with normals.
     *
     * @return the shared mesh (immutable)
     */
    public static BoxMesh getMesh() {
        if (bm111 == null) {
            bm111 = new BoxMesh(-1f, -1f, -1f, 1f, 1f, 1f);
            bm111.generateFacetNormals();
            bm111.makeImmutable();
        }

        return bm111;
    }
}
