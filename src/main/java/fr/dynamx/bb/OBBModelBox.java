package fr.dynamx.bb;

import fr.dynamx.client.ClientProxy;
import fr.dynamx.utils.maths.DynamXMath;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OBBModelBox {
    public String name;
    public Vector3f anchor;
    public Vector3f rotation;
    public Vector3f size;
    public Vector3f center;
    public Axis axis = new Axis();
    public Axis axisNormal = new Axis();

    public static class Axis implements Iterable<Vector3f> {
        public Vector3f x = new Vector3f(1, 0, 0);
        public Vector3f y = new Vector3f(0, 1, 0);
        public Vector3f z = new Vector3f(0, 0, 1);

        @Override
        public Iterator<Vector3f> iterator() {
            List<Vector3f> list = new ArrayList<>();
            list.add(x);
            list.add(y);
            list.add(z);
            return list.iterator();
        }

        public Axis copy() {
            Axis axis = new Axis();
            axis.x = new Vector3f(this.x);
            axis.y = new Vector3f(this.y);
            axis.z = new Vector3f(this.z);
            return axis;
        }

        public Vector3f getAxis(int i) {
            if (i == 0) {
                return x;
            } else if (i == 1) {
                return y;
            } else {
                return z;
            }
        }
    }

    public static class RayCastResult {
        public double result;
        public Vector3f normal;

        public RayCastResult(double result, Vector3f normal) {
            this.result = result;
            this.normal = normal;
        }

    }

    public OBBModelBox copy() {
        OBBModelBox box = new OBBModelBox();
        box.name = this.name;
        box.anchor = new Vector3f(this.anchor);
        box.rotation = new Vector3f(this.rotation);
        box.size = new Vector3f(this.size);
        box.center = new Vector3f(this.center);
        box.axis = axis.copy();
        box.axisNormal = axisNormal.copy();
        return box;
    }

    public void compute(Matrix4f matrix) {
        center = DynamXMath.transform(matrix, anchor,null).translate(matrix.m30, matrix.m31, matrix.m32);
        matrix = matrix.rotate(rotation.y, OBBModelBone.YAW).rotate(rotation.x, OBBModelBone.PITCH)
                .rotate(rotation.z, OBBModelBone.ROLL).scale(size);
        axisNormal = new Axis();
        axisNormal.x = DynamXMath.transform(matrix, axisNormal.x, null).normalise(null);
        axisNormal.y = DynamXMath.transform(matrix, axisNormal.y, null).normalise(null);
        axisNormal.z = DynamXMath.transform(matrix, axisNormal.z, null).normalise(null);
        axis = new Axis();
        axis.x = DynamXMath.transform(matrix, axis.x, null);
        axis.y = DynamXMath.transform(matrix, axis.y, null);
        axis.z = DynamXMath.transform(matrix, axis.z, null);
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugBox() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(anchor.x, anchor.y, anchor.z);
        GlStateManager.scale(size.x * 2, size.y * 2, size.z * 2);
        GlStateManager.rotate((float) Math.toDegrees(rotation.x), -1, 0, 0);
        GlStateManager.rotate((float) Math.toDegrees(rotation.y), 0, -1, 0);
        GlStateManager.rotate((float) Math.toDegrees(rotation.z), 0, 0, 1);
        GlStateManager.scale(16, 16, 16);
        ClientProxy.obbModel.renderModel(true);
        GlStateManager.scale(1 / 16f, 1 / 16f, 1 / 16f);
        GlStateManager.popMatrix();
    }

    public static boolean testCollisionOBBAndOBB(OBBModelBox obb1, OBBModelBox obb2) {
        OBBModelBox[] obbs = new OBBModelBox[]{obb1, obb2};
        Vector3f obb1VecX = obb1.axis.x;
        Vector3f obb1VecY = obb1.axis.y;
        Vector3f obb1VecZ = obb1.axis.z;
        Vector3f obb2VecX = obb2.axis.x;
        Vector3f obb2VecY = obb2.axis.y;
        Vector3f obb2VecZ = obb2.axis.z;
        Vector3f axiVec;
        Vector3f dis = new Vector3f(obb2.center.x - obb1.center.x, obb2.center.y - obb1.center.y,
                obb2.center.z - obb1.center.z);
        double proj1;
        double proj2;
        double projAB;
        for (OBBModelBox obb : obbs) {
            for (Vector3f axi : obb.axisNormal) {
                axiVec = axi;
                proj1 = projectionFast(obb1VecX, axiVec) + projectionFast(obb1VecY, axiVec)
                        + projectionFast(obb1VecZ, axiVec);
                proj2 = projectionFast(obb2VecX, axiVec) + projectionFast(obb2VecY, axiVec)
                        + projectionFast(obb2VecZ, axiVec);
                projAB = projectionFast(dis, axiVec);
                if (projAB > proj1 + proj2) {
                    return false;
                }
            }
        }
        return true;
    }

    //t = (d - Dot(p, n)) / Dot(rayDir, n)
    public static RayCastResult testCollisionOBBAndRay(OBBModelBox obb1, Vector3f pos, Vector3f ray) {
        double result = Double.MAX_VALUE;
        final double errorRange = 0.01f;
        boolean flag = false;
        Vector3f resultVec = null;
        Vector3f rayDir = ray;
        for (int i = 0; i < 3; i++) {
            Vector3f n = obb1.axisNormal.getAxis(i);
            Vector3f axi = obb1.axis.getAxis(i);
            Vector3f center1 = new Vector3f(obb1.center.x + axi.x, obb1.center.y + axi.y, obb1.center.z + axi.z);
            Vector3f center2 = new Vector3f(obb1.center.x - axi.x, obb1.center.y - axi.y, obb1.center.z - axi.z);
            double projp = Vector3f.dot(pos, n);
            double d1 = Vector3f.dot(center1, n);
            double d2 = Vector3f.dot(center2, n);
            double len = Vector3f.dot(rayDir, n);
            double t1 = (d1 - projp) / len;
            double t2 = (d2 - projp) / len;
//            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("test3:" + t1 + " - " + t2));
            if (t1 > 0 && t1 < result) {
                Vector3f crossDis = new Vector3f((float) (pos.x + ray.x * t1 - obb1.center.x), (float) (pos.y + ray.y * t1 - obb1.center.y),
                        (float) (pos.z + ray.z * t1 - obb1.center.z));
                if (projectionFast(crossDis, obb1.axisNormal.x) < projectionFast(obb1.axis.x, obb1.axisNormal.x) + errorRange) {
                    if (projectionFast(crossDis, obb1.axisNormal.y) < projectionFast(obb1.axis.y, obb1.axisNormal.y) + errorRange) {
                        if (projectionFast(crossDis, obb1.axisNormal.z) < projectionFast(obb1.axis.z,
                                obb1.axisNormal.z) + errorRange) {
                            result = t1;
                            resultVec = n;
                        }
                    }
                }
            }
            if (t2 > 0 && t2 < result) {
                Vector3f crossDis = new Vector3f((float) (pos.x + ray.x * t2 - obb1.center.x), (float) (pos.y + ray.y * t2 - obb1.center.y),
                        (float) (pos.z + ray.z * t2 - obb1.center.z));
                if (projectionFast(crossDis, obb1.axisNormal.x) < projectionFast(obb1.axis.x, obb1.axisNormal.x) + errorRange) {
                    if (projectionFast(crossDis, obb1.axisNormal.y) < projectionFast(obb1.axis.y, obb1.axisNormal.y) + errorRange) {
                        if (projectionFast(crossDis, obb1.axisNormal.z) < projectionFast(obb1.axis.z,
                                obb1.axisNormal.z) + errorRange) {
                            result = t2;
                            resultVec = n.negate(null);
                        }
                    }
                }
            }
        }
        return new RayCastResult(result, resultVec);
    }

    public static double projectionFast(Vector3f vec1, Vector3f vec2) {
        double delta = Vector3f.dot(vec1, vec2);
        return Math.abs(delta);
    }
}
