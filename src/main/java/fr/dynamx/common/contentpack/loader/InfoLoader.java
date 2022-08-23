package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.SubInfoTypeEntry;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.common.ProgressManager;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Automatic loader of specific named objects
 *
 * @param <T> The objects class
 * @param <A> The object type if it's an {@link ISubInfoTypeOwner} and you use a {@link SubInfoTypesRegistry}, or {@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner.Empty}
 * @see INamedObject
 * @see ObjectLoader
 */
public class InfoLoader<T extends INamedObject, A extends ISubInfoTypeOwner<?>> {
    /**
     * Optional dependency matcher : <br>
     * Optional blocks are blocks depending on addons that don't throw errors and that are ignored when their dependency isn't loaded.
     */
    public static final Predicate<String> optionalDependencyMatcher = s -> s.equals("Op") || s.equals("OptionalDependency");
    /**
     * Loaded objects, identified by their full name
     */
    protected final Map<String, T> infos = new HashMap<>();
    /**
     * The prefix used to detect associated .dnx files
     */
    protected final String prefix;
    /**
     * A function matching an object packName and name with its object class
     */
    protected final BiFunction<String, String, T> assetCreator;
    /**
     * SubInfoTypesRegistry for this object
     */
    protected final SubInfoTypesRegistry<A> infoTypesRegistry;

    /**
     * @param prefix       The prefix used to detect associated .dnx files
     * @param assetCreator A function matching an object packName and name with its object class
     */
    public InfoLoader(String prefix, BiFunction<String, String, T> assetCreator, @Nullable SubInfoTypesRegistry<A> infoTypesRegistry) {
        this.prefix = prefix;
        this.assetCreator = assetCreator;
        this.infoTypesRegistry = infoTypesRegistry;
    }

    /**
     * @return The prefix used to detect associated .dnx files
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Clears infos, used for hot reload
     *
     * @param hot If it's an hot reload
     */
    public void clear(boolean hot) {
        infos.clear();
    }

    /**
     * Loads a file, only if the prefix matches with this object
     *
     * @param loadingPack The pack owning the object
     * @param configName  The object's name
     * @param inputStream The object file
     * @param hot         If it's an hot reload
     * @return True if this InfoLoader has loaded this object
     * @throws IOException If an error occurs while reading the stream
     */
    public boolean load(String loadingPack, String configName, BufferedReader inputStream, boolean hot) throws IOException {
        if (configName.startsWith(prefix)) {
            T info = assetCreator.apply(loadingPack, configName);
            if (infos.containsKey(info.getFullName()))
                throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
            readInfo(inputStream, info);
            loadItems(info, hot);
            return true;
        }
        return false;
    }

    /**
     * Reads the inputStream into the info object
     *
     * @param reader The object file
     * @param info   The loading info
     */
    @SuppressWarnings("unchecked")
    protected void readInfo(BufferedReader reader, INamedObject info) throws IOException {
        if (info instanceof ISubInfoTypeOwner<?>)
            readInfoWithSubInfos((A) info, reader);
        else {
            List<PackFilePropertyData<?>> foundProperties = new ArrayList<>();
            String s;
            boolean inComment = false;
            while ((s = reader.readLine()) != null) {
                s = s.trim();
                if (s.endsWith("*/")) {
                    if (inComment)
                        inComment = false;
                    else {
                        //DynamXMain.log.error("Illegal multi-line comment end in " + info.getFullName() + " : " + s);
                        DynamXErrorManager.addError(info.getPackName(), "syntax_error", ErrorLevel.HIGH, info.getName(), "Illegal multi-line comment end in line " + s + ", property skipped");
                    }
                } else if (inComment && s.contains("}")) {
                    //DynamXMain.log.error("Found a never ending multi-line comment in " + info.getFullName());
                    DynamXErrorManager.addError(info.getPackName(), "syntax_error", ErrorLevel.FATAL, info.getName(), "Found a never ending multi-line comment, some properties may be missing in-game");
                } else if (!inComment && !s.startsWith("//")) {
                    if (s.startsWith("/*")) {
                        inComment = true;
                    } else if (s.contains("}")) //End of sub property
                        break;
                    else
                        readLineProperty(foundProperties, info, s);
                }
            }
            if (inComment) {
                //DynamXMain.log.error("Found a never ending multi-line comment in " + info.getFullName());
                DynamXErrorManager.addError(info.getPackName(), "syntax_error", ErrorLevel.FATAL, info.getName(), "Found a never ending multi-line comment, some properties may be missing in-game");
            }
            SubInfoTypeAnnotationCache.getOrLoadData(info.getClass()).values().forEach(p -> {
                if (p.isRequired() && !foundProperties.contains(p)) {
                    DynamXErrorManager.addError(info.getPackName(), "required_property", ErrorLevel.HIGH, info.getName(), p.getConfigFieldName());
                }
            });
        }
    }

