package fr.dynamx.common.capability.itemdata;

import fr.dynamx.common.entities.modules.AbstractLightsModule;
import scala.Int;

import java.util.*;

public class DynamXItemData {

    public static Map<UUID, AbstractLightsModule.ItemLightsModule> itemInstanceLights = new HashMap<>();

    public AbstractLightsModule.ItemLightsModule itemModule;
    public List<Integer> lightIds = new ArrayList<>();
    /*public static Map<UUID, AbstractLightsModule.ItemLightsModule> itemInstanceLights = new HashMap<>();

    @Nullable
    public static AbstractLightsModule.ItemLightsModule getLightContainer(ItemStack stack){
        if(!stack.hasTagCompound()){
            return null;
        }
        if(!stack.getTagCompound().hasUniqueId("InstanceUUID")){
            return null;
        }

        return itemInstanceLights.get(stack.getTagCompound().getUniqueId("InstanceUUID"));
    }*/
}
