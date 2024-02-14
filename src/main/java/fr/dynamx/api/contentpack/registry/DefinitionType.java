package fr.dynamx.api.contentpack.registry;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.common.contentpack.loader.PackConstants;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.RegistryNameSetter;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.EnumCollisionType;
import lombok.Getter;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.vecmath.Vector2f;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A DefinitionType provides a method to parse a configurable string value to the corresponding java object
 *
 * @param <T> The corresponding object's type
 * @see PackFileProperty
 */
public class DefinitionType<T> {
    private static final Map<Class<?>, DefinitionType<?>> definitionTypes = new HashMap<>();

    /**
     * Gets the parser for this type of object
     *
     * @param type The class of the object to parse
     * @param <A>  The type of the object to parse
     * @return The parser corresponding to the given type, of null
     */
    public static <A> DefinitionType<A> getParserOf(Class<A> type) {
        return (DefinitionType<A>) definitionTypes.get(type);
    }

    private final Function<String, T> parser;
    private final Function<T, String> inverser;

    /**
     * -- GETTER --
     *
     * @return the type name of the property
     */
    @Getter
    private final String typeName;

    /**
     * NOTE : This constructor is not suitable for array parsers
     *
     * @param type   The class of the object parsed by this parser
     * @param parser Takes the configurable string in argument and should return the corresponding object
     */
    public DefinitionType(Class<T> type, Function<String, T> parser, String typeName) {
        this(type, parser, Object::toString, typeName);
        if (type != null && type.isArray())
            throw new IllegalArgumentException("You should add an inverser on " + type + " DefinitionType");
    }

    /**
     * @param type     The class of the object parsed by this parser
     * @param parser   Takes the configurable string in argument and should return the corresponding object
     * @param inverser Gives the string value of an object, should be parseable by the parser
     */
    public DefinitionType(Class<T> type, Function<String, T> parser, Function<T, String> inverser, String typeName) {
        this.parser = parser;
        this.inverser = inverser;
        this.typeName = typeName;
        if (type != null) {
            definitionTypes.put(type, this);
        }
    }

    /**
     * Parses the given configurable string value into the corresponding object
     */
    public T getValue(String value) {
        String newValue = value;
        for (PackConstants packConstants : PackConstants.values()) {
            if (value.contains(packConstants.name())) {
                newValue = value.replace(packConstants.name(), String.valueOf(packConstants.getValue()));
            }
        }
        return parser.apply(newValue);
    }

    /**
     * Returns the string value of this object <br>
     * Used for pack synchronization <br>
     * The object must have been created by this parser
     */
    public String toValue(Object object) {
        return inverser.apply((T) object);
    }