    /**
     * Loads the contents of the input reader into the loading object <br>
     * Supports recursive loading of {@link fr.dynamx.api.contentpack.object.subinfo.SubInfoType}s
     *
     * @param obj    The object to load
     * @param reader The data of the object
     * @throws IOException If a reading error occurs
     */
    protected void readInfoWithSubInfos(A obj, BufferedReader reader) throws IOException {
        List<PackFilePropertyData<?>> foundProperties = obj.getInitiallyConfiguredProperties();
        //Read the category
        String s;
        boolean inComment = false;
        while ((s = reader.readLine()) != null) {
            s = s.trim();
            if (s.endsWith("*/")) {
                if (inComment)
                    inComment = false;
                else {
                    //DynamXMain.log.error("Illegal multi-line comment end in " + obj.getFullName() + " : " + s);
                    DynamXErrorManager.addError(obj.getPackName(), "syntax_error", ErrorLevel.HIGH, obj.getName(), "Illegal multi-line comment end in line " + s + ", property skipped");
                }
            } else if (!inComment && !s.startsWith("//")) {
                if (s.startsWith("/*")) {
                    inComment = true;
                } else if (s.contains("{")) { //New sub property
                    String name = s.replace("{", "").trim();
                    ISubInfoType<A> type = getClassForPropertyOwner(obj, name);
                    if (type != null) { //Read all properties of type
                        readInfo(reader, type);
                        type.appendTo(obj);
                    } else //Skip invalid properties
                        while ((s = reader.readLine()) != null && !s.contains("}")) ;
                } else if (s.contains("}")) {//End of sub property
                    break;
                } else {
                    readLineProperty(foundProperties, obj, s);
                }
            }
        }
        if (inComment) {
            //DynamXMain.log.error("Found a never ending multi-line comment in " + obj.getFullName());
            DynamXErrorManager.addError(obj.getPackName(), "syntax_error", ErrorLevel.FATAL, obj.getName(), "Found a never ending multi-line comment, some properties may be missing in-game");
        }

        SubInfoTypeAnnotationCache.getOrLoadData(obj.getClass()).values().forEach(p -> {
            if (p.isRequired() && !foundProperties.contains(p)) {
                DynamXErrorManager.addError(obj.getPackName(), "required_property", ErrorLevel.HIGH, obj.getName(), p.getConfigFieldName());
            }
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
    protected void readLineProperty(List<PackFilePropertyData<?>> foundProperties, INamedObject obj, String line) {
        if (line.contains(":")) {
            int index = line.indexOf(':');
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            IPackFilePropertyFixer propertyFixer = infoTypesRegistry.getSubInfoTypePropertiesFixer(obj.getClass());
            if (propertyFixer != null) {
                IPackFilePropertyFixer.FixResult fixResult = propertyFixer.fixInputField(obj, key, value);
                if (fixResult != null) {
                    if (fixResult.isDeprecation()) {
                        INamedObject parent = obj;
                        if(parent instanceof ISubInfoType)
                            parent = ((ISubInfoType<?>) parent).getRootOwner();
                        DynamXErrorManager.addError(obj.getPackName(), "deprecated_prop", ErrorLevel.LOW, parent.getName(), "Deprecated config key found " + key + " in " + obj.getName()+". You should now use " + fixResult.newKey());
                    }
                    key = fixResult.newKey();
                    value = fixResult.newValue(value);
                }
            }
            PackFilePropertyData<?> d = setFieldValue(obj, key, value);
            if (d != null) foundProperties.add(d);
        } else if (!line.isEmpty()) {
            //DynamXMain.log.warn("Illegal line content (not a comment, not a property) in " + obj.getFullName() + " : " + line);
            DynamXErrorManager.addError(obj.getPackName(), "syntax_error", ErrorLevel.LOW, obj.getName(), "Missing ':' on line " + line + ", and not a comment");
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
                if(parent instanceof ISubInfoType)
                    parent = ((ISubInfoType<?>) parent).getRootOwner();
                DynamXErrorManager.addError(obj.getPackName(), "missing_prop", ErrorLevel.HIGH, parent.getName(), "PackFileProperty with name " + key + " does not exists in part " + obj.getName());
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
    protected ISubInfoType<A> getClassForPropertyOwner(A obj, String name) {
        String[] tags = name.split("#");
        for (SubInfoTypeEntry<A> type : infoTypesRegistry.getRegisteredEntries()) {
            if (type.matches(tags[0]))
                return type.create(obj, tags[0]);
        }
        if (tags.length > 1 && optionalDependencyMatcher.test(tags[1])) {
            DynamXMain.log.debug("Ignoring optional block " + name + " in " + obj);
        } else {
            //DynamXMain.log.error("Unknown sub property " + tags[0] + " in " + obj.getFullName());
            DynamXErrorManager.addError(obj.getPackName(), "unknown_sub_info", ErrorLevel.HIGH, obj.getName(), name);
        }
        return null;
    }

    /**
     * Puts the info into infos map, and updates other references to this objects (in {@link IInfoOwner}s for example)
     */
    public void loadItems(T info, boolean hot) {
        info.onComplete(hot);
        infos.put(info.getFullName(), info);
    }

    /**
     * @return True if postLoad() should be called
     */
    public boolean hasPostLoad() {
        return !infos.isEmpty() && infos.values().iterator().next() instanceof IShapeContainer;
    }

    /**
     * Used to compute shapes of {@link IShapeContainer}s
     *
     * @param hot True if it's an hot reload
     */
    public void postLoad(boolean hot) {
        if (hasPostLoad()) {
            ProgressManager.ProgressBar bar1 = ProgressManager.push("Generating " + getPrefix() + " shapes", infos.size());
            for (T info : infos.values()) {
                bar1.step(info.getFullName());
                try {
                    ((IShapeContainer) info).generateShape();
                } catch (Exception e) {
                    ((IShapeContainer) info).markFailedShape();
                    //DynamXMain.log.fatal("Cannot load physics collision shape of " + info.getFullName() + " !", e);
                    DynamXErrorManager.addError(info.getPackName(), "collision_shape_error", ErrorLevel.FATAL, info.getName(), null, e);
                }
            }
            ProgressManager.pop(bar1);
        }
    }

    /**
     * @return The info from the info's full name, or null
     */
    @Nullable
    public T findInfo(String infoFullName) {
        return infos.get(infoFullName);
    }

    /**
     * @return Returns all owned infos
     */
    public Map<String, T> getInfos() {
        return infos;
    }

    /**
     * @return True if no info is registered
     */
    public boolean isEmpty() {
        return infos.isEmpty();
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
                T obj = o.charAt(0) == '*' ? findInfo(of) : assetCreator.apply(pack, object);
                if (obj == null)
                    throw new IllegalArgumentException("Object " + o.substring(1) + " not found for pack sync in " + getPrefix());
                try {
                    decodeAObject(obj, d);
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
     * @param obj     The object to override
     * @param dataStr The object data
     */
    protected void decodeAObject(INamedObject obj, byte[] dataStr) throws UnsupportedEncodingException {
        Map<String, PackFilePropertyData<?>> data = SubInfoTypeAnnotationCache.getOrLoadData(obj.getClass());
        String[] split = new String(dataStr, StandardCharsets.UTF_8).split("\n");
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

    @Nullable
    public SubInfoTypesRegistry<A> getSubInfoTypesRegistry() {
        return infoTypesRegistry;
    }

    /**
     * @throws IllegalArgumentException If this object does not support sub info types or if this {@link SubInfoTypeEntry} is duplicated
     * @deprecated Use {@link fr.dynamx.api.contentpack.registry.RegisteredSubInfoType} annotation
     */
    @Deprecated
    public void addSubInfoType(SubInfoTypeEntry<A> entry) {
        if (infoTypesRegistry == null)
            throw new IllegalArgumentException("This object does not support sub info types !");
        infoTypesRegistry.addSubInfoType(entry);
    }

    public boolean hasSubInfoTypesRegistry() {
        return infoTypesRegistry != null;
    }
}
