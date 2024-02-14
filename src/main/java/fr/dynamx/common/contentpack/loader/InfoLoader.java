package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.SubInfoTypeEntry;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Automatic loader of specific named objects
 *
 * @param <T> The objects class
 * @see INamedObject
 * @see ObjectLoader
 */
public class InfoLoader<T extends ISubInfoTypeOwner<?>> extends InfoList<T> {
    /**
     * Optional dependency matcher : <br>
     * Optional blocks are blocks depending on addons that don't throw errors and that are ignored when their dependency isn't loaded.
     */
    public static final Predicate<String> optionalDependencyMatcher = s -> s.equals("Op") || s.equals("OptionalDependency");
    /**
     * The prefix used to detect associated .dnx files
     * -- GETTER --
     *
     * @return The prefix used to detect associated .dnx files
     */
    @Getter
    protected final String prefix;
    /**
     * A function matching an object packName and name with its object class
     */
    protected final AssetCreator<T> assetCreator;

    /**
     * @param prefix                      The prefix used to detect associated .dnx files
     * @param assetCreator                A function matching an object packName and name with its object
     * @param defaultSubInfoTypesRegistry The default SubInfoTypesRegistry for this object (can be overridden by ISubInfoTypeOwners)
     */
    public InfoLoader(String prefix, BiFunction<String, String, T> assetCreator, @Nullable SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry) {
        this(prefix, ((pack, name, firstLine) -> assetCreator.apply(pack, name)), defaultSubInfoTypesRegistry);
    }

    /**
     * @param prefix                      The prefix used to detect associated .dnx files
     * @param assetCreator                A function matching an object packName and name with its object
     * @param defaultSubInfoTypesRegistry The default SubInfoTypesRegistry for this object (can be overridden by ISubInfoTypeOwners)
     */
    public InfoLoader(String prefix, AssetCreator<T> assetCreator, @Nullable SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry) {
        super(defaultSubInfoTypesRegistry);
        this.prefix = prefix;
        this.assetCreator = assetCreator;
    }

    @Override
    public String getName() {
        return getPrefix().substring(0, getPrefix().length() - 1);
    }

