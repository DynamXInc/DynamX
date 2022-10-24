package fr.dynamx.api.contentpack.object;

import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

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
     * Adds a {@link BasePart} to this object
     */
    void addPart(BasePart<T> tBasePart);

}
