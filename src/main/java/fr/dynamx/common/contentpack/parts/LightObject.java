package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link LightObject} is the link between an obj part and a light (designed by its id) <br>
 * One part can have multiple {@link LightObject}
 *
 * @see PartLightSource
 */
@Setter
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

    /**
     * @return The hashed light id (the light id if it's an int, or the hash if it's a string) <br>
     * In {@link fr.dynamx.common.entities.modules.AbstractLightsModule}, it can be used to get the light (or you can use the string version of the id)
     */
    public int getLightId() {
        return lightIdHashed;
    }

    /**
     * @return The light id as a string
     */
    public String getLightIdString() {
        return lightId;
    }

    @RegisteredSubInfoType(name = "LightObject", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
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
}
