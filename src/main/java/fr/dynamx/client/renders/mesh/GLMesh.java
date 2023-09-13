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
package fr.dynamx.client.renders.mesh;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.vecmath.Vector4f;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate a vertex array object (VAO), to which vertex buffer objects
 * (VBOs) are attached. The VAO is created lazily, the first time
 * {@link #enableAttributes(ShaderProgram)} is invoked.
 */
public class GLMesh implements jme3utilities.lbj.Mesh {
    // *************************************************************************
    // constants

    /**
     * number of axes in a vector
     */
    protected static final int numAxes = 3;
    /**
     * number of vertices per edge (line)
     */
    protected static final int vpe = 2;
    /**
     * number of vertices per triangle
     */
    protected static final int vpt = 3;
    // *************************************************************************
    // fields

    /**
     * true for mutable, or false if immutable
     */
    private boolean mutable = true;
    /**
     * vertex indices, or null if none
     */
    private DxIndexBuffer indices;
    /**
     * kind of geometric primitives contained in this Mesh, such as
     * GL_TRIANGLES, GL_LINE_LOOP, or GL_POINTS
     */
    private final int drawMode;
    /**
     * number of vertices (based on buffer sizes, unmodified by indexing)
     */
    private final int vertexCount;
    /**
     * OpenGL name of the VAO (for binding or deleting) or null if it hasn't
     * been generated yet TODO rename
     */
    private Integer vaoId;
    /**
     * vertex normals (3 floats per vertex) or null if none
     */
    private VertexBuffer normals;
    /**
     * vertex positions (3 floats per vertex)
     */
    private VertexBuffer positions;
    /**
     * texture coordinates (2 floats per vertex) or null if none
     */
    @Getter
    private VertexBuffer textureCoordinates;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mutable mesh with the specified mode and vertex positions,
     * but no indices, normals, or texture coordinates.
     *
     * @param drawMode draw mode, such as GL_TRIANGLES
     * @param positionsArray vertex positions (not null, not empty, length a
     * multiple of 3, unaffected)
     */
    public GLMesh(int drawMode, float... positionsArray) {
        this(drawMode, positionsArray.length / numAxes);
        Validate.require(
                positionsArray.length % numAxes == 0, "length a multiple of 3");

        FloatBuffer data = BufferUtils.createFloatBuffer(positionsArray);
        this.positions = new VertexBuffer(data, numAxes, 0);
    }

    /**
     * Instantiate a mutable mesh with the specified mode and vertex positions,
     * but no indices, normals, or texture coordinates.
     *
     * @param drawMode draw mode, such as GL_TRIANGLES
     * @param positionsBuffer vertex positions (not null, not empty, capacity a
     * multiple of 3, alias created)
     */
    protected GLMesh(int drawMode, FloatBuffer positionsBuffer) {
        this(drawMode, positionsBuffer.capacity() / numAxes);
        int capacity = positionsBuffer.capacity();
        Validate.require(capacity % numAxes == 0, "capacity a multiple of 3");

        positionsBuffer.rewind();
        positionsBuffer.limit(capacity);

        this.positions = new VertexBuffer(positionsBuffer, numAxes, 0);
    }

    /**
     * Instantiate a mutable mesh with the specified mode and vertex positions,
     * but no indices, normals, or texture coordinates.
     *
     * @param drawMode draw mode, such as GL_TRIANGLES
     * @param positionsArray vertex positions (in mesh coordinates, not null,
     * not empty)
     */
    public GLMesh(int drawMode, Vector3f... positionsArray) {
        this(drawMode, positionsArray.length);

        FloatBuffer data = BufferUtils.createFloatBuffer(positionsArray);
        this.positions = new VertexBuffer(data, numAxes, 0);
    }

    /**
     * Instantiate a mutable mesh with the specified mode and number of
     * vertices, but no indices, normals, positions, or texture coordinates.
     *
     * @param drawMode draw mode, such as GL_TRIANGLES
     * @param vertexCount number of vertices (&ge;0)
     */
    protected GLMesh(int drawMode, int vertexCount) {
        Validate.nonNegative(vertexCount, "vertex count");

        this.drawMode = drawMode;
        this.vertexCount = vertexCount;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Delete the VAO and all its VBOs.
     */
    public void cleanUp() {
        if (vaoId == null) {
            return;
        }

        DynamXRenderUtils.bindVertexArray(vaoId);
        DynamXRenderUtils.checkForOglError();

        if (indices != null) {
            indices.cleanUp();
        }
        if (positions != null) {
            positions.cleanUp();
        }
        if (normals != null) {
            normals.cleanUp();
        }
        if (textureCoordinates != null) {
            textureCoordinates.cleanUp();
        }

        GL30.glDeleteVertexArrays(vaoId);
        DynamXRenderUtils.checkForOglError();
    }

    /**
     * Count how many vertices this Mesh renders, taking indexing into account,
     * but not the draw mode.
     *
     * @return the count (&ge;0)
     */
    public int countIndexedVertices() {
        int result = (indices == null) ? vertexCount : indices.capacity();
        return result;
    }

    /**
     * Count how many line primitives this Mesh contains.
     *
     * @return the count (&ge;0)
     */
    public int countLines() {
        int numIndices = countIndexedVertices();
        int result;
        switch (drawMode) {
            case GL11.GL_LINES:
                result = numIndices / 2;
                break;

            case GL11.GL_LINE_LOOP:
                result = numIndices;
                break;

            case GL11.GL_LINE_STRIP:
                result = numIndices - 1;
                break;

            case GL11.GL_POINTS:
            case GL11.GL_TRIANGLES:
            case GL11.GL_TRIANGLE_STRIP:
            case GL11.GL_TRIANGLE_FAN:
            case GL11.GL_QUADS:
                result = 0;
                break;

            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }

        return result;
    }

    /**
     * Count how many point primitives this Mesh contains.
     *
     * @return the count (&ge;0)
     */
    public int countPoints() {
        int numIndices = countIndexedVertices();
        int result;
        switch (drawMode) {
            case GL11.GL_POINTS:
                result = numIndices;
                break;

            case GL11.GL_LINES:
            case GL11.GL_LINE_LOOP:
            case GL11.GL_LINE_STRIP:
            case GL11.GL_TRIANGLES:
            case GL11.GL_TRIANGLE_STRIP:
            case GL11.GL_TRIANGLE_FAN:
            case GL11.GL_QUADS:
                result = 0;
                break;

            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }

        return result;
    }

    /**
     * Count how many triangle primitives this Mesh contains.
     *
     * @return the count (&ge;0)
     */
    public int countTriangles() {
        int numIndices = countIndexedVertices();
        int result;
        switch (drawMode) {
            case GL11.GL_POINTS:
            case GL11.GL_LINES:
            case GL11.GL_LINE_LOOP:
            case GL11.GL_LINE_STRIP:
            case GL11.GL_QUADS:
                result = 0;
                break;

            case GL11.GL_TRIANGLES:
                result = numIndices / vpt;
                break;

            case GL11.GL_TRIANGLE_STRIP:
            case GL11.GL_TRIANGLE_FAN:
                result = numIndices - 2;
                break;

            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }

        return result;
    }

    /**
     * Count how many vertices this Mesh contains, based on VertexBuffer
     * capacities, unmodified by draw mode and indexing.
     *
     * @return the count (&ge;0)
     */
    public int countVertices() {
        return vertexCount;
    }

    /**
     * Return the draw mode, which indicates the kind of geometric primitives
     * contained in this Mesh.
     *
     * @return the mode, such as: GL_TRIANGLES, GL_LINE_LOOP, or GL_POINTS
     */
    public int drawMode() {
        return drawMode;
    }

    /**
     * Generate normals on a triangle-by-triangle basis for a non-indexed,
     * GL_TRIANGLES mesh. Any pre-existing normals are discarded.
     *
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh generateFacetNormals() {
        verifyMutable();
        if (drawMode != GL11.GL_TRIANGLES) {
            throw new IllegalStateException("drawMode = " + drawMode);
        }
        if (indices != null) {
            throw new IllegalStateException("must be non-indexed");
        }
        int numTriangles = countTriangles();
        assert vertexCount == vpt * numTriangles;

        Vector3f posA = new Vector3f();
        Vector3f posB = new Vector3f();
        Vector3f posC = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        createNormals();
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int trianglePosition = triIndex * vpt * numAxes;
            positions.get(trianglePosition, posA);
            positions.get(trianglePosition + numAxes, posB);
            positions.get(trianglePosition + 2 * numAxes, posC);

            posB.subtract(posA, normal);
            posC.subtract(posA, ac);
            normal.cross(ac, normal);
            MyVector3f.normalizeLocal(normal);

            for (int j = 0; j < vpt; ++j) {
                normals.put(normal);
            }
        }
        normals.flip();
        assert normals.limit() == normals.capacity();

        return this;
    }

    /**
     * Generate normals using the specified strategy. Any pre-existing normals
     * are discarded.
     *
     * @param option how to generate the normals (not null)
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh generateNormals(NormalsOption option) {
        switch (option) {
            case Facet:
                generateFacetNormals();
                break;
            case None:
                this.normals = null;
                break;
            case Smooth:
                generateFacetNormals();
                smoothNormals();
                break;
            case Sphere:
                generateSphereNormals();
                break;
            default:
                throw new IllegalArgumentException("option = " + option);
        }

        return this;
    }

    /**
     * Generate normals on a vertex-by-vertex basis for an outward-facing
     * sphere. Any pre-existing normals are discarded.
     *
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh generateSphereNormals() {
        verifyMutable();
        Vector3f tmpVector = new Vector3f();

        createNormals();
        for (int vertIndex = 0; vertIndex < vertexCount; ++vertIndex) {
            int vPosition = vertIndex * numAxes;
            positions.get(vPosition, tmpVector);
            MyVector3f.normalizeLocal(tmpVector);

            normals.put(tmpVector.x).put(tmpVector.y).put(tmpVector.z);
        }
        normals.flip();
        assert normals.limit() == normals.capacity();

        return this;
    }

    /**
     * Generate texture coordinates using the specified strategy and
     * coefficients. Any pre-existing texture coordinates are discarded.
     *
     * @param option how to generate the texture coordinates (not null)
     * @param uCoefficients the coefficients for generating the first (U)
     * texture coordinate (not null)
     * @param vCoefficients the coefficients for generating the 2nd (V) texture
     * coordinate (not null)
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh generateUvs(UvsOption option, Vector4f uCoefficients,
                              Vector4f vCoefficients) {
        verifyMutable();
        if (option == UvsOption.None) {
            textureCoordinates = null;
            return this;
        }
        createUvs();

        Vector3f tmpVector = new Vector3f();
        for (int vertIndex = 0; vertIndex < vertexCount; ++vertIndex) {
            int inPosition = vertIndex * numAxes;
            positions.get(inPosition, tmpVector);
            switch (option) {
                case Linear:
                    break;
                case Spherical:
                    DynamXGeometry.toSpherical(tmpVector);
                    tmpVector.y /= FastMath.PI;
                    tmpVector.z /= FastMath.PI;
                    break;
                default:
                    throw new IllegalArgumentException("option = " + option);
            }

            float u = uCoefficients.dot(
                    new Vector4f(tmpVector.x, tmpVector.y, tmpVector.z, 1f));
            float v = vCoefficients.dot(
                    new Vector4f(tmpVector.x, tmpVector.y, tmpVector.z, 1f));
            textureCoordinates.put(u).put(v);
        }
        textureCoordinates.flip();
        assert textureCoordinates.limit() == textureCoordinates.capacity();

        return this;
    }

    public void setUvs(VertexBuffer uvs){
        verifyMutable();
        this.textureCoordinates = new VertexBuffer(uvs.getBuffer(), uvs.fpv, 2);
    }

    /**
     * Access the positions VertexBuffer.
     *
     * @return the pre-existing buffer (not null)
     */
    public VertexBuffer getPositions() {
        return positions;
    }

    /**
     * Make this mesh immutable.
     *
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh makeImmutable() {
        this.mutable = false;
        positions.makeImmutable();
        if (normals != null) {
            normals.makeImmutable();
        }
        if (textureCoordinates != null) {
            textureCoordinates.makeImmutable();
        }
        if (indices != null) {
            indices.makeImmutable();
        }

        return this;
    }

    public void render() {
        renderUsing();
    }

    /**
     * Render using the specified ShaderProgram.
     *
     * @param program the program to use (not null)
     */
    public void renderUsing() {
        enableAttributes();

        DynamXRenderUtils.bindVertexArray(vaoId);
        DynamXRenderUtils.checkForOglError();

        if (indices == null) {
            int startVertex = 0;
            GL11.glDrawArrays(drawMode, startVertex, vertexCount);
            DynamXRenderUtils.checkForOglError();
        } else {
            indices.drawElements(drawMode);
            DynamXRenderUtils.checkForOglError();

        }

        disableAttributes();
        DynamXRenderUtils.checkForOglError();

        DynamXRenderUtils.bindVertexArray(0);


    }

    /**
     * Apply the specified rotation to all vertices.
     *
     * @param xAngle the X rotation angle (in radians)
     * @param yAngle the Y rotation angle (in radians)
     * @param zAngle the Z rotation angle (in radians)
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh rotate(float xAngle, float yAngle, float zAngle) {
        if (xAngle == 0f && yAngle == 0f && zAngle == 0f) {
            return this;
        }
        verifyMutable();

        Quaternion quaternion // TODO garbage
                = new Quaternion().fromAngles(xAngle, yAngle, zAngle);

        positions.rotate(quaternion);
        if (normals != null) {
            normals.rotate(quaternion);
        }

        return this;
    }

    /**
     * Apply the specified scaling to all vertices.
     *
     * @param scaleFactor the scale factor to apply
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh scale(float scaleFactor) {
        if (scaleFactor == 1f) {
            return this;
        }
        verifyMutable();

        int numFloats = vertexCount * numAxes;
        for (int floatIndex = 0; floatIndex < numFloats; ++floatIndex) {
            float floatValue = positions.get(floatIndex);
            floatValue *= scaleFactor;
            positions.put(floatIndex, floatValue);
        }

        return this;
    }

    /**
     * Apply the specified transform to all vertices.
     *
     * @param transform the transform to apply (not null, unaffected)
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh transform(Transform transform) {
        Validate.nonNull(transform, "transform");
        if (MyMath.isIdentity(transform)) {
            return this;
        }
        verifyMutable();

        positions.transform(transform);

        if (normals != null) {
            Transform normalsTransform = transform.clone();
            normalsTransform.getTranslation().zero();
            normalsTransform.setScale(1f);

            normals.transform(normalsTransform);
        }

        return this;
    }

    /**
     * Transform all texture coordinates using the specified coefficients. Note
     * that the Z components of the coefficients are currently unused.
     *
     * @param uCoefficients the coefficients for calculating new Us (not null,
     * unaffected)
     * @param vCoefficients the coefficients for calculating new Vs (not null,
     * unaffected)
     * @return the (modified) current instance (for chaining)
     */
    public GLMesh transformUvs(Vector4f uCoefficients, Vector4f vCoefficients) {
        verifyMutable();
        if (textureCoordinates == null) {
            throw new IllegalStateException("There are no UVs in the mesh.");
        }

        for (int vIndex = 0; vIndex < vertexCount; ++vIndex) {
            int startPosition = 2 * vIndex;
            float oldU = textureCoordinates.get(startPosition);
            float oldV = textureCoordinates.get(startPosition + 1);

            float newU = uCoefficients.getW()
                    + uCoefficients.getX() * oldU
                    + uCoefficients.getY() * oldV;
            float newV = vCoefficients.getW()
                    + vCoefficients.getX() * oldU
                    + vCoefficients.getY() * oldV;

            textureCoordinates.put(startPosition, newU);
            textureCoordinates.put(startPosition + 1, newV);
        }

        return this;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Create a buffer for putting vertex indices.
     *
     * @param capacity the desired capacity (in indices, &ge;0)
     * @return a new IndexBuffer with the specified capacity
     */
    protected DxIndexBuffer createIndices(int capacity) {
        verifyMutable();
        this.indices = new DxIndexBuffer(vertexCount, capacity);
        return indices;
    }

    /**
     * Create a buffer for putting vertex normals.
     *
     * @return a new buffer with a capacity of 3 * vertexCount floats
     */
    protected VertexBuffer createNormals() {
        verifyMutable();
        if (countTriangles() == 0) {
            throw new IllegalStateException(
                    "The mesh doesn't contain any triangles.");
        }

        this.normals = new VertexBuffer(vertexCount, numAxes,
                1);

        return normals;
    }

    /**
     * Create a buffer for putting vertex positions.
     *
     * @return a new buffer with a capacity of 3 * vertexCount floats
     */
    protected VertexBuffer createPositions() {
        verifyMutable();
        this.positions = new VertexBuffer(vertexCount, numAxes,
                0);
        return positions;
    }

    /**
     * Create a buffer for putting vertex texture coordinates.
     *
     * @return a new buffer with a capacity of 2 * vertexCount floats
     */
    public VertexBuffer createUvs() {
        verifyMutable();
        this.textureCoordinates
                = new VertexBuffer(vertexCount, 2, 2);
        return textureCoordinates;
    }

    /**
     * Set new normals for the vertices.
     *
     * @param normalsArray the desired vertex normals (not null,
     * length=3*vertexCount, unaffected)
     */
    protected void setNormals(float... normalsArray) {
        verifyMutable();
        Validate.require(normalsArray.length == vertexCount * numAxes,
                "correct length");

        this.normals = new VertexBuffer(normalsArray, numAxes,
                1);
    }

    /**
     * Set new positions for the vertices.
     *
     * @param positionArray the desired vertex positions (not null,
     * length=3*vertexCount, unaffected)
     */
    protected void setPositions(float... positionArray) {
        verifyMutable();
        Validate.require(positionArray.length == vertexCount * numAxes,
                "correct length");

        this.positions = new VertexBuffer(positionArray, numAxes,
                0);
    }

    /**
     * Set new texture coordinates for the vertices.
     *
     * @param uvArray the desired vertex texture coordinates (not null,
     * length=2*vertexCount, unaffected)
     */
    protected void setUvs(float... uvArray) {
        verifyMutable();
        Validate.require(uvArray.length == 2 * vertexCount,
                "correct length");

        this.textureCoordinates
                = new VertexBuffer(uvArray, 2, 2);
    }
    // *************************************************************************
    // jme3utilities.lbj.Mesh methods

    /**
     * Access the index buffer.
     *
     * @return the pre-existing instance (not null)
     */
    @Override
    public DxIndexBuffer getIndexBuffer() {
        assert indices != null;
        return indices;
    }

    /**
     * Access the normals data buffer.
     *
     * @return the pre-existing buffer (not null)
     */
    @Override
    public FloatBuffer getNormalsData() {
        return normals.getBuffer();
    }

    /**
     * Access the positions data buffer.
     *
     * @return the pre-existing buffer (not null)
     */
    @Override
    public FloatBuffer getPositionsData() {
        return positions.getBuffer();
    }

    /**
     * Test whether the draw mode is GL_LINES. Indexing is allowed.
     *
     * @return true if pure lines, otherwise false
     */
    @Override
    public boolean isPureLines() {
        boolean result = (drawMode == GL11.GL_LINES);
        return result;
    }

    /**
     * Test whether the draw mode is GL_TRIANGLES.
     *
     * @return true if pure triangles, otherwise false
     */
    @Override
    public boolean isPureTriangles() {
        boolean result = (drawMode == GL11.GL_TRIANGLES);
        return result;
    }

    /**
     * Indicate that the normals data has changed.
     */
    @Override
    public void setNormalsModified() {
        normals.setModified();
    }

    /**
     * Indicate that the positions data has changed.
     */
    @Override
    public void setPositionsModified() {
        positions.setModified();
    }
    // *************************************************************************
    // private methods

    /**
     * Prepare all vertex attributes for rendering.
     * <p>
     * If the VAO doesn't already exist, it is created.
     *
     */
    private void enableAttributes() {
        if (vaoId == null) {
            this.vaoId = DynamXRenderUtils.genVertexArrays();
            DynamXRenderUtils.checkForOglError();

            DynamXRenderUtils.bindVertexArray(vaoId);
            DynamXRenderUtils.checkForOglError();

            this.mutable = false;

        } else {
            assert !mutable;

            // Use the existing VAO.
            DynamXRenderUtils.bindVertexArray(vaoId);
            DynamXRenderUtils.checkForOglError();
        }

        positions.prepareToDraw();
        if (normals != null) {
            normals.prepareToDraw();
        }
        if (textureCoordinates != null) {
            textureCoordinates.prepareToDraw();
        }
    }

    private void disableAttributes(){
        positions.stopDraw();
        if (normals != null) {
            normals.stopDraw();
        }
        if (textureCoordinates != null) {
            textureCoordinates.stopDraw();
        }
    }

    /**
     * Smooth the pre-existing normals by averaging them across all uses of each
     * distinct vertex position.
     */
    private void smoothNormals() {
        verifyMutable();
        assert indices == null;
        assert normals != null;

        Map<Vector3f, Integer> mapPosToDpid = new HashMap<>(vertexCount);
        int numDistinctPositions = 0;
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            Vector3f position = new Vector3f();
            positions.get(start, position);
            MyVector3f.standardize(position, position);
            if (!mapPosToDpid.containsKey(position)) {
                mapPosToDpid.put(position, numDistinctPositions);
                ++numDistinctPositions;
            }
        }
        /*
         * Initialize the normal sum for each distinct position.
         */
        Vector3f[] normalSums = new Vector3f[numDistinctPositions];
        for (int dpid = 0; dpid < numDistinctPositions; ++dpid) {
            normalSums[dpid] = new Vector3f();
        }

        Vector3f tmpPosition = new Vector3f();
        Vector3f tmpNormal = new Vector3f();
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            positions.get(start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);

            normals.get(start, tmpNormal);
            normalSums[dpid].addLocal(tmpNormal);
        }
        /*
         * Re-normalize the normal sum for each distinct position.
         */
        for (Vector3f normal : normalSums) {
            MyVector3f.normalizeLocal(normal);
        }
        /*
         * Write new normals to the buffer.
         */
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            positions.get(start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);
            normals.put(start, normalSums[dpid]);
        }
    }

    /**
     * Verify that this Mesh is still mutable.
     */
    private void verifyMutable() {
        if (!mutable) {
            throw new IllegalStateException("The mesh is no longer mutable.");
        }
    }
}
