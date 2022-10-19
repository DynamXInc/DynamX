package fr.dynamx.api.contentpack.object.subinfo;

/**
 * Basic implementation of {@link ISubInfoType}, keeping the owner in a field
 */
public abstract class SubInfoType<T extends ISubInfoTypeOwner<?>> implements ISubInfoType<T> {
    private final T owner;

    protected SubInfoType(ISubInfoTypeOwner<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    @Override
    public T getOwner() {
        return owner;
    }
}
