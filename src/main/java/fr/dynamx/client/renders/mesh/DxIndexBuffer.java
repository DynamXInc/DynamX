/*
 Copyright (c) 2022, Stephen Gold and Yanis Boudiaf
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

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

import fr.dynamx.utils.client.DynamXRenderUtils;
import jme3utilities.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Wrapper class for the index buffer of a SPORT mesh, including its VBO and
 * data.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DxIndexBuffer extends jme3utilities.lbj.IndexBuffer {
    // *************************************************************************
    // constants

    /**
     * target for glBindBuffer() and glBufferData() (=vertex array indices)
     */
    final private static int target = GL15.GL_ELEMENT_ARRAY_BUFFER;
    // *************************************************************************
    // fields

    /**
     * true if the data store needs updating, otherwise false
     */
    private boolean isModified = true;
    /**
     * OpenGL data type of the elements (GL_UNSIGNED_BYTE, GL_UNSIGNED_SHORT, or
     * GL_UNSIGNED_INT)
     */
    final private int elementType;
    /**
     * expected usage pattern
     */
    private int usageHint = GL15.GL_STATIC_DRAW;
    /**
     * OpenGL name of the VBO, or null if the VBO hasn't been generated yet
     */
    private Integer vbo;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an IndexBuffer with a new data buffer.
     *
     * @param maxVertices one more than the highest index value (&ge;0)
     * @param capacity number of indices (&ge;0)
     */
    public DxIndexBuffer(int maxVertices, int capacity) {
        super(maxVertices, capacity);
        Validate.nonNegative(maxVertices, "max vertices");
        Validate.nonNegative(capacity, "capacity");

        Buffer buffer = super.getBuffer();
        if (buffer instanceof ByteBuffer) {
            this.elementType = GL11.GL_UNSIGNED_BYTE;
        } else if (buffer instanceof ShortBuffer) {
            this.elementType = GL11.GL_UNSIGNED_SHORT;
        } else {
            assert buffer instanceof IntBuffer;
            this.elementType = GL11.GL_UNSIGNED_INT;
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Return the buffer's capacity.
     *
     * @return the element count (&ge;0)
     */
    public int capacity() {
        int result = getBuffer().capacity();
        return result;
    }

    /**
     * Delete the VBO during cleanup.
     */
    void cleanUp() {
        if (vbo != null) {
            GL15.glDeleteBuffers(vbo);
            DynamXRenderUtils.checkForOglError();
        }
    }

    /**
     * Clear the buffer. The read/write position is zeroed, and the limit is set
     * to the capacity. The data in the buffer is unaffected.
     *
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer clear() {
        getBuffer().clear();
        return this;
    }

    /**
     * Draw a sequence of geometric primitives using this IndexBuffer. This
     * includes generating the VBO (if that hasn't occurred yet) and updating
     * its data store (if needed).
     *
     * @param drawMode the kind of geometric primitives to draw, such as
     * GL_LINE_LOOP
     */
    void drawElements(int drawMode) {
        if (vbo == null) {
            generateVbo();
        }
        assert vbo != null;
        if (isModified) {
            updateDataStore();
        }

        bindVbo();
        long indices = 0L;
        int numIndices = capacity();
        GL11.glDrawElements(drawMode, numIndices, elementType, indices);
        DynamXRenderUtils.checkForOglError();
    }

    /**
     * Flip the buffer. The limit is set to the current read/write position, and
     * then the read/write position is zeroed. The data in the buffer is
     * unaffected.
     *
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer flip() {
        getBuffer().flip();
        return this;
    }

    /**
     * Test whether the buffer is read-only.
     *
     * @return {@code true} if read-only, otherwise false
     */
    public boolean isReadOnly() {
        boolean result = getBuffer().isReadOnly();
        return result;
    }

    /**
     * Return the buffer's limit.
     *
     * @return the limit position (&ge;0, &le;capacity)
     */
    public int limit() {
        int result = getBuffer().limit();
        return result;
    }

    /**
     * Make the buffer immutable.
     *
     * @return the (modified) current instance (for chaining)
     */
    @Override
    public DxIndexBuffer makeImmutable() {
        super.makeImmutable();
        return this;
    }

    /**
     * Return the buffer's read/write position.
     *
     * @return the position (&ge;0, &le;limit)
     */
    public int position() {
        int result = getBuffer().position();
        return result;
    }

    /**
     * Alter the buffer's read/write position. The data in the buffer is
     * unaffected.
     *
     * @param newPosition the desired position (&ge;0, &le;limit)
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer position(int newPosition) {
        getBuffer().position(newPosition);
        return this;
    }

    /**
     * Write the specified index at the current read/write position, then
     * increment the position.
     *
     * @param index the index to be written (&ge;0, &lt;numVertices)
     * @return the (modified) current instance (for chaining)
     */
    @Override
    public DxIndexBuffer put(int index) {
        super.put(index);
        setModified();

        return this;
    }

    /**
     * Rewind the buffer. The read/write position is zeroed. The data in the
     * buffer is unaffected.
     *
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer rewind() {
        getBuffer().rewind();
        return this;
    }

    /**
     * Alter the usage hint. Not allowed after the VBO has been created.
     *
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer setDynamic() {
        if (vbo != null) {
            throw new IllegalStateException(
                    "Too late to alter the usage hint.");
        }

        this.usageHint = GL15.GL_DYNAMIC_DRAW;
        return this;
    }

    /**
     * Indicate that the buffer data has changed.
     *
     * @return the (modified) current instance (for chaining)
     */
    public DxIndexBuffer setModified() {
        verifyMutable();
        this.isModified = true;
        return this;
    }
    // *************************************************************************
    // private methods

    /**
     * Bind this VBO to its target.
     */
    private void bindVbo() {
        GL15.glBindBuffer(target, vbo);
        DynamXRenderUtils.checkForOglError();
    }

    /**
     * Generate the VBO for this buffer and initialize its data store.
     */
    private void generateVbo() {
        assert vbo == null;

        this.vbo = GL15.glGenBuffers();
        DynamXRenderUtils.checkForOglError();

        assert position() == 0 : position();
        assert limit() == capacity() : limit();

        bindVbo();

        Buffer buffer = super.getBuffer();
        if (buffer instanceof ByteBuffer) {
            GL15.glBufferData(target, (ByteBuffer) buffer, usageHint);
            DynamXRenderUtils.checkForOglError();

        } else if (buffer instanceof ShortBuffer) {
            GL15.glBufferData(target, (ShortBuffer) buffer, usageHint);
            DynamXRenderUtils.checkForOglError();

        } else {
            GL15.glBufferData(target, (IntBuffer) buffer, usageHint);
            DynamXRenderUtils.checkForOglError();
        }
        isModified = false;

        if (usageHint == GL15.GL_STATIC_DRAW) {
            makeImmutable();
        }
    }

    /**
     * Update the data store.
     */
    private void updateDataStore() {
        assert isModified;

        bindVbo();

        long offset = 0L;
        Buffer buffer = super.getBuffer();
        if (buffer instanceof ByteBuffer) {
            GL15.glBufferSubData(target, offset, (ByteBuffer) buffer);
            DynamXRenderUtils.checkForOglError();

        } else if (buffer instanceof ShortBuffer) {
            GL15.glBufferSubData(target, offset, (ShortBuffer) buffer);
            DynamXRenderUtils.checkForOglError();

        } else {
            GL15.glBufferSubData(target, offset, (IntBuffer) buffer);
            DynamXRenderUtils.checkForOglError();
        }

        isModified = false;
    }
}
