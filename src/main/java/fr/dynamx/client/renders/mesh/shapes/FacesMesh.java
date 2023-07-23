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

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Transform;
import com.jme3.util.BufferUtils;
import fr.dynamx.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;

/**
 * An auto-generated GL_TRIANGLES mesh to visualize the faces in a soft body.
 */
public class FacesMesh extends GLMesh {
    // *************************************************************************
    // fields

    /**
     * copy vertex indices for the faces
     */
    final private IntBuffer copyIndices;
    /**
     * body being visualized
     */
    final private PhysicsSoftBody softBody;
    // *************************************************************************
    // constructors

    /**
     * Auto-generate a mutable mesh for the specified soft body.
     *
     * @param softBody the soft body from which to generate the mesh (not null,
     * alias created)
     */
    public FacesMesh(PhysicsSoftBody softBody) {
        super(GL11.GL_TRIANGLES, softBody.countNodes());

        this.softBody = softBody;

        // Create the VertexBuffer for node locations.
        VertexBuffer positions = super.createPositions();
        positions.setDynamic();

        // Create the VertexBuffer for node normals.
        VertexBuffer normals = super.createNormals();
        normals.setDynamic();

        // Create the IndexBuffer for triangle indices.
        int numFaces = softBody.countFaces();
        int numIndices = vpt * numFaces;
        DxIndexBuffer indices = super.createIndices(numIndices);
        indices.setDynamic();

        // Create a buffer for copying indices.
        this.copyIndices = BufferUtils.createIntBuffer(numIndices);

        update();
    }

    /**
     * Update this Mesh to match the soft body.
     *
     * @return true if successful, otherwise false
     */
    public boolean update() {
        int numNodes = softBody.countNodes();
        if (numNodes != countVertices()) {
            return false;
        }
        int numFaces = softBody.countFaces();
        if (numFaces != countTriangles()) {
            return false;
        }

        // Update the vertex positions from node locations in physics space.
        IntBuffer indexMap = null;
        boolean localFlag = false;
        boolean normalsFlag = true;
        Transform transform = null; // physics locations = mesh positions
        NativeSoftBodyUtil.updateMesh(
                softBody, indexMap, this, localFlag, normalsFlag, transform);

        // Update the index buffer from faces. TODO avoid copying indices
        softBody.copyFaces(copyIndices);
        DxIndexBuffer indices = super.getIndexBuffer();
        for (int i = 0; i < vpt * numFaces; ++i) {
            int index = copyIndices.get(i);
            indices.put(i, index);
        }

        return true;
    }
}
