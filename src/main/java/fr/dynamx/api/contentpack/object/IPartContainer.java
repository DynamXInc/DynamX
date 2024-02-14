package fr.dynamx.api.contentpack.object;

import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface IPartContainer<T extends ISubInfoTypeOwner<?>> extends ISubInfoTypeOwner<T> {

    /**
     * @param clazz The class of the parts to return
     * @param <A>   The type of the parts to return
     * @return All the parts of the given type
     */
    default <A extends BasePart<T>> List<A> getPartsByType(Class<A> clazz) {
        return (List<A>) this.getAllParts().stream().filter(p -> clazz.isAssignableFrom(p.getClass())).collect(Collectors.toList());
    }

    List<BasePart<T>> getAllParts();

    /**
     * @param clazz The class of the part to return
     * @param <A>   The type of the part to return
     * @return The part with the given type and the given id (wheel index for example), or null
     */
    default <A extends BasePart<T>> A getPartByTypeAndId(Class<A> clazz, byte id) {
        return getPartsByType(clazz).stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    /**
     * Adds a {@link BasePart} to this object
     */
    void addPart(BasePart<T> tBasePart);

    default <T extends InteractivePart<?, ?>> List<T> getInteractiveParts() {
        return Collections.emptyList();
    }
}
