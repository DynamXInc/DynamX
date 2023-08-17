package fr.dynamx.bb;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class OBBModelScene {
    private Matrix4f matrix = new Matrix4f();
    public List<OBBModelBone> rootBones = new ArrayList<>();

    public void resetMatrix() {
        matrix = new Matrix4f();
    }

    public void translate(float x, float y, float z) {
        matrix.translate(new Vector3f(x, y, z));
    }

    public void translate(double x, double y, double z) {
        matrix.translate(new Vector3f((float) x, (float) y, (float) z));
    }

    public void rotate(float angle, float x, float y, float z) {
        matrix.rotate(angle, new Vector3f(x, y, z));
    }

    public void rotateDegree(float angle, float x, float y, float z) {
        matrix.rotate(angle / 180 * 3.14159f, new Vector3f(x, y, z));
    }

    public void scale(float x, float y, float z) {
        matrix.scale(new Vector3f(x, y, z));
    }

    public void computePose() {
        for (OBBModelBone rootBone : rootBones) {
            rootBone.computePose(new Matrix4f(matrix));
        }
    }

    public void updatePose(OBBModelObject obbModelObject) {
        for (OBBModelBone rootBone : rootBones) {
            rootBone.updatePose(obbModelObject);
        }
    }
}
