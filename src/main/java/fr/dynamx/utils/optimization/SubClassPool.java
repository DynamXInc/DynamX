package fr.dynamx.utils.optimization;

public class SubClassPool<T>
{
    private final SubClassPool<T> parent;
    private final int startIndex;
    private int affectedObjectsCount;

    public SubClassPool(SubClassPool<T> parent, int startIndex)
    {
        this.parent = parent;
        this.startIndex = startIndex;
    }

    public SubClassPool<T> getParent() {
        return parent;
    }

    public int getStartIndex() { return startIndex; }

    public int getAffectedObjectsCount() {
        return affectedObjectsCount;
    }

    public void affectObject(T obj)
    {
        affectedObjectsCount++;
    }

    public int getHierarchy() {
        SubClassPool<T> pool = this;
        int c = 0;
        while(pool != null)
        {
            pool = pool.getParent();
            c++;
        }
        return c;
    }
}
