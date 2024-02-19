package fr.dynamx.bb;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;


public class OBBModelBone {
    public String name;
    public OBBModelBone parent;
    public Vector3f origin = new Vector3f();
    public Vector3f translation = new Vector3f(0, 0, 0);
    public Vector3f rotation = new Vector3f();
    public ArrayList<OBBModelBone> children = new ArrayList<>();
    public Matrix4f currentPose = new Matrix4f();
    public static final Vector3f PITCH = new Vector3f(1, 0, 0);
    public static final Vector3f YAW = new Vector3f(0, -1, 0);
    public static final Vector3f ROLL = new Vector3f(0, 0, -1);

    public void updatePose(OBBModelObject obbModelObject) {
        obbModelObject.onBoneUpdatePose(this);
    }

    public void computePose(Matrix4f matrix) {
        matrix = matrix
                .translate(translation)
                .translate(origin)
                .rotate(rotation.y, YAW)
                .rotate(rotation.x, PITCH)
                .rotate(rotation.z, ROLL)
                .translate(origin.negate(null));

        currentPose = new Matrix4f(matrix);
        for (OBBModelBone child : children) {
            child.computePose(new Matrix4f(matrix));
        }
    }
}
