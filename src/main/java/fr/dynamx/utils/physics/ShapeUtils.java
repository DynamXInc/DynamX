package fr.dynamx.utils.physics;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.math.Vector3f;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.type.objects.AbstractProp;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.objloader.ObjModelData;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import vhacd.VHACD;
import vhacd.VHACDHull;
import vhacd.VHACDParameters;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static fr.dynamx.common.DynamXMain.log;

public class ShapeUtils {
    // Shape generation
    public static CompoundCollisionShape generateComplexModelCollisions(ObjModelPath path, String objectName, Vector3f scale, Vector3f centerOfMass, float shapeYOffset) {
        String tessellatorModel = DynamXMain.resDir + File.separator + path.getPackName() + File.separator + "assets" +
                File.separator + path.getModelPath().getNamespace() + File.separator + path.getModelPath().getPath().replace("/", File.separator);

        String modelName = tessellatorModel.substring(tessellatorModel.lastIndexOf(File.separator) + 1);
        File file = new File(tessellatorModel.replace(".obj", "_" + DynamXConstants.DC_FILE_VERSION + ".dc"));
        ShapeGenerator shapeGenerator = null;
        long start = System.currentTimeMillis();
        if (file.exists()) {
            //load file
            try {
                shapeGenerator = loadFile(new FileInputStream(file));
            } catch (Exception e) {
                log.error("Cannot load .dc file of " + tessellatorModel + ". Re-creating it", e);
                file.delete();
                shapeGenerator = null;
            }
        } else if (file.getPath().contains(".zip") || file.getPath().contains(ContentPackLoader.PACK_FILE_EXTENSION)) {
            boolean zipped = file.getPath().contains(".zip");
            //System.out.println("ZIP FILE " + zipped);
            File unZip;
            if (zipped) {
                unZip = new File(file.getPath().replace(".zip", ""));
            } else {
                unZip = new File(file.getPath().replace(ContentPackLoader.PACK_FILE_EXTENSION, ""));
            }
            //System.out.println("Unzip " + unZip);
            if (unZip.exists()) {
                //load file
                try {
                    log.info("Using dezipped .dc file for " + objectName + " of pack " + path.getPackName() + ". Consider putting it in the zip file and delete the dezipped file.");
                    shapeGenerator = loadFile(new FileInputStream(unZip));
                } catch (Exception e) {
                    log.error("Cannot load .dc file of " + tessellatorModel + " (Zipped pack). Re-creating it.", e);
                    unZip.delete();
                }
            } else {
                File zipFile;
                if (zipped) {
                    zipFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(".zip") + 4));
                } else {
                    zipFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(ContentPackLoader.PACK_FILE_EXTENSION) + ContentPackLoader.PACK_FILE_EXTENSION.length()));
                }
                //System.out.println("So zip file " + zipFile + " " + zipFile.exists());
                if (zipFile.exists()) {
                    try {
                        ZipFile zip = new ZipFile(zipFile);
                        ZipEntry entry;
                        if (zipped) {
                            entry = zip.getEntry(file.getPath().substring(file.getPath().lastIndexOf(".zip") + 5).replace(File.separator, "/"));
                        } else {
                            entry = zip.getEntry(file.getPath().substring(file.getPath().lastIndexOf(ContentPackLoader.PACK_FILE_EXTENSION)
                                    + ContentPackLoader.PACK_FILE_EXTENSION.length() + 1).replace(File.separator, "/"));
                        }
                        //System.out.println("Then entry " + entry);
                        if (entry != null) {
                            shapeGenerator = loadFile(zip.getInputStream(entry));
                        }
                    } catch (IOException e) {
                        log.error("Cannot load .dc file of " + tessellatorModel + " (Zipped pack). Creating it out of the zip.", e);
                        shapeGenerator = null;
                    }
                } else {
                    System.out.println("File not found");
                }
            }
        }
        if (shapeGenerator == null) {
            ObjModelData model = DynamXContext.getObjModelDataFromCache(path);

            float[] pos = objectName.isEmpty() ? model.getVerticesPos() : model.getVerticesPos(objectName);
            int[] indices = objectName.isEmpty() ? model.getAllMeshIndices() : model.getMeshIndices(objectName);

            long end = System.currentTimeMillis();
            long time = end - start;
            log.info("Converted " + modelName + " model to shape and in " + time + " ms");

            start = System.currentTimeMillis();
            VHACDParameters parameters = new VHACDParameters();

            parameters.setConvexHullDownSampling(1);
            parameters.setPlaneDownSampling(1);
            parameters.setMaxVerticesPerHull(1024);
            parameters.setVoxelResolution(10000);
            //parameters.setDebugEnabled(true);

            shapeGenerator = new ShapeGenerator(pos, indices, parameters);
            if (!file.getPath().contains(".zip") && !file.getPath().contains(ContentPackLoader.PACK_FILE_EXTENSION)) { //not a zip pack
                saveFile(file, shapeGenerator);
            } else {
                file = new File(file.getPath().replace(".zip", "").replace(ContentPackLoader.PACK_FILE_EXTENSION, ""));
                new File(file.getParent()).mkdirs();
                log.warn("Saving .dc file of " + tessellatorModel + " of a zipped pack in " + file + ". Consider putting it in the zip file of the pack.");
                saveFile(file, shapeGenerator);
            }
            end = System.currentTimeMillis();
            time = end - start;
            log.info("Generated " + modelName + " shape in " + time + " ms");
        }
        CompoundCollisionShape collisionShape = new CompoundCollisionShape();
        for (float[] hullPoint : shapeGenerator.getHullPoints()) {
            HullCollisionShape hullShape = new HullCollisionShape(hullPoint);
            hullShape.setScale(scale.subtract(new Vector3f(.1f, .1f, .1f)));
            collisionShape.addChildShape(hullShape, new Vector3f(centerOfMass.x, shapeYOffset + centerOfMass.y, centerOfMass.z));
        }
        long end = System.currentTimeMillis();
        long time = end - start;
        log.info("Loaded " + modelName + " in " + time + " ms");
        return collisionShape;
    }

    private static void saveFile(File file, ShapeGenerator shapeGenerator) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
            out.writeObject(shapeGenerator);
            out.close();
            log.info("Saved the shape " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ShapeGenerator loadFile(InputStream file) {
        try {
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(file));
            return (ShapeGenerator) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Cannot load " + file, e);
        }
    }

    public static FloatBuffer[] getDebugBuffer(CompoundCollisionShape compoundShape) {
        int i = 0;
        FloatBuffer[] debugBuffer = new FloatBuffer[compoundShape.listChildren().length];
        for (ChildCollisionShape ccs : compoundShape.listChildren()) {
            debugBuffer[i] = getDebugBuffer(ccs.getShape());
            i++;
        }
        return debugBuffer;
    }

    public static FloatBuffer getDebugBuffer(CollisionShape collisionShape) {
        return DebugShapeFactory.getDebugTriangles(collisionShape, DebugShapeFactory.highResolution);
    }

    public static List<Vector3f> getDebugVectorList(CompoundCollisionShape compoundShape, FloatBuffer[] debugBuffer) {
        Vector3fPool.openPool();
        List<Vector3f> vectors = new ArrayList<>();
        if (compoundShape != null) {
            int j = 0;
            for (ChildCollisionShape sh : compoundShape.listChildren()) {
                FloatBuffer fb = debugBuffer[j];
                if (fb != null) {
                    vectors.addAll(DynamXUtils.floatBufferToVec3f(fb, sh.copyOffset(Vector3fPool.get())));
                }
                j++;
            }
        } else {
            for (FloatBuffer fb : debugBuffer) {
                if (fb != null) {
                    vectors.addAll(DynamXUtils.floatBufferToVec3f(fb, Vector3fPool.get()));
                }
            }
        }
        Vector3fPool.closePool();
        return vectors;
    }

    public static MutableBoundingBox getAABB(AbstractProp<?> info, Vector3f min, Vector3f max,
                                             Vector3f additionalScale, Vector3f additionalTranslation) {
        if (additionalScale == null)
            additionalScale = new Vector3f();
        if (additionalTranslation == null)
            additionalTranslation = new Vector3f();
        MutableBoundingBox aabb = new MutableBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
        aabb.scale(info.getScaleModifier().x + additionalScale.x,
                info.getScaleModifier().y + additionalScale.y,
                info.getScaleModifier().z + additionalScale.z);
        if (!(info instanceof PropObject)) {
            aabb.offset(
                    0.5 + info.getTranslation().x + additionalTranslation.x,
                    1.5 + info.getTranslation().y + additionalTranslation.y,
                    0.5 + info.getTranslation().z + additionalTranslation.z);
        }
        return aabb;
    }

    public static void generateModelCollisions(AbstractProp<?> abstractProp, ObjModelData objModelData, CompoundCollisionShape compoundCollisionShape) {
        objModelData.getObjObjects().forEach(objObject -> {
            abstractProp.getCollisionBoxes().add(ShapeUtils.getAABB(abstractProp, objObject.getMesh().min(), objObject.getMesh().max(), new Vector3f(), new Vector3f()));
            objObject.getMesh().addCollisionShape(compoundCollisionShape, abstractProp.getScaleModifier());
        });
    }

    public static class ShapeGenerator implements Serializable {

        public List<float[]> points = new ArrayList<>();

        public ShapeGenerator(float[] positions, int[] indices, VHACDParameters params) {
            List<VHACDHull> hullList = VHACD.compute(positions, indices, params);
            hullList.forEach(hull -> points.add(hull.clonePositions()));
        }

        public List<float[]> getHullPoints() {
            return points;
        }
    }
}
