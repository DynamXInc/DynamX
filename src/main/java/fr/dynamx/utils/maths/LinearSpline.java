/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fr.dynamx.utils.maths;


import com.jme3.math.Vector3f;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nehon
 */
public class LinearSpline {

    @Getter
    private final List<Vector3f> controlPoints = new ArrayList<>();

    /**
     * Create a spline
     *
     * @param controlPoints a list of vector to use as control points of the spline
     *                      If the type of the curve is Bezier curve the control points should be provided
     *                      in the appropriate way. Each point 'p' describing control position in the scene
     *                      should be surrounded by two handler points. This applies to every point except
     *                      for the border points of the curve, who should have only one handle point.
     *                      The pattern should be as follows:
     *                      P0 - H0  :  H1 - P1 - H1  :  ...  :  Hn - Pn
     *                      <p>
     *                      n is the amount of 'P' - points.
     */
    public LinearSpline(List<Vector3f> controlPoints) {
        this.controlPoints.addAll(controlPoints);
    }


    /**
     * Interpolate a position on the spline
     *
     * @param value               a value from 0 to 1 that represent the position between the current control point and the next one
     * @param currentControlPoint the current control point
     * @param store               a vector to store the result (use null to create a new one that will be returned by the method)
     * @return the position
     */
    public Vector3f interpolate(float value, int currentControlPoint, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        DynamXMath.interpolateLinear(value, controlPoints.get(currentControlPoint), controlPoints.get(currentControlPoint + 1), store);

        return store;
    }


}