    /**
     * Loads a file, only if the prefix matches with this object
     *
     * @param loadingPack The pack owning the object
     * @param configName  The object's name
     * @param file        The object file
     * @param hot         If it's a hot reload
     * @return True if this InfoLoader has loaded this object
     * @throws IOException If an error occurs while reading the stream
     */
    public boolean load(String loadingPack, String configName, ContentPackLoader.PackFile file, boolean hot) throws IOException {
        if (configName.startsWith(prefix)) {

            BufferedReader inputStream = null;
            try {
                inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
                T info = assetCreator.create(loadingPack, configName, null);
                if (infos.containsKey(info.getFullName()))
                    throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
                readInfo(getDefaultSubInfoTypesRegistry(), inputStream, info);
                loadItems(info, hot);
                return true;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Reads the inputStream into the info object <br>
     * Loads the contents of the input reader into the loading object <br>
     * Supports recursive loading of {@link fr.dynamx.api.contentpack.object.subinfo.SubInfoType}s
     *
     * @param subInfoTypesRegistry The sub info types registry for this object
     * @param info                 The object to load
     * @param reader               The data of the object
     * @throws IOException If a reading error occurs
     */
    @SuppressWarnings("unchecked")
    protected void readInfo(SubInfoTypesRegistry<T> subInfoTypesRegistry, BufferedReader reader, INamedObject info) throws IOException {
        boolean isSubInfoOwner = info instanceof ISubInfoTypeOwner<?>;
        T obj = isSubInfoOwner ? (T) info : null;
        if (isSubInfoOwner && obj.getSubInfoTypesRegistry() != null)
            subInfoTypesRegistry = (SubInfoTypesRegistry<T>) obj.getSubInfoTypesRegistry();
        List<PackFilePropertyData<?>> foundProperties = isSubInfoOwner ? obj.getInitiallyConfiguredProperties() : new ArrayList<>();
        //Read the category
        String s;
        boolean inComment = false;
        INamedObject parent = info;
        if (parent instanceof ISubInfoType)
            parent = ((ISubInfoType<?>) parent).getRootOwner();
        while ((s = reader.readLine()) != null) {
            s = s.trim();
            if (s.endsWith("*/")) {
                if (inComment)
                    inComment = false;
                else  //TODO FORMAT ERROR
                    DynamXErrorManager.addPackError(info.getPackName(), "syntax_error", ErrorLevel.HIGH, parent.getName(), "Illegal multi-line comment end in line " + s + ", property skipped in " + info.getName());
            } else if (!isSubInfoOwner && inComment && s.contains("}")) {
                DynamXErrorManager.addPackError(info.getPackName(), "syntax_error", ErrorLevel.FATAL, parent.getName(), "Found a never ending multi-line comment in " + info.getName() + ", some properties may be missing in-game");
            } else if (!inComment && !s.startsWith("//")) {
                if (s.startsWith("/*")) {
                    inComment = true;
                } else if (isSubInfoOwner && s.contains("{")) { //New sub property
                    String name = s.replace("{", "").trim();
                    ISubInfoType<T> type = getClassForPropertyOwner(subInfoTypesRegistry, obj, name);
                    if (type != null) { //Read all properties of type
                        readInfo(subInfoTypesRegistry, reader, type);
                        type.appendTo(obj);
                    } else //Skip invalid properties
                        while ((s = reader.readLine()) != null && !s.contains("}")) ;
                } else if (s.contains("}")) {//End of sub property
                    break;
                } else {
                    readLineProperty(subInfoTypesRegistry, foundProperties, info, s);
                }
            }
        }
        if (inComment)
            DynamXErrorManager.addPackError(info.getPackName(), "syntax_error", ErrorLevel.FATAL, parent.getName(), "Found a never ending multi-line comment in " + info.getName() + ", some properties may be missing in-game");
        INamedObject finalParent = parent;
        SubInfoTypeAnnotationCache.getOrLoadData(info.getClass()).values().forEach(p -> {
            if (p.isRequired() && !foundProperties.contains(p) && foundProperties.stream().noneMatch(p2 -> p2.getField() == p.getField()))
                DynamXErrorManager.addPackError(info.getPackName(), "required_property", ErrorLevel.HIGH, finalParent.getName(), "'" + p.getConfigFieldName() + "' in " + info.getName());
        });
    }

    /**
     * Parsed the given line, adding any found (and loaded) property in the foundProperties list <br>
     * It is assumed that the line is not a comment
     *
     * @param foundProperties The list of found properties
     * @param obj             The currently read object
     * @param line            The line to parse
     * @see fr.dynamx.api.contentpack.registry.PackFileProperty
     */
    protected void readLineProperty(SubInfoTypesRegistry<T> subInfoTypesRegistry, List<PackFilePropertyData<?>> foundProperties, INamedObject obj, String line) {
        if (line.contains(":")) {
            int index = line.indexOf(':');
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            IPackFilePropertyFixer propertyFixer = subInfoTypesRegistry.getSubInfoTypePropertiesFixer(obj.getClass());
            if (propertyFixer != null) {
                IPackFilePropertyFixer.FixResult fixResult = propertyFixer.fixInputField(obj, key, value);
                if (fixResult != null) {
                    if (fixResult.isDeprecation()) {
                        INamedObject parent = obj;
                        if (parent instanceof ISubInfoType)
                            parent = ((ISubInfoType<?>) parent).getRootOwner();
                        DynamXErrorManager.addPackError(obj.getPackName(), "deprecated_prop", ErrorLevel.LOW, parent.getName(), "Deprecated config key found " + key + " in " + obj.getName() + ". You should now use " + fixResult.newKey());
                    }
                    if (!fixResult.isKeepOldKey())
                        key = fixResult.newKey();
                    value = fixResult.newValue(value);
                }
            }
            PackFilePropertyData<?> d = setFieldValue(obj, key, value);
            if (d != null) foundProperties.add(d);
        } else if (!line.isEmpty()) {
            INamedObject parent = obj;
            if (parent instanceof ISubInfoType)
                parent = ((ISubInfoType<?>) parent).getRootOwner();
            DynamXErrorManager.addPackError(obj.getPackName(), "syntax_error", ErrorLevel.LOW, parent.getName(), "Missing ':' on line " + line + ", and not a comment");
        }
    }

    /**
     * Sets the value of the field corresponding to "key"
     *
     * @param obj   The object
     * @param key   The name of the java field, should correspond to one {@link fr.dynamx.api.contentpack.registry.PackFileProperty}
     * @param value The string value of the field, automatically parsed via the {@link fr.dynamx.api.contentpack.registry.DefinitionType}
     */
    protected PackFilePropertyData<?> setFieldValue(INamedObject obj, String key, String value) {
        try {
            PackFilePropertyData<?> data = SubInfoTypeAnnotationCache.getFieldFor(obj, key);
            if (data == null) {
                INamedObject parent = obj;
                if (parent instanceof ISubInfoType)
                    parent = ((ISubInfoType<?>) parent).getRootOwner();
                //TODO FORMAT ERROR
                DynamXErrorManager.addPackError(obj.getPackName(), "missing_prop", ErrorLevel.HIGH, parent.getName(), "Property '" + key + "' in " + obj.getName());
                return null;
            }
            return data.apply(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Invalid key " + key + " for " + obj.getFullName(), e);
        }
    }

    /**
     * @param name The name of the sub info type
     * @return The {@link ISubInfoType} corresponding to the given key, or null if not sub info type was found (an error is logged)
     * @throws IllegalArgumentException if the corresponding info type was not found
     * @see SubInfoTypesRegistry
     */
    @Nullable
    protected ISubInfoType<T> getClassForPropertyOwner(SubInfoTypesRegistry<T> subInfoTypesRegistry, T obj, String name) {
        String[] tags = name.split("#");
        String key = tags[0].toLowerCase();
        //Take strict before, and longer keys before
        Collection<SubInfoTypeEntry<T>> types = subInfoTypesRegistry.getEntries().values().stream().sorted((t1, t2) -> t1.isStrict() != t2.isStrict() ? (t1.isStrict() ? -1 : 1) : t2.getKey().length() - t1.getKey().length()).collect(Collectors.toList());
        for (SubInfoTypeEntry<T> type : types) {
            if (type.matches(key))
                return type.create(obj, tags[0]);
        }
        if (key.contains("seat") && subInfoTypesRegistry.getEntries().containsKey("seat")) {
            DynamXErrorManager.addPackError(obj.getPackName(), "deprecated_seat_config", ErrorLevel.LOW, obj.getName(), name);
            return subInfoTypesRegistry.getEntries().get("seat").create(obj, tags[0]);
        }
        if (key.contains("door") && subInfoTypesRegistry.getEntries().containsKey("door")) {
            DynamXErrorManager.addPackError(obj.getPackName(), "deprecated_door_config", ErrorLevel.LOW, obj.getName(), name);
            return subInfoTypesRegistry.getEntries().get("door").create(obj, tags[0]);
        }
        if (tags.length == 1 || !optionalDependencyMatcher.test(tags[1]))
            DynamXErrorManager.addPackError(obj.getPackName(), "unknown_sub_info", ErrorLevel.HIGH, obj.getName(), name);
        // else optional block
        return null;
    }

    @Override
    public void postLoad(boolean hot) {
    }

    /**
     * Computes uniques hash for each object <br>
     * Permits to keep same objects on server and client sides
     *
     * @param hacheur The object hash algorithm
     * @param data    A map where you should put all hash, named by the objects names
     */
    public void hashObjects(PackSyncHandler hacheur, Map<String, byte[]> data) {
        getInfos().values().forEach((ob) -> data.put(ob.getFullName(), hacheur.hash(ob)));
    }

    /**
     * Writes objects data <br>
     * Permits to keep same objects on server and client sides
     *
     * @param objects The objects to encode, all names are prefixed by the type of delta (one character)
     * @param out     A map where you should put all hash, named by the objects names
     */
    public void encodeObjects(List<String> objects, Map<String, byte[]> out) {
        objects.forEach(o -> {
            if (o.charAt(0) == '*' || o.charAt(0) == '-') {
                T object = findInfo(o.substring(1));
                if (object == null) {
                    throw new IllegalArgumentException("Object " + o.substring(1) + " not found for pack sync in " + getPrefix());
                }
                encodeAObject(out, o, object);
            } else if (o.charAt(0) == '+') {
                out.put(o, new byte[0]);
            } else {
                throw new IllegalArgumentException("Wrong delta : " + o);
            }
        });
    }

    /**
     * Writes an object data <br>
     * Permits to keep same objects on server and client sides
     *
     * @param objectKey The object name, prefixed by the type of delta (one character)
     * @param object    The object to encode
     * @param into      A map where the hash is added, named by the object name
     */
    protected void encodeAObject(Map<String, byte[]> into, String objectKey, INamedObject object) {
        StringBuilder sdata = new StringBuilder();
        sdata.append(object.getClass()).append("\n");
        Map<String, PackFilePropertyData<?>> data = SubInfoTypeAnnotationCache.getOrLoadData(object.getClass());
        data.forEach((n, p) ->
        {
            try {
                p.getField().setAccessible(true);
                Object e = p.getField().get(object);
                if (e != null)
                    sdata.append(p.getType().toValue(e));
                else
                    sdata.append("null");
                sdata.append("\n");
                p.getField().setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException("Cannot read  " + object.getFullName() + " : failed on " + n, e);
            }
        });
        into.put(objectKey, sdata.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Overrides owned objects wih new delta <br>
     * Permits to keep same objects on server and client sides
     *
     * @param objects The objects to read, all names are prefixed by the type of delta (one character)
     */
    public void receiveObjects(Map<String, byte[]> objects) {
        objects.forEach((o, d) -> {
            String of = o.substring(1);
            String pack = of.substring(0, of.indexOf('.'));
            String object = of.substring(of.indexOf('.') + 1);
            if (o.charAt(0) == '*' || o.charAt(0) == '-') {
                String[] split = new String(d, StandardCharsets.UTF_8).split("\n");
                T obj = o.charAt(0) == '*' ? findInfo(of) : assetCreator.create(pack, object, split[0]);
                if (obj == null)
                    throw new IllegalArgumentException("Object " + o.substring(1) + " not found for pack sync in " + getPrefix());
                try {
                    String[] props = new String[split.length - 1];
                    System.arraycopy(split, 1, props, 0, split.length - 1);
                    decodeAObject(obj, props);
                    loadItems(obj, o.charAt(0) == '*');
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Cannot decode  " + of, e);
                }
            } else if (o.charAt(0) == '+') {
                if (!infos.containsKey(of))
                    DynamXMain.log.warn("[PACK_SYNC] Cannot remove " + of + " : not found x)");
                infos.remove(of);
            } else {
                throw new IllegalArgumentException("Wrong delta : " + o);
            }
        });
        postLoad(true);
        DynamXMain.log.info("[PACK_SYNC] Synced " + getPrefix());
    }

    /**
     * Overrides the object wih new delta <br>
     * Permits to keep same objects on server and client sides
     *
     * @param obj   The object to override
     * @param split The object data
     */
    protected void decodeAObject(INamedObject obj, String[] split) throws UnsupportedEncodingException {
        Map<String, PackFilePropertyData<?>> data = SubInfoTypeAnnotationCache.getOrLoadData(obj.getClass());
        int i = 0;
        if (split.length != data.size()) {
            throw new IllegalStateException("Wrong number of properties srv " + split.length + " cli " + data.size());
        }
        for (Map.Entry<String, PackFilePropertyData<?>> entry : data.entrySet()) {
            String n = entry.getKey();
            setFieldValue(obj, n, split[i]);
            i++;
        }
    }

    public interface AssetCreator<T extends INamedObject> {
        T create(String pack, String name, String clazz);
    }
}
