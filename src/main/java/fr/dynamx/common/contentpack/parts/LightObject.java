package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link LightObject} is the link between an obj part and a light (designed by its id) <br>
 * One part can have multiple {@link LightObject}
 *
 * @see PartLightSource
 */
public class LightObject {
    @PackFileProperty(configNames = "LightId")
    protected String lightId = "";
    @Getter
    @PackFileProperty(configNames = "Textures", defaultValue = "Textures: Light_On")
    protected String[] textures;
    @Getter
    @PackFileProperty(configNames = "BlinkSequenceTicks", required = false, defaultValue = "none")
    protected int[] blinkSequence;
    @Getter
    @PackFileProperty(configNames = "RotateDuration", required = false, defaultValue = "0")
    protected int rotateDuration;

    @Getter
    @PackFileProperty(configNames = "SpotLightColor", required = false)
    protected Vector3f spotLightColor = new Vector3f(1, 1, 1);

    @Getter
    @PackFileProperty(configNames = "ActivationState", required = false)
    protected ActivationState activationState = ActivationState.NONE;

    protected int lightIdHashed;

    @Getter
    private final List<TextureVariantData> blinkTextures = new ArrayList<>();

    protected void hashLightId() {
        try {
            lightIdHashed = Integer.parseInt(lightId);
        } catch (NumberFormatException e) {
            lightIdHashed = lightId.hashCode();
        }
    }

    public int getLightId() {
        return lightIdHashed;
    }

    @RegisteredSubInfoType(name = "LightObject", registries = {SubInfoTypeRegistries.LIGHTS, SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS, SubInfoTypeRegistries.ITEMS, SubInfoTypeRegistries.ARMORS}, strictName = false)
    public static class SubLightObject extends LightObject implements ISubInfoType<PartLightSource> {
        protected final PartLightSource owner;

        public SubLightObject(ISubInfoTypeOwner<PartLightSource> owner) {
            this.owner = (PartLightSource) owner;
        }

        @Override
        public void appendTo(PartLightSource owner) {
            hashLightId();
            owner.addLightSource(this);
        }

        @Override
        public String getName() {
            return "LightObject in " + owner.getPartName();
        }

        @Override
        public String getPackName() {
            return owner.getPackName();
        }

        @Nullable
        @Override
        public PartLightSource getOwner() {
            return owner;
        }
    }

   /* public interface SpotLightContainer extends ISubInfoTypeOwner<SpotLightContainer> {
        void addSpotLight(SpotLightObject spotLightObject);
    }*/

    public enum ActivationState{
        NONE, ALWAYS, REDSTONE_SIGNAL, INTERACT;

        public static ActivationState fromString(String targetName) {
            for (ActivationState activationState : values()) {
                if (activationState.name().equalsIgnoreCase(targetName)) {
                    return activationState;
                }
            }
            throw new IllegalArgumentException("Invalid activation state '" + targetName + "'");
        }
    }
}
