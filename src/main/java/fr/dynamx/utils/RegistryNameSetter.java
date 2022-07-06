package fr.dynamx.utils;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * To avoid shitty and horrible FML WARN because of threaded loading and multiple mods in DynamX jar
 */
public class RegistryNameSetter {
    /**
     * Sets the registry name of the given object without sending the shitty warnings of Forge
     */
    public static <T extends IForgeRegistryEntry.Impl<T>> void setRegistryName(T object, String name) {
        ObfuscationReflectionHelper.setPrivateValue(IForgeRegistryEntry.Impl.class, object, new ResourceLocation(name), "registryName");
    }

    /**
     * Sets the registry name of the given object without sending the shitty warnings of Forge
     */
    public static <T extends IForgeRegistryEntry.Impl<T>> void setRegistryName(T object, String modID, String name) {
        setRegistryName(object, modID + ":" + name);
    }

    /**
     * @return The ResourceLocation corresponding to the given resourceName, appending the namespace dynamxmod if no namespace is present
     */
    public static ResourceLocation getResourceLocationWithDynamXDefault(String resourceName) {
        return getDynamXResourceLocation("", resourceName);
    }

    /**
     * @return The ResourceLocation corresponding to the given resourceName, appending 'models/' and the namespace dynamxmod if no namespace is present
     */
    public static ResourceLocation getDynamXModelResourceLocation(String resourceName) {
        return getDynamXResourceLocation("models/", resourceName);
    }

    /**
     * @return The ResourceLocation corresponding to the given resourceName, appending 'pathPrefix' and the namespace dynamxmod if no namespace is present
     */
    public static ResourceLocation getDynamXResourceLocation(String pathPrefix, String resourceName) {
        String[] astring = new String[]{DynamXConstants.ID, resourceName};
        int i = resourceName.indexOf(58);

        if (i >= 0) {
            astring[1] = resourceName.substring(i + 1);

            if (i > 1) {
                astring[0] = resourceName.substring(0, i);
            }
        }
        return new ResourceLocation(astring[0], pathPrefix + astring[1]);
    }
}