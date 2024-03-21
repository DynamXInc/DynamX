/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2016 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package de.javagl.jgltf.model.io.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

import de.javagl.jgltf.impl.v2.GlTF;

/**
 * A class for reading a version 2.0 {@link GlTF} from an input stream
 */
public final class GltfReaderV2
{
    // Note: This class could use GltfReader as a delegate, and could
    // then verify that the glTF has the right version. Right now, it
    // assumes that it is only used for glTF 2.0 inputs.
    
    /**
     * Creates a new glTF reader
     */
    public GltfReaderV2()
    {
        // Default constructor
    }
    
    /**
     * Read the {@link GlTF} from the given stream
     *  
     * @param inputStream The input stream
     * @return The {@link GlTF}
     * @throws IOException If an IO error occurs
     */
    public GlTF read(InputStream inputStream) throws IOException
    {
    	InputStreamReader reader = new InputStreamReader(inputStream);
    	GlTF gltf = new Gson().fromJson(reader, GlTF.class);
    	reader.close();
        return gltf;
    }
    
}
