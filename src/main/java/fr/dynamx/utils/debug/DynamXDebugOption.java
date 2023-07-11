package fr.dynamx.utils.debug;

import java.util.HashMap;
import java.util.Map;

/**
 * A DynamX debug option, allowing to render/log additional information, to help users debugging their packs or their addons
 *
 * @see fr.dynamx.utils.debug.renderer.DebugRenderer
 * @see DynamXDebugOptions
 */
public class DynamXDebugOption {
    /**
     * @param category The category
     * @param name     The name of the debug option
     * @return A new debug option in this category
     */
    public static DynamXDebugOption newOption(DynamXDebugOptions.DebugCategories category, String name) {
        return category.addOption(new DynamXDebugOption(category, name, 1 << category.getOptionCount()));
    }

    /**
     * The returned option will notify the server when enabled on a client machine <br>
     * Note that the server handling of this is hardcoded todo improve handling of ServerDependantDebugOptions
     *
     * @param category The category
     * @param name     The name of the debug option
     * @return A new debug option in this category
     */
    public static ServerDependantDebugOption newServerDependantOption(DynamXDebugOptions.DebugCategories category, String name) {
        return (ServerDependantDebugOption) category.addOption(new ServerDependantDebugOption(category, name, 1 << category.getOptionCount()));
    }

    /**
     * @param name              The name of the debug option
     * @param needsServerInfo   if the option should notify the server when enabled on a client machine (see newServerDependantOption)
     * @param mask              The mask of the option (should be a power of two, and unique in this category)
     * @param incompatibilities The incompatible masks
     * @return A new debug option in terrain category
     */
    public static TerrainDebugOption newTerrainOption(String name, boolean needsServerInfo, int mask, int... incompatibilities) {
        return (TerrainDebugOption) DynamXDebugOptions.DebugCategories.TERRAIN.addOption(new TerrainDebugOption(DynamXDebugOptions.DebugCategories.TERRAIN, name, mask, needsServerInfo, incompatibilities));
    }

    /**
     * @param category The category
     * @param name     The name of the debug option
     * @param mask     The mask of the option (should be a power of two, and unique in this category)
     * @return A new debug option in this category
     */
    public static DynamXDebugOption newOptionWithMask(DynamXDebugOptions.DebugCategories category, String name, int mask) {
        return category.addOption(new DynamXDebugOption(category, name, mask));
    }

    private final String name;
    private String description, subCategory;
    private final int mask;
    private final DynamXDebugOptions.DebugCategories category;

    private DynamXDebugOption(DynamXDebugOptions.DebugCategories category, String name, int mask) {
        this.name = name;
        this.mask = mask;
        this.category = category;
    }

    public int getMask() {
        return mask;
    }

    public DynamXDebugOptions.DebugCategories getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Adds a description to this debug option
     */
    public DynamXDebugOption withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSubCategory() {
        return subCategory;
    }

    /**
     * Adds a sub-category to this debug option
     */
    public DynamXDebugOption withSubCategory(String subCategory) {
        this.subCategory = subCategory;
        return this;
    }

    public DynamXDebugOption enable() {
        category.setState(category.getState() | mask);
        return this;
    }

    public void disable() {
        category.setState((Integer.MAX_VALUE - mask) & category.getState());
    }

    public boolean isActive() {
        return (category.getState() & mask) > 0;
    }

    public String getDisplayName() {
        return name;
    }

    public String getCommandName() {
        return name.replace(" ", "_");
    }

    public int serverRequestMask() {
        return 0;
    }

    public boolean matchesNetMask(int request) {
        return (request & serverRequestMask()) > 0;
    }

    public static class ServerDependantDebugOption extends DynamXDebugOption {
        private static int serverRequestMaskIndex;
        private final int serverRequestMask;

        private ServerDependantDebugOption(DynamXDebugOptions.DebugCategories category, String name, int mask) {
            super(category, name, mask);
            serverRequestMaskIndex++;
            this.serverRequestMask = 1 << serverRequestMaskIndex;
        }

        @Override
        public int serverRequestMask() {
            return serverRequestMask;
        }
    }

    public static class TerrainDebugOption extends ServerDependantDebugOption {
        private final boolean needServerRq;
        private final int[] incompatibilities;
        private Map<Integer, TerrainDebugData> dataIn = new HashMap<>();

        private TerrainDebugOption(DynamXDebugOptions.DebugCategories category, String name, int mask, boolean needServerRq, int... incompatibilities) {
            super(category, name, mask);
            this.needServerRq = needServerRq;
            this.incompatibilities = incompatibilities;
        }

        @Override
        public DynamXDebugOption enable() {
            super.enable();
            for (int incompatibility : incompatibilities)
                getCategory().setState((Integer.MAX_VALUE - incompatibility) & getCategory().getState());
            return this;
        }

        @Override
        public int serverRequestMask() {
            return needServerRq ? super.serverRequestMask() : 0;
        }

        public Map<Integer, TerrainDebugData> getDataIn() {
            return dataIn;
        }

        public void setDataIn(Map<Integer, TerrainDebugData> dataIn) {
            this.dataIn = dataIn;
        }
    }
}
