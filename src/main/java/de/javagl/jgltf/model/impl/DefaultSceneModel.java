/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2017 Marco Hutter - http://www.javagl.de
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
package de.javagl.jgltf.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;

/**
 * Implementation of a {@link SceneModel} 
 */
public class DefaultSceneModel extends AbstractNamedModelElement
    implements SceneModel
{
    /**
     * The list of root nodes
     */
    private final List<NodeModel> nodeModels;
    
    /**
     * Creates a new instance
     */
    public DefaultSceneModel()
    {
        this.nodeModels = new ArrayList<NodeModel>();
    }
    
    /**
     * Add the given (root) {@link NodeModel} to this scene
     * 
     * @param node The {@link NodeModel}
     */
    public void addNode(NodeModel node)
    {
       nodeModels.add(node); 
    }
    
    @Override
    public List<NodeModel> getNodeModels()
    {
        return Collections.unmodifiableList(nodeModels);
    }

}
