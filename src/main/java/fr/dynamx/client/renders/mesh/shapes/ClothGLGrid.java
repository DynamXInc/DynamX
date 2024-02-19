/*
 Copyright (c) 2019-2022, Stephen Gold and Yanis Boudiaf
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


import fr.dynamx.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;
import jme3utilities.Validate;
import org.lwjgl.opengl.GL11;

/**
 * A GL_TRIANGLES mesh (with indices and normals but no texture coordinates)
 * that renders a subdivided rectangle.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ClothGLGrid extends GLMesh {
    // *************************************************************************
    // fields

    /**
     * number of grid lines parallel to the X axis
     */
    final private int numXLines;
    /**
     * number of grid lines parallel to the Z axis
     */
    final private int numZLines;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a grid in the X-Z plane, centered on (0,0,0).
     *
     * @param xLines the desired number of grid lines parallel to the X axis
     * (&ge;2)
     * @param zLines the desired number of grid lines parallel to the Z axis
     * (&ge;2)
     * @param lineSpacing the desired initial distance between adjacent grid
     * lines (in mesh units, &gt;0)
     */
    public ClothGLGrid(int xLines, int zLines, float lineSpacing) {
        super(GL11.GL_TRIANGLES, xLines * zLines);
        Validate.inRange(xLines, "X lines", 2, Integer.MAX_VALUE);
        Validate.inRange(zLines, "Z lines", 2, Integer.MAX_VALUE);
        Validate.positive(lineSpacing, "line spacing");

        this.numXLines = xLines;
        this.numZLines = zLines;

        int numVertices = super.countVertices();
        VertexBuffer posBuffer = super.createPositions();

        // Write the vertex locations:
        for (int xIndex = 0; xIndex < zLines; ++xIndex) {
            float x = (2 * xIndex - zLines + 1) * lineSpacing / 2f;
            for (int zIndex = 0; zIndex < xLines; ++zIndex) {
                float z = (2 * zIndex - xLines + 1) * lineSpacing / 2f;
                posBuffer.put(x).put(0f).put(z);
            }
        }
        assert posBuffer.position() == numAxes * numVertices;
        posBuffer.flip();

        VertexBuffer normBuffer = createNormals();

        // Write the normals:
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            normBuffer.put(0f).put(1f).put(0f);
        }
        assert normBuffer.position() == numAxes * numVertices;
        normBuffer.flip();

        int numTriangles = 2 * (xLines - 1) * (zLines - 1);
        int numIndices = vpt * numTriangles;
        DxIndexBuffer indexBuffer = createIndices(numIndices);

        // Write vertex indices for triangles:
        for (int zIndex = 0; zIndex < xLines - 1; ++zIndex) {
            for (int xIndex = 0; xIndex < zLines - 1; ++xIndex) {
                // 4 vertices and 2 triangles forming a square
                int vi0 = zIndex + xLines * xIndex;
                int vi1 = vi0 + 1;
                int vi2 = vi0 + xLines;
                int vi3 = vi1 + xLines;
                if ((xIndex + zIndex) % 2 == 0) {
                    // major diagonal: joins vi1 to vi2
                    indexBuffer.put(vi0);
                    indexBuffer.put(vi1);
                    indexBuffer.put(vi2);

                    indexBuffer.put(vi3);
                    indexBuffer.put(vi2);
                    indexBuffer.put(vi1);
                } else {
                    // minor diagonal: joins vi0 to vi3
                    indexBuffer.put(vi0);
                    indexBuffer.put(vi1);
                    indexBuffer.put(vi3);

                    indexBuffer.put(vi3);
                    indexBuffer.put(vi2);
                    indexBuffer.put(vi0);
                }
            }
        }
        indexBuffer.flip();
        assert indexBuffer.size() == numIndices;
    }
}
