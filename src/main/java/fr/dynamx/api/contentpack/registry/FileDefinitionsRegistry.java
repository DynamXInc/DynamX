package fr.dynamx.api.contentpack.registry;

import fr.dynamx.common.contentpack.ContentPackLoader;

import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.api.contentpack.registry.DefinitionType.DynamXDefinitionTypes;
import static fr.dynamx.api.contentpack.registry.DefinitionType.DynamXDefinitionTypes.*;

/**
 * Holds all {@link FileDefinition} used by the {@link ContentPackLoader}
 *
 * @deprecated replaced by annotation system
 */
@Deprecated
public class FileDefinitionsRegistry
{
    private static final Map<String, FileDefinition<?>> DEFINITIONS = new HashMap<>();

    /**
     * Registers a new {@link FileDefinition} which can be used by the {@link ContentPackLoader}
     *
     * @param configFieldName The name of the field inside of the config, we advise addon creators to put a unique prefix (e.g. "myaddon_"), to avoid conflicts with other addons. Can be shared between different config types.
     * @param classFieldName The name of the corresponding field name (can be used in various classes at same time). You don't need any prefix here, because the right class would have already been found.
     * @param type The "parser" which will translate the string config's value into a value of the type of the class field. <br>
     *             Here it's a builtin dynamx parser but you can use custom ones with the other addFileDefinition method
     */
    public static void addFileDefinition(String configFieldName, String classFieldName, DynamXDefinitionTypes type)
    {
        addFileDefinition(configFieldName, classFieldName, type.type);
    }

    /**
     * Registers a new {@link FileDefinition} which can be used by the {@link ContentPackLoader}
     *
     * @param configFieldName The name of the field inside of the config, we advise addon creators to put a unique prefix (e.g. "myaddon_"), to avoid conflicts with other addons. Can be shared between different config types.
     * @param classFieldName The name of the corresponding field name (can be used in various classes at same time). You don't need any prefix here, because the right class would have already been found.
     * @param type The "parser" which will translate the string config's value into a value of the type of the class field.
     */
    public static void addFileDefinition(String configFieldName, String classFieldName, DefinitionType<?> type)
    {
        if(DEFINITIONS.containsKey(configFieldName))
            throw new IllegalArgumentException("File definition with config field name "+configFieldName+" is already registered !");
        DEFINITIONS.put(configFieldName, new FileDefinition<>(classFieldName, type));
    }

    /**
     * Gets a FileDefinition from its config field name (can be shared between different configs types)
     * @return null If no FileDefinition was found
     */
    public static FileDefinition<?> getFileDefinition(String configFieldName)
    {
        if(!DEFINITIONS.containsKey(configFieldName))
            return null;
        return DEFINITIONS.get(configFieldName);
    }

    /**
     * Represents a configuration field, contains the corresponding field name (can be used in various classes at same time), and the "parser" ({@link DefinitionType}) able to translation the string value of this field into the correct type
     * @param <T> The corresponding class field type
     * @deprecated Use the annotation system, replaced by the new FileDefinition
     */
    @Deprecated
    public static class FileDefinition<T>
    {
        private final String fieldName;
        private final DefinitionType<T> type;

        /**
         *
         * @param fieldName The field name corresponding to this definition
         * @param type The "parser"
         */
        public FileDefinition(String fieldName, DefinitionType<T> type)
        {
            this.fieldName = fieldName;
            this.type = type;
        }

        /**
         * Parses the provided config value (string format) into the appropriate object to affect it to the class field
         */
        public T parse(String value) { return type.getValue(value); }

        /**
         * @return The field name corresponding to this definition
         */
        public String getFieldName() {
            return fieldName;
        }
    }

    /**
     * Built-in file definitions, automatically registered in the DEFINITIONS list
     */
    @Deprecated
    private enum DynamXDefinitions
    {
        //Item
        defaultName("Name", STRING),
        description("Description", STRING),

        translate("Translate", VECTOR3F),
        //rotate("Rotate", FLOAT_ARRAY),
        scale("Scale", VECTOR3F_INVERSED),

        scaleModifier("ScaleModifier", VECTOR3F),

        position("Position", VECTOR3F_INVERSED_Y),

