package fr.dynamx.utils.errors;

import fr.aym.acslib.api.services.ErrorManagerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamXErrorRegistry
{
    public static final ErrorFormatter SINGLE_ERROR = (errorBuilder, addStackStrace, errors) -> {
        for(ErrorManagerService.ErrorData error : errors) {
            if(error.getMessage() != null)
                errorBuilder.append("- ").append(error.getGenericType()).append(" : ").append(error.getMessage()).append("\n");
            else
                errorBuilder.append("- ").append(error.getGenericType()).append(" : ").append("\n");
            if(error.getException() != null) {
                errorBuilder.append(error.getException().toString()).append("\n");
                if(addStackStrace) {
                    for(StackTraceElement stackTraceElement : error.getException().getStackTrace()) {
                        errorBuilder.append(stackTraceElement.toString()).append("\n");
                    }
                }
            }
        }
    };
    public static final ErrorFormatter MULTIPLE_ERROR_ONE_LI = (errorBuilder, addStackStrace, errors) -> {
        errorBuilder.append("- ").append(errors.get(0).getGenericType()).append(" : ");
        errorBuilder.append(errors.stream().map(ErrorManagerService.ErrorData::getMessage).reduce((a, b) -> a + ", " + b).orElse("reduce error"));
        errorBuilder.append("\n");
    };

    public static final ErrorFormatter MULTIPLE_ERROR = (errorBuilder, addStackStrace, errors) -> {
        errorBuilder.append("- ").append(errors.get(0).getGenericType()).append(" : ");
        if(errors.size() > 1) {
            errorBuilder.append("\n");
            for (ErrorManagerService.ErrorData error : errors) {
                if (error.getMessage() != null)
                    errorBuilder.append("-> ").append(error.getMessage()).append("\n");
            }
        } else if(errors.get(0).getMessage() != null)
            errorBuilder.append(errors.get(0).getMessage()).append("\n");
    };

    private static final Map<String, ErrorFormatter> ERRORS = new HashMap<>();

    public static ErrorFormatter getErrorFormatter(String errorType) {
        return ERRORS.getOrDefault(errorType, SINGLE_ERROR);
    }

    public static void registerErrorFormatter(String errorType, ErrorFormatter errorFormatter) {
        ERRORS.put(errorType, errorFormatter);
    }

    public interface ErrorFormatter {
        void formatError(StringBuilder errorBuilder, boolean addStackStrace, List<ErrorManagerService.ErrorData> errors);
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
