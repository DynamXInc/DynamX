package fr.dynamx.utils.errors;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.error.*;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.*;

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

    private static String getErrorName(ErrorData errorData, boolean addStackTrace) {
       String translated = I18n.translateToLocal("dynamx.error."+errorData.getGenericType());
       return addStackTrace ? (translated + " (" + errorData.getGenericType() +")") : translated;
    }

    public static final ErrorFormatter FORMATTER_SINGLE_ERROR = (errorBuilder, addStackTrace, errors) -> {
        for(ErrorData error : errors) {
            if(error.getMessage() != null)
                errorBuilder.append("- ").append(getErrorName(error, addStackTrace)).append(" : ").append("\n").append(error.getMessage()).append("\n");
            else
                errorBuilder.append("- ").append(getErrorName(error, addStackTrace)).append(" : ").append("\n");
            if(error.getException() != null) {
                errorBuilder.append(error.getException().toString()).append("\n");
                if(addStackTrace) {
                    for(StackTraceElement stackTraceElement : error.getException().getStackTrace()) {
                        errorBuilder.append(stackTraceElement.toString()).append("\n");
                    }
                }
            }
        }
    };
    public static final ErrorFormatter FORMATTER_MULTIPLE_ERROR_ONE_LI = (errorBuilder, addStackTrace, errors) -> {
        errorBuilder.append("- ").append(getErrorName(errors.get(0), addStackTrace)).append(" : ").append("\n");
        errorBuilder.append(errors.stream().map(ErrorData::getMessage).reduce((a, b) -> a + ", " + b).orElse("reduce error"));
        errorBuilder.append("\n");
    };
    public static final ErrorFormatter FORMATTER_MULTIPLE_ERROR = (errorBuilder, addStackTrace, errors) -> {
        errorBuilder.append("- ").append(getErrorName(errors.get(0), addStackTrace)).append(" : ").append("\n");
        if(errors.size() > 1) {
            for (ErrorData error : errors) {
                if (error.getMessage() != null)
                    errorBuilder.append("-> ").append(error.getMessage()).append("\n");
            }
        } else if(errors.get(0).getMessage() != null)
            errorBuilder.append(errors.get(0).getMessage()).append("\n");
    };

    static {
        registerErrorFormatter("required_property", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("obj_duplicated_custom_textures", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("armor_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("updates", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("syntax_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("deprecated_prop", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("missing_prop", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("unknown_sub_info", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("sound_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("config_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter("obj_none_material", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter("mps_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("addon_init_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("pack_requirements", FORMATTER_MULTIPLE_ERROR); //FORMAT
        registerErrorFormatter("collision_shape_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("complete_vehicle_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("property_parse_error", FORMATTER_MULTIPLE_ERROR); //FORMAT
        registerErrorFormatter("loading_tasks", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("addon_load_error", FORMATTER_SINGLE_ERROR); //FORMAT
        registerErrorFormatter("res_pack_load_fail", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("pack_load_fail", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("missing_pack_info", FORMATTER_MULTIPLE_ERROR);  //FORMAT
        registerErrorFormatter("pack_file_load_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter("addon_error", FORMATTER_SINGLE_ERROR); //FORMAT
    }
}
