package fr.dynamx.utils.errors;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.error.*;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.aym.acslib.api.services.error.ErrorFormatter.*;

public class DynamXErrorManager {
    private static final ErrorManagerService errorManager = ACsLib.getPlatform().provideService(ErrorManagerService.class);

    public static final ErrorCategory DYNAMX_ERRORS = errorManager.createErrorCategory(new ResourceLocation(DynamXConstants.ID, "general_errors"), "DynamX errors");

    /**
     * The current {@link ErrorManagerService} instance
     */
    public static ErrorManagerService getErrorManager() {
        return errorManager;
    }

    public static void addError(String pack, String genericType, ErrorLevel errorLevel, String object, String message) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message);
    }

    public static void addError(String pack, String genericType, ErrorLevel errorLevel, String object, String message, Exception exception) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message, exception);
    }

    public static void addError(String pack, String genericType, ErrorLevel errorLevel, String object, String message, Exception exception, int priority) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message, exception, priority);
    }

    public static void printErrors(ErrorLevel minLevel) {
        errorManager.printErrors(DynamXMain.log, Arrays.asList(DYNAMX_ERRORS, ACsLib.getPlatform().getACsLibErrorCategory(), ACsGuiApi.getCssErrorType()), minLevel);
    }

    public static void registerErrorFormatter(String genericType, ErrorFormatter errorFormatter) {
        DYNAMX_ERRORS.registerErrorFormatter(genericType, errorFormatter);
    }

    static {
        registerErrorFormatter("required_property", MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("obj_duplicated_custom_textures", MULTIPLE_ERROR);
        registerErrorFormatter("armor_error", MULTIPLE_ERROR);
        registerErrorFormatter("updates", SINGLE_ERROR);
        registerErrorFormatter("syntax_error", MULTIPLE_ERROR);
        registerErrorFormatter("deprecated_prop", MULTIPLE_ERROR);
        registerErrorFormatter("missing_prop", MULTIPLE_ERROR);
        registerErrorFormatter("unknown_sub_info", MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("sound_error", MULTIPLE_ERROR);
        registerErrorFormatter("config_error", MULTIPLE_ERROR);
        registerErrorFormatter("obj_none_material", MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("mps_error", SINGLE_ERROR);
        registerErrorFormatter("addon_init_error", SINGLE_ERROR);
        registerErrorFormatter("pack_requirements", MULTIPLE_ERROR); //FORMAT
        registerErrorFormatter("collision_shape_error", SINGLE_ERROR);
        registerErrorFormatter("complete_vehicle_error", SINGLE_ERROR);
        registerErrorFormatter("property_parse_error", SINGLE_ERROR); //FORMAT
        registerErrorFormatter("loading_tasks", SINGLE_ERROR);
        registerErrorFormatter("addon_load_error", SINGLE_ERROR); //FORMAT
        registerErrorFormatter("res_pack_load_fail", SINGLE_ERROR);
        registerErrorFormatter("pack_load_fail", SINGLE_ERROR);
        registerErrorFormatter("missing_pack_info", MULTIPLE_ERROR);  //FORMAT
        registerErrorFormatter("pack_file_load_error", SINGLE_ERROR);
        registerErrorFormatter("addon_error", SINGLE_ERROR); //FORMAT
    }
}
