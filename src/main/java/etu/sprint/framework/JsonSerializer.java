// File name: JsonSerializer.java
package etu.sprint.framework;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SPRINT 9 : Serialiseur JSON simple pour convertir des objets Java en JSON
 */
public class JsonSerializer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Convertit un objet Java en chaîne JSON
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        StringBuilder json = new StringBuilder();
        serialize(obj, json);
        return json.toString();
    }
    
    /**
     * Sérialise un objet dans un StringBuilder
     */
    private static void serialize(Object obj, StringBuilder json) {
        if (obj == null) {
            json.append("null");
            return;
        }
        
        Class<?> clazz = obj.getClass();
        
        // Cas des types de base
        if (obj instanceof String) {
            json.append("\"").append(escapeJsonString((String) obj)).append("\"");
        } 
        else if (obj instanceof Number) {
            if (obj instanceof Float || obj instanceof Double) {
                json.append(obj);
            } else {
                json.append(obj);
            }
        }
        else if (obj instanceof Boolean) {
            json.append(obj);
        }
        else if (obj instanceof Character) {
            json.append("\"").append(escapeJsonString(String.valueOf(obj))).append("\"");
        }
        else if (obj instanceof Date) {
            json.append("\"").append(DATE_FORMAT.format((Date) obj)).append("\"");
        }
        else if (obj instanceof LocalDate) {
            json.append("\"").append(LOCAL_DATE_FORMAT.format((LocalDate) obj)).append("\"");
        }
        else if (obj instanceof LocalDateTime) {
            json.append("\"").append(LOCAL_DATE_TIME_FORMAT.format((LocalDateTime) obj)).append("\"");
        }
        // Collections
        else if (obj instanceof Iterable) {
            serializeIterable((Iterable<?>) obj, json);
        }
        else if (obj instanceof Map) {
            serializeMap((Map<?, ?>) obj, json);
        }
        else if (obj.getClass().isArray()) {
            serializeArray(obj, json);
        }
        // Objet personnalisé
        else {
            serializeObject(obj, json);
        }
    }
    
    /**
     * Sérialise un objet personnalisé
     */
    private static void serializeObject(Object obj, StringBuilder json) {
        Class<?> clazz = obj.getClass();
        json.append("{");
        
        List<String> fields = new ArrayList<>();
        
        // Récupérer tous les champs (incluant les hérités)
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                fields.add(field.getName());
            }
        }
        
        // Récupérer les getters
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if ((methodName.startsWith("get") || methodName.startsWith("is")) 
                && method.getParameterCount() == 0
                && !Modifier.isStatic(method.getModifiers())) {
                
                String fieldName = methodName.startsWith("get") 
                    ? methodName.substring(3)
                    : methodName.substring(2);
                
                if (!fieldName.isEmpty()) {
                    fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                    if (!fields.contains(fieldName)) {
                        fields.add(fieldName);
                    }
                }
            }
        }
        
        boolean first = true;
        for (String fieldName : fields) {
            try {
                Object value = getFieldValue(obj, fieldName);
                if (value != null || shouldIncludeNulls()) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("\"").append(fieldName).append("\":");
                    serialize(value, json);
                    first = false;
                }
            } catch (Exception e) {
                // Ignorer les champs inaccessibles
            }
        }
        
        json.append("}");
    }
    
    /**
     * Récupère la valeur d'un champ (via getter ou accès direct)
     */
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        
        // Essayer le getter
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method getter = clazz.getMethod(getterName);
            return getter.invoke(obj);
        } catch (NoSuchMethodException e) {
            // Essayer le getter boolean
            getterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method getter = clazz.getMethod(getterName);
                return getter.invoke(obj);
            } catch (NoSuchMethodException e2) {
                // Accéder directement au champ
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (NoSuchFieldException e3) {
                    // Chercher dans la classe parent
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null && superClass != Object.class) {
                        try {
                            Field field = superClass.getDeclaredField(fieldName);
                            field.setAccessible(true);
                            return field.get(obj);
                        } catch (NoSuchFieldException e4) {
                            return null;
                        }
                    }
                    return null;
                }
            }
        }
    }
    
    /**
     * Sérialise une collection
     */
    private static void serializeIterable(Iterable<?> iterable, StringBuilder json) {
        json.append("[");
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                json.append(",");
            }
            serialize(item, json);
            first = false;
        }
        json.append("]");
    }
    
    /**
     * Sérialise un tableau
     */
    private static void serializeArray(Object array, StringBuilder json) {
        json.append("[");
        
        if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            for (int i = 0; i < objArray.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                serialize(objArray[i], json);
            }
        } else if (array instanceof int[]) {
            int[] intArray = (int[]) array;
            for (int i = 0; i < intArray.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(intArray[i]);
            }
        } else if (array instanceof long[]) {
            long[] longArray = (long[]) array;
            for (int i = 0; i < longArray.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(longArray[i]);
            }
        } else if (array instanceof double[]) {
            double[] doubleArray = (double[]) array;
            for (int i = 0; i < doubleArray.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(doubleArray[i]);
            }
        } else if (array instanceof boolean[]) {
            boolean[] booleanArray = (boolean[]) array;
            for (int i = 0; i < booleanArray.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(booleanArray[i]);
            }
        }
        
        json.append("]");
    }
    
    /**
     * Sérialise une Map
     */
    private static void serializeMap(Map<?, ?> map, StringBuilder json) {
        json.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJsonString(String.valueOf(entry.getKey()))).append("\":");
            serialize(entry.getValue(), json);
            first = false;
        }
        json.append("}");
    }
    
    /**
     * Échappe une chaîne pour JSON
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '/': sb.append("\\/"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
    
    /**
     * Détermine si les valeurs null doivent être incluses
     */
    private static boolean shouldIncludeNulls() {
        return true; // Peut être configuré
    }
}