        //Armor
        armorHead("ArmorHead", STRING),
        armorBody("ArmorBody", STRING),
        armorArms("ArmorArms", STRING_ARRAY),
        armorLegs("ArmorLegs", STRING_ARRAY),
        armorFoot("ArmorFoot", STRING_ARRAY),

        //Blocks
        renderDistance("RenderDistanceSquared", FLOAT),

        //Vehicle
        emptyMass("EmptyMass", INT),
        dragFactor("DragCoefficient", FLOAT),
        model("Model", STRING),
        centerOfMass("CenterOfGravityOffset", VECTOR3F),
        //        compatibleWheels("CompatibleWheels", DefinitionTypes.STRING_ARRAY),
        defaultEngine("DefaultEngine", STRING),
        defaultSounds("DefaultSounds", STRING),
        shapeYOffset("ShapeYOffset", FLOAT),

        //Parts
        partName("PartName", STRING),

        //Steering wheel
        steeringWheelBaseRotation("BaseRotation", FLOAT_ARRAY),
        //steeringWheelRotationPoint("RotationPoint", VECTOR3F_INVERSED_Y),

        //Light source
        textureOn("TextureOn", STRING),

        //Trailer
        trailerAttachPoint("AttachPoint", VECTOR3F_INVERSED_Y),
        trailerAttachStrength("AttachStrength", INT),

        //Caterpillar
        caterpillar("Caterpillar",BOOL),
        caterpillarLeftBuffer("CaterpillarPointsBufferLeft",VECTOR3F_ARRAY_BLENDER),
        caterpillarRightBuffer("CaterpillarPointsBufferRight",VECTOR3F_ARRAY_BLENDER),
        caterpillarWidth("CaterpillarWidth",FLOAT),

        //Textures
        textures("Textures", STRING_ARRAY),
        iconName("Icon", STRING),

        //Wheel
        defaultWheelName("AttachedWheel", STRING),
        isRight("isRight", BOOL),
        //wheelPosition("WheelPosition", DynamXDefinitionTypes.VECTOR3F_INVERSED_Y),
        wheelIsSteerable("IsSteerable", BOOL),
        wheelMaxTurn("MaxTurn", FLOAT),
        drivingWheel("DrivingWheel", BOOL),
        //For motos, since 2.12.0
        rotationPoint("RotationPoint", VECTOR3F_INVERSED_Y),
        suspensionAxis("SuspensionAxis", FLOAT_ARRAY),
        mudGuardPartName("MudGuard", STRING),

        wheelWidth("Width", FLOAT),
        wheelRadius("Radius", FLOAT),
        rimRadius("RimRadius",FLOAT),
        wheelFriction("Friction", FLOAT),
        wheelBrakeForce("BrakeForce", FLOAT),
        wheelRollInInfluence("RollInInfluence", FLOAT),
        suspensionRestLength("SuspensionRestLength", FLOAT),
        suspensionStiffness("SuspensionStiffness", FLOAT),
        suspensionMaxForce("SuspensionMaxForce", FLOAT),
        wheelsDampingRelaxation("WheelDampingRelaxation", FLOAT),
        wheelsDampingCompression("WheelsDampingCompression", FLOAT),

        //Seat
        isDriver("Driver", BOOL),

        //Engine
        power("Power", FLOAT),
        maxRevs("MaxRPM", FLOAT),
        braking("Braking", FLOAT),
        rpmPower("RPMPower", VECTOR3F_0Z),
        speedRange("SpeedRange", INT_ARRAY),
        rpmRange("RPMRange", INT_ARRAY),

        //Sound
        soundName("Sound", STRING),
        pitchRange("PitchRange", FLOAT_ARRAY);

        final String definition;
        final DynamXDefinitionTypes type;

        /**
         * @param def the definition in the config file
         * @param type the type of the definition {@link DynamXDefinitionTypes}
         */
        DynamXDefinitions(String def, DynamXDefinitionTypes type) {
            definition = def;
            this.type = type;
        }
    }

    /*
     * We add all built-in definitions to the DEFINITIONS map
     */
    static
    {
        for(DynamXDefinitions def : DynamXDefinitions.values())
        {
            addFileDefinition(def.definition, def.name(), def.type.type);
        }
       // addFileDefinition("Radius", "wheelRadius", FLOAT);
    }
}
