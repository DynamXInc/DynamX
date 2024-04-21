package fr.dynamx.common.capability.itemdata;

import fr.dynamx.common.entities.modules.AbstractLightsModule;

import java.util.*;

public class DynamXItemData {

    public static Map<UUID, AbstractLightsModule.ItemLightsModule> itemInstanceLights = new HashMap<>();

    public AbstractLightsModule.ItemLightsModule itemModule;

    public static void setLightOn(AbstractLightsModule.ItemLightsModule module, boolean state){
        for (Integer id : module.getLightCasterPartSyncs().keySet()) {
            module.setLightOn(id, state);
        }
    }
}
