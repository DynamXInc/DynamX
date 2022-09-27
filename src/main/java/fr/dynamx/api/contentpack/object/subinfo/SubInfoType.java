package fr.dynamx.api.contentpack.object.subinfo;

/**
 * Basic implementation of {@link ISubInfoType}, keeping the owner in a field
 */
public abstract class SubInfoType<T extends ISubInfoTypeOwner<T>> implements ISubInfoType<T> {
    private final ISubInfoTypeOwner<T> owner;

    protected SubInfoType(ISubInfoTypeOwner<T> owner) {
        this.owner = owner;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    public ISubInfoTypeOwner<T> getOwner() {
        return owner;
    }
}
