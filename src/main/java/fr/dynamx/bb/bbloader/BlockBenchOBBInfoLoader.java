package fr.dynamx.bb.bbloader;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import fr.dynamx.bb.OBBModelBone;
import fr.dynamx.bb.OBBModelBox;
import fr.dynamx.bb.OBBModelObject;
import fr.dynamx.bb.OBBModelScene;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class BlockBenchOBBInfoLoader {
    public static HashMap<ResourceLocation, BBInfo> infoCache = new HashMap<>();

    public static <T extends OBBModelObject> T loadOBBInfo(Class<T> clazz, ResourceLocation loc) {
        Gson gson = new Gson();
        try {
            BBInfo info;
            if (infoCache.containsKey(loc)) {
                info = infoCache.get(loc);
            } else {
                InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
                info = gson.fromJson(new InputStreamReader(stream), BBInfo.class);
                stream.close();
                infoCache.put(loc, info);
            }
            OBBModelObject object;
            try {
                object = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
            OBBModelScene scene = new OBBModelScene();

            object.scene = scene;

            HashMap<String, OBBModelBone> bones = new HashMap<String, OBBModelBone>();
            info.groups.forEach((g) -> {
                OBBModelBone bone = new OBBModelBone();
                bone.name = g.name;
                bone.origin = new Vector3f();
                bone.origin.x = g.origin[0];
                bone.origin.y = g.origin[1];
                bone.origin.z = g.origin[2];
                bones.put(g.name, bone);
            });
            info.groups.forEach((g) -> {
                if (!g.parent.equals("undefined")) {
                    OBBModelBone parent = bones.get(g.parent);
                    parent.children.add(bones.get(g.name));
                    bones.get(g.name).parent = parent;
                } else {
                    scene.rootBones.add(bones.get(g.name));
                }
            });

            info.cubes.forEach((c) -> {
                OBBModelBox box = new OBBModelBox();
                Vector3f from = new Vector3f(c.from[0], c.from[1], c.from[2]);
                Vector3f to = new Vector3f(c.to[0], c.to[1], c.to[2]);
                Vector3f size = new Vector3f((to.x - from.x) / 2, (to.y - from.y) / 2, (to.z - from.z) / 2);
                Vector3f center = new Vector3f(from.x + size.x, from.y + size.y, from.z + size.z);
                OBBModelBone bone = bones.get(c.parent);
                if (bone == null) {
                    throw new RuntimeException();
                }
                box.name = c.name;
                box.center = center;
                box.anchor = new Vector3f(box.center.x - bone.origin.x, box.center.y - bone.origin.y,
                        box.center.z - bone.origin.z);
                box.size = size;
                box.rotation = new Vector3f(0, 0, 0);
                object.boxes.add(box);
                object.boneBinding.put(box, bone);
            });
            return (T) object;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class BBInfo {
        @SerializedName("groups")
        public ArrayList<Group> groups;
        @SerializedName("cubes")
        public ArrayList<Cube> cubes;
    }

    public static class Cube {
        @SerializedName("name")
        public String name;
        @SerializedName("parent")
        public String parent;
        @SerializedName("from")
        public float[] from;
        @SerializedName("to")
        public float[] to;
    }

    public static class Group {
        @SerializedName("name")
        public String name;
        @SerializedName("parent")
        public String parent;
        @SerializedName("origin")
        public float[] origin;
    }
}
