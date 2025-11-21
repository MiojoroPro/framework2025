package etu.sprint.framework;

import javax.servlet.http.HttpServletRequest;

import etu.sprint.framework.annotation.RequestParam;

import java.lang.reflect.Parameter;

public class ParamHandler {

    // Convert string to Java type
    public static Object convert(String v, Class<?> type) {
        if (v == null) return null;

        if (type == int.class || type == Integer.class) return Integer.parseInt(v);
        if (type == double.class || type == Double.class) return Double.parseDouble(v);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(v);

        return v;
    }

    // Get parameter value either from annotation or from URL extracted params
    public static Object resolveParameter(
            Parameter param,
            HttpServletRequest request,
            String extractedValue
    ) {
        // Check @RequestParam annotation
        if (param.isAnnotationPresent(RequestParam.class)) {
            String name = param.getAnnotation(RequestParam.class).value();
            String reqVal = request.getParameter(name);
            return convert(reqVal, param.getType());
        }

        // If no annotation → use extracted URL param
        return convert(extractedValue, param.getType());
    }
}
