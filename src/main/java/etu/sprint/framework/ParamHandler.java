package etu.sprint.framework;

import javax.servlet.http.HttpServletRequest;

import etu.sprint.framework.annotation.RequestParam;

import java.lang.reflect.Parameter;

public class ParamHandler {

    // Convert string to Java type
    public static Object convert(String v, Class<?> type) {
        if (v == null || v.isEmpty()) {
            return getDefaultValue(type);
        }

        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(v);
            if (type == double.class || type == Double.class) return Double.parseDouble(v);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(v);
            if (type == long.class || type == Long.class) return Long.parseLong(v);
            if (type == float.class || type == Float.class) return Float.parseFloat(v);
            if (type == short.class || type == Short.class) return Short.parseShort(v);
            if (type == byte.class || type == Byte.class) return Byte.parseByte(v);
            
            // SPRINT 8: Support pour les tableaux
            if (type.isArray() && type.getComponentType() == String.class) {
                return v.split(",");
            }
            
            return v;
        } catch (NumberFormatException e) {
            return getDefaultValue(type);
        }
    }

    // SPRINT 8: Méthode pour récupérer la valeur par défaut d'un type
    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) return 0;
            else if (type == double.class) return 0.0;
            else if (type == boolean.class) return false;
            else if (type == long.class) return 0L;
            else if (type == float.class) return 0.0f;
            else if (type == short.class) return (short)0;
            else if (type == byte.class) return (byte)0;
            else if (type == char.class) return '\0';
        }
        return null;
    }

    // SPRINT 8: Nouvelle méthode pour gérer les paramètres de requête
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