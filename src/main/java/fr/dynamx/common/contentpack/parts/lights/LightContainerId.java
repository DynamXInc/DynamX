package fr.dynamx.common.contentpack.parts.lights;

import dz.betterlights.lighting.lightcasters.LightCaster;
import fr.dynamx.common.contentpack.parts.LightObject;
import fr.dynamx.common.contentpack.parts.PartLightSource;

import java.util.ArrayList;
import java.util.List;

public class LightContainerId {

    public PartLightSource owner;
    public LightObject controller;
    public List<LightCasterContainer> lightCasterContainers = new ArrayList<>();

    public boolean isEnabled;

    public LightContainerId(PartLightSource owner, LightObject controller) {
        this.owner = owner;
        this.controller = controller;
        isEnabled = controller.getActivationState().equals(LightObject.ActivationState.ALWAYS);
    }

    public static class LightCasterContainer{
        public LightContainerId owner;
        public SpotLightObject spotLightObject;
        public LightCaster lightCaster;

        public LightCasterContainer(LightContainerId owner, SpotLightObject spotLightObject, LightCaster lightCaster) {
            this.owner = owner;
            this.spotLightObject = spotLightObject;
            this.lightCaster = lightCaster;
            lightCaster.setEnabled(owner.isEnabled);
        }
    }
}
