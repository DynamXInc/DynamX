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
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.ResourceLocation;
import vhacd.VHACD;
import vhacd.VHACDHull;
import vhacd.VHACDParameters;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

import static fr.dynamx.common.DynamXMain.log;

public class ShapeUtils {
    // Shape generation
    public static CompoundCollisionShape generateComplexModelCollisions(ObjModelPath path, String objectName, Vector3f scale, Vector3f centerOfMass, float shapeYOffset) {
        String lowerCaseObjectName = objectName.toLowerCase();
        ResourceLocation dcFileLocation = new ResourceLocation(path.getModelPath().toString().replace(".obj", "_" +lowerCaseObjectName+"_"+ DynamXConstants.DC_FILE_VERSION + ".dc"));
        InputStream dcInputStream = null;
        PackInfo dcFilePackInfo = null;
        for(PackInfo packInfo : path.getPackLocations()) {
            try {
                dcInputStream = packInfo.readFile(dcFileLocation);
                if(dcInputStream != null) {
                    dcFilePackInfo = packInfo;
                    break;
                }
            } catch (IOException e) {
                //TODO FIX
                throw new RuntimeException(e);
            }
        }

        ShapeGenerator shapeGenerator = null;
        long start = System.currentTimeMillis();
        if(dcInputStream != null) {
            //load file
            try {
                shapeGenerator = loadFile(dcInputStream);
            } catch (Exception e) {
                log.error("Cannot load .dc file of " + path + ". Re-creating it. Errored dc file was found in " + dcFilePackInfo, e);
                //do it just after file.delete();
            } finally {
                try {
                    dcInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (shapeGenerator == null) {
            ObjModelData model = DynamXContext.getObjModelDataFromCache(path);
            String modelPath = DynamXMain.resDir + File.separator + path.getPackName() + File.separator + "assets" + //todo prevents from saving in zip files : we use the pack name
                    File.separator + path.getModelPath().getNamespace() + File.separator + path.getModelPath().getPath().replace("/", File.separator);
            String modelName = modelPath.substring(modelPath.lastIndexOf(File.separator) + 1);
            File file = new File(modelPath.replace(".obj", "_" +lowerCaseObjectName+"_"+ DynamXConstants.DC_FILE_VERSION + ".dc"));

            float[] pos = lowerCaseObjectName.isEmpty() ? model.getVerticesPos() : model.getVerticesPos(lowerCaseObjectName);
            int[] indices = lowerCaseObjectName.isEmpty() ? model.getAllMeshIndices() : model.getMeshIndices(lowerCaseObjectName);
            if(pos.length == 0 || indices.length == 0) {
                throw new IllegalArgumentException("Part '" + objectName + "' of '" + path + "' does not exist or is empty. Check the name of the part in the obj file.");
            }

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
                file.getParentFile().mkdirs(); //todo pb if a pack is DartcherPack-Trucks.zip, and PackName: DartcherPack, the file will use DartcherPack
                saveFile(file, shapeGenerator);
            } else {
                log.warn("Saving .dc file of " + modelPath + " of a zipped pack in " + file + ". Consider putting it in the zip file of the pack.");
                try {
                    boolean zipped = file.getPath().contains(".zip");
                    File zipFile;
                    if (zipped) {
                        zipFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(".zip") + 4));
                    } else {
                        zipFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(ContentPackLoader.PACK_FILE_EXTENSION) + ContentPackLoader.PACK_FILE_EXTENSION.length()));
                    }
                    addFilesToExistingZip(zipFile, file, shapeGenerator);

                    log.info("Saved the shape " + file.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            end = System.currentTimeMillis();
            time = end - start;
            log.info("Generated " + file.getName() + " shape in " + time + " ms");
        }
        CompoundCollisionShape collisionShape = new CompoundCollisionShape();
        for (float[] hullPoint : shapeGenerator.getHullPoints()) {
            if(hullPoint.length == 0) {
                throw new IllegalArgumentException("Empty .dc file for part '" + objectName + "' of '" + path + "'. Please delete it, check your obj model and restart the game.");
            }
            HullCollisionShape hullShape = new HullCollisionShape(hullPoint);
            hullShape.setScale(scale.subtract(new Vector3f(.1f, .1f, .1f)));
            collisionShape.addChildShape(hullShape, new Vector3f(centerOfMass.x, shapeYOffset + centerOfMass.y, centerOfMass.z));
        }
        long time = System.currentTimeMillis() - start;
        if(time > 10)
            log.warn("Loaded " + dcFileLocation + " in " + time + " ms");
        return collisionShape;
    }

    public static void addFilesToExistingZip(File zipFile, File modelFile, ShapeGenerator shapeGenerator) throws IOException {
        byte[] buf = new byte[1024];
        File outputZipFile = new File(zipFile.getParentFile(), zipFile.getName()+".temp");
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipFile.toPath()));
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outputZipFile.toPath()));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            boolean notInFiles = !modelFile.getName().equals(name);
            if (notInFiles) {
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        // Close the streams
        zin.close();
        // Compress the files

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(modelFile.getName()));
        // Transfer bytes from the file to the ZIP file
        ObjectOutputStream shapeBytes = new ObjectOutputStream(new GZIPOutputStream(out));
        shapeBytes.writeObject(shapeGenerator);

        // Complete the entry
        out.closeEntry();
        // Complete the ZIP file
        out.close();

        System.out.println(zipFile.delete());
        System.out.println("Deleted old");
        System.out.println(outputZipFile.renameTo(zipFile));
    }

    private static void saveFile(File file, ShapeGenerator shapeGenerator) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(Files.newOutputStream(file.toPath())));
            out.writeObject(shapeGenerator);
            out.close();
            log.info("Saved the shape " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ShapeGenerator loadFile(InputStream file) {
        try {
            Set<String> classesSet = Collections.unmodifiableSet(new HashSet(Arrays.asList(ShapeGenerator.class.getName(), ArrayList.class.getName(), float[].class.getName())));
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(file)) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    if (!classesSet.contains(desc.getName())) {
                        throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
                    }
                    return super.resolveClass(desc);
                }
            };
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

    public static List<Vector3f> getDebugVectorList(FloatBuffer[] debugBuffer) {
        return getDebugVectorList(null, debugBuffer);
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