    /**
     * Build-in definition types for this mod, if you want to use another types, you can add them into this enum
     * (via the {@link net.minecraftforge.common.util.EnumHelper} of Forge)
     */
    public enum DynamXDefinitionTypes {
        AUTO(null),
        INT(new DefinitionType<>(int.class, Integer::parseInt, "type.int")),
        BYTE(new DefinitionType<>(byte.class, Byte::parseByte, "type.byte")),
        FLOAT(new DefinitionType<>(float.class, Float::parseFloat, "type.float")),
        BOOL(new DefinitionType<>(boolean.class, Boolean::parseBoolean, "type.boolean")),
        STRING(new DefinitionType<>(String.class, (s) -> s, "type.string")),
        STRING_ARRAY(new DefinitionType<>(String[].class, (s) -> {
            String[] t = s.split(" ");
            String[] string_array = new String[t.length];
            System.arraycopy(t, 0, string_array, 0, string_array.length);
            return string_array;
        }, DefinitionType::arrayToString, "type.string.array")),
        STRING_ARRAY_2D(new DefinitionType<>(String[][].class, (s) -> {
            String[] t = s.split(",");
            String[][] result = new String[t.length][2];
            for (int i = 0; i < t.length; i++) {
                t[i] = t[i].trim();
                String[] ts = t[i].split(" ");
                result[i][0] = ts[0];
                if (ts.length > 1)
                    result[i][1] = ts[1];
            }
            return result;
        }, DefinitionType::arrayToString, "type.string.array2d")),
        INT_ARRAY(new DefinitionType<>(int[].class, (s) -> {
            String[] t = s.split(" ");
            int[] int_array = new int[t.length];
            for (int i = 0; i < int_array.length; i++) {
                int_array[i] = Integer.parseInt(t[i]);
            }
            return int_array;
        }, DefinitionType::arrayToString, "type.int.array")),
        FLOAT_ARRAY(new DefinitionType<>(float[].class, (s) -> {
            String[] t = s.split(" ");
            float[] int_array = new float[t.length];
            for (int i = 0; i < int_array.length; i++) {
                int_array[i] = Float.parseFloat(t[i]);
            }
            return int_array;
        }, DefinitionType::arrayToString, "type.float.array")),
        VECTOR3F_ARRAY_ORDERED(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(",");
            Vector3f[] vec_array = new Vector3f[t.length];
            for (int i = 0; i < vec_array.length; i++) {
                vec_array[i] = new Vector3f(Float.parseFloat(t[i].split(" ")[0]), Float.parseFloat(t[i].split(" ")[1]), Float.parseFloat(t[i].split(" ")[2]));
            }
            return vec_array;
        }, DefinitionType::arrayToString, "type.vector3f.array")),
        VECTOR3F_ARRAY_BLENDER(new DefinitionType<>(Vector3f[].class, (s) -> {
            String[] t = s.split(", ");
            Vector3f[] vec_array = new Vector3f[t.length];
            for (int i = 0; i < vec_array.length; i++) {
                vec_array[i] = new Vector3f(Float.parseFloat(t[i].split(" ")[0]), Float.parseFloat(t[i].split(" ")[2]), Float.parseFloat(t[i].split(" ")[1])*-1);
            }
            return vec_array;
        }, DefinitionType::arrayToString, "type.vector3f.array.blender")),
        VECTOR3F(new DefinitionType<>(Vector3f.class, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2]));
        }, v -> v.x + " " + v.y + " " + v.z, "type.vector3f")),
        QUATERNION(new DefinitionType<>(Quaternion.class, (s) -> {
            String[] t = s.split(" ");
            Quaternion q = new Quaternion(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]), Float.parseFloat(t[0]));
            return q.normalizeLocal();
        }, v -> v.getX() + " " + v.getY() + " " + v.getZ() + " " + v.getW(), "type.quaternion")),
        /**
         * Vector3f def, but only with x and y arguments, z=0
         */
        VECTOR3F_0Z(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), 0);
        }, v -> v.x + " " + v.y, "type.vector3f_0z")),
        /**
         * Vector3f def, but with y and z arguments inversed
         */
        VECTOR3F_INVERSED(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[2]), Float.parseFloat(t[1]));
        }, v -> v.x + " " + v.z + " " + v.y, "type.vector3f.inverse")),
        /**
         * Vector3f def, but with (y= y* -1) and z arguments inversed <br>
         * Blender format
         */
        VECTOR3F_INVERSED_Y(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[2]), Float.parseFloat(t[1]) * -1);
        }, v -> v.x + " " + (-v.z) + " " + v.y, "type.vector3f.inverse_Y")),
        VECTOR2F(new DefinitionType<>(Vector2f.class, (s) -> {
            String[] t = s.split(" ");
            return new Vector2f(Float.parseFloat(t[0]), Float.parseFloat(t[1]));
        }, v -> v.x + " " + v.y, "type.vector2f")),
        VECTOR2F_UNIT(new DefinitionType<>(Vector2f.class, (s) -> {
            String[] t = s.split(" ");
            float[] values = new float[2];
            for (int i = 0; i < t.length; i++) {
                String str = t[i];
                //Populate values (safety) (if the "if" below isn't executed)
                values[i] = Float.parseFloat(str);
                if (str.endsWith("Deg")) {
                    values[i] = (float) Math.toRadians(Float.parseFloat(str.substring(0, str.indexOf("Deg"))));
                }
            }
            return new Vector2f(values[0], values[1]);
        }, v -> v.x + " " + v.y, "type.vector2f")),
        ITEM_RENDER_LOCATION(new DefinitionType<>(Enum3DRenderLocation.class, Enum3DRenderLocation::fromString, "type.item_render_location")),
        SOUND_EVENT(new DefinitionType<>(SoundEvent.class, s -> {
            SoundEvent e = SoundEvent.REGISTRY.getObject(new ResourceLocation(s));
            if (e == null)
                throw new IllegalArgumentException("Sound event " + s + " not found");
            return e;
        }, e -> ObfuscationReflectionHelper.getPrivateValue(SoundEvent.class, e, 1).toString(), "type.sound_event")),
        PARTICLE_TYPE(new DefinitionType<>(EnumParticleTypes.class, (s) -> {
            if (s.equals("none"))
                return null;
            return EnumParticleTypes.getByName(s);
        }, p -> p == null ? "none" : p.getParticleName(), "type.particle")),
        COLLISION_TYPE(new DefinitionType<>(EnumCollisionType.class, EnumCollisionType::valueOf,
                p -> p == null ? EnumCollisionType.SIMPLE.name() : p.name(), "type.collision")),
        SHAPE_TYPE(new DefinitionType<>(PartShape.EnumPartType.class, PartShape.EnumPartType::fromString, "type.shapetype")),
        DYNX_RESOURCE_LOCATION(new DefinitionType<>(ResourceLocation.class, RegistryNameSetter::getDynamXModelResourceLocation, "type.resourcelocation")),
        PLAYER_STAND_ON_TOP(new DefinitionType<>(EnumPlayerStandOnTop.class, EnumPlayerStandOnTop::fromString, "type.player_stand_on_top")),
        PLAYER_SEAT_POSITION(new DefinitionType<>(EnumSeatPlayerPosition.class, EnumSeatPlayerPosition::fromString, "type.player_seat_position")),
        MATERIAL(new DefinitionType<>(Material.class, (s) -> {
            List<String> names = Arrays.asList("AIR", "GRASS", "GROUND", "WOOD", "ROCK", "IRON", "ANVIL", "WATER", "LAVA", "LEAVES", "PLANTS", "VINE", "SPONGE", "CLOTH", "FIRE", "SAND", "CIRCUITS", "CARPET", "GLASS", "REDSTONE_LIGHT", "TNT", "CORAL", "ICE", "PACKED_ICE", "SNOW", "CRAFTED_SNOW", "CACTUS", "CLAY", "GOURD", "DRAGON_EGG", "PORTAL", "CAKE", "WEB", "PISTON", "BARRIER", "STRUCTURE_VOID");
            Material[] materials = new Material[] {Material.AIR, Material.GRASS, Material.GROUND, Material.WOOD, Material.ROCK, Material.IRON, Material.ANVIL, Material.WATER, Material.LAVA, Material.LEAVES, Material.PLANTS, Material.VINE, Material.SPONGE, Material.CLOTH, Material.FIRE, Material.SAND, Material.CIRCUITS, Material.CARPET, Material.GLASS, Material.REDSTONE_LIGHT, Material.TNT, Material.CORAL, Material.ICE, Material.PACKED_ICE, Material.SNOW, Material.CRAFTED_SNOW, Material.CACTUS, Material.CLAY, Material.GOURD, Material.DRAGON_EGG, Material.PORTAL, Material.CAKE, Material.WEB, Material.PISTON, Material.BARRIER, Material.STRUCTURE_VOID};
            return materials[names.indexOf(s.toUpperCase())];
        }, "type.material")),
        AXIS(new DefinitionType<>(DynamXPhysicsHelper.EnumPhysicsAxis.class, DynamXPhysicsHelper.EnumPhysicsAxis::fromString, "type.axis"));

        public final DefinitionType<?> type;

        DynamXDefinitionTypes(DefinitionType<?> type) {
            this.type = type;
        }
    }

    public static <U> String arrayToString(U[] array) {
        StringBuilder s = new StringBuilder();
        for (U a : array) {
            s.append(a.toString()).append(" ");
        }
        return s.toString().trim();
    }

    public static <U> String array2DToString(U[][] array) {
        StringBuilder s = new StringBuilder();
        for (U[] a : array) {
            s.append(arrayToString(a)).append(", ");
        }
        return s.toString().trim();
    }

    public static String arrayToString(int[] array) {
        StringBuilder s = new StringBuilder();
        for (int a : array) {
            s.append(a).append(" ");
        }
        return s.toString().trim();
    }

    public static String arrayToString(float[] array) {
        StringBuilder s = new StringBuilder();
        for (float a : array) {
            s.append(a).append(" ");
        }
        return s.toString().trim();
    }
}
