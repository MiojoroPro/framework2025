package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.annotation.ModelAttribute;
import etu.sprint.framework.controller.Controller;

/**
 * FrontServlet : Le contrôleur frontal du framework
 * VERSION SPRINT 8 BIS : Support du binding automatique d'objets Java
 */
public class FrontServlet extends HttpServlet {

    private List<RouteMapping> mappings = new ArrayList<>();
    private boolean isScanned = false;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("[FrontServlet] Initialisation OK - Sprint 8 Bis avec binding automatique");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");

        if (!isScanned) {
            synchronized (this) {
                if (!isScanned) {
                    scanControllers();
                    isScanned = true;
                }
            }
        }

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());
        String httpMethod = request.getMethod();

        RouteMapping matched = null;
        String[] extractedParams = null;

        // --- MATCH ROUTE (STATIC + DYNAMIC + HTTP METHOD) ---
        for (RouteMapping rm : mappings) {

            if (!rm.matchesHttpMethod(httpMethod)) {
                continue;
            }

            String regex = convertPathToRegex(rm.getPattern());
            Pattern p = Pattern.compile("^" + regex + "$");
            Matcher m = p.matcher(path);

            if (m.matches()) {
                matched = rm;

                extractedParams = new String[m.groupCount()];
                for (int i = 0; i < m.groupCount(); i++) {
                    extractedParams[i] = m.group(i + 1);
                }
                break;
            }
        }

        PrintWriter out = response.getWriter();

        if (matched == null) {
            out.println("<h1>404 - No route matches " + httpMethod + " " + path + "</h1>");
            return;
        }

        // --- EXECUTE CONTROLLER METHOD ---
        try {
            Method method = matched.getMethod();
            Object controller = matched.getController();

            // Construction des arguments de la méthode (SPRINT 8 BIS)
            Object[] args = buildMethodArguments(method, extractedParams, request);

            Object result = method.invoke(controller, args);

            // --- HANDLE ModelView ---
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;

                // REDIRECT ?
                if (mv.isRedirect()) {
                    String redirectUrl = mv.getView();
                    
                    // Gestion du context path
                    if (redirectUrl.startsWith("/") && !redirectUrl.startsWith(ctx)) {
                        redirectUrl = ctx + redirectUrl;
                        System.out.println("[Redirect] " + mv.getView() + " → " + redirectUrl);
                    }
                    
                    response.sendRedirect(redirectUrl);
                    return;
                }

                // Add data
                for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/" + mv.getView());
                rd.forward(request, response);
                return;
            }

            // --- RESULT NOT ModelView ---
            out.println("<h3>Controller returned : " + result + "</h3>");

        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    /**
     * SPRINT 8 BIS : Construit les arguments pour une méthode de contrôleur
     * Gère @ModelAttribute, Map<String, Object>, @RequestParam et paramètres d'URL
     */
    private Object[] buildMethodArguments(
            Method method, 
            String[] extractedParams, 
            HttpServletRequest request) {
        
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        int extractedIndex = 0;
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // Cas 1: @ModelAttribute annotation
            if (param.isAnnotationPresent(ModelAttribute.class)) {
                // Si le paramètre est une Map, utiliser createRequestMap
                if (Map.class.isAssignableFrom(paramType)) {
                    Map<String, Object> requestMap = createRequestMap(request);
                    args[i] = requestMap;
                } 
                // Si c'est une liste ou un tableau
                else if (paramType.isArray() || List.class.isAssignableFrom(paramType)) {
                    args[i] = bindToCollection(paramType, request);
                }
                // Sinon, binder à un objet
                else {
                    args[i] = bindToObject(paramType, request);
                }
            }
            // Cas 2: Paramètre Map<String, Object>
            else if (Map.class.isAssignableFrom(paramType)) {
                Map<String, Object> requestMap = createRequestMap(request);
                args[i] = requestMap;
            }
            // Cas 3: Paramètre avec annotation @RequestParam
            else if (param.isAnnotationPresent(RequestParam.class)) {
                String paramName = param.getAnnotation(RequestParam.class).value();
                String value = request.getParameter(paramName);
                args[i] = ParamHandler.convert(value, paramType);
            }
            // Cas 4: Paramètre extrait de l'URL
            else if (extractedIndex < extractedParams.length) {
                args[i] = ParamHandler.convert(extractedParams[extractedIndex], paramType);
                extractedIndex++;
            }
            // Cas 5: Valeur par défaut
            else {
                args[i] = getDefaultValue(paramType);
            }
        }
        
        return args;
    }
    
    /**
     * SPRINT 8 BIS : Lie les paramètres de requête à un objet Java
     */
    private Object bindToObject(Class<?> targetClass, HttpServletRequest request) {
        try {
            Object instance = targetClass.getDeclaredConstructor().newInstance();
            
            // Pour chaque paramètre de la requête
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                
                // Utiliser la première valeur pour les propriétés simples
                String paramValue = (paramValues != null && paramValues.length > 0) ? paramValues[0] : null;
                
                // Gérer les propriétés imbriquées (ex: "user.address.street")
                if (paramName.contains(".")) {
                    bindNestedProperty(instance, paramName, paramValue);
                } else {
                    bindSimpleProperty(instance, paramName, paramValue);
                }
            }
            
            return instance;
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de l'objet " + targetClass.getName());
            e.printStackTrace();
            try {
                // Retourner une instance vide en cas d'erreur
                return targetClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    /**
     * SPRINT 8 BIS : Lie une propriété simple à l'objet
     */
    private void bindSimpleProperty(Object obj, String propertyName, String value) {
        try {
            Class<?> clazz = obj.getClass();
            String setterName = "set" + capitalize(propertyName);
            
            // Chercher le setter
            Method setter = findMethod(clazz, setterName);
            if (setter != null && setter.getParameterCount() == 1) {
                Class<?> paramType = setter.getParameterTypes()[0];
                Object convertedValue = convertValue(value, paramType);
                
                if (convertedValue != null || !paramType.isPrimitive()) {
                    setter.setAccessible(true);
                    setter.invoke(obj, convertedValue);
                }
                return;
            }
            
            // Essayer avec les champs directement
            try {
                Field field = clazz.getDeclaredField(propertyName);
                field.setAccessible(true);
                Object convertedValue = convertValue(value, field.getType());
                
                if (convertedValue != null || !field.getType().isPrimitive()) {
                    field.set(obj, convertedValue);
                }
            } catch (NoSuchFieldException e) {
                // Ignorer si le champ n'existe pas
            }
            
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de la propriété " + propertyName);
            e.printStackTrace();
        }
    }
    
    /**
     * SPRINT 8 BIS : Lie une propriété imbriquée à l'objet
     */
    private void bindNestedProperty(Object obj, String propertyPath, String value) {
        try {
            String[] parts = propertyPath.split("\\.");
            Object currentObj = obj;
            
            // Naviguer jusqu'à l'avant-dernier niveau
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Class<?> currentClass = currentObj.getClass();
                
                // Chercher le getter
                String getterName = "get" + capitalize(part);
                Method getter = findMethod(currentClass, getterName);
                
                if (getter == null) {
                    getterName = "is" + capitalize(part);
                    getter = findMethod(currentClass, getterName);
                }
                
                if (getter != null) {
                    Object nestedObj = getter.invoke(currentObj);
                    
                    // Si l'objet imbriqué est null, le créer
                    if (nestedObj == null) {
                        Class<?> nestedClass = getter.getReturnType();
                        nestedObj = nestedClass.getDeclaredConstructor().newInstance();
                        
                        // Appeler le setter pour l'affecter
                        String setterName = "set" + capitalize(part);
                        Method setter = findMethod(currentClass, setterName, nestedClass);
                        if (setter != null) {
                            setter.invoke(currentObj, nestedObj);
                        } else {
                            // Si pas de setter, essayer le champ directement
                            Field field = currentClass.getDeclaredField(part);
                            field.setAccessible(true);
                            field.set(currentObj, nestedObj);
                        }
                    }
                    
                    currentObj = nestedObj;
                } else {
                    // Essayer d'accéder directement au champ
                    try {
                        Field field = currentClass.getDeclaredField(part);
                        field.setAccessible(true);
                        Object nestedObj = field.get(currentObj);
                        
                        if (nestedObj == null) {
                            nestedObj = field.getType().getDeclaredConstructor().newInstance();
                            field.set(currentObj, nestedObj);
                        }
                        
                        currentObj = nestedObj;
                    } catch (NoSuchFieldException e) {
                        // Si le champ n'existe pas, créer un objet dynamique ?
                        System.err.println("[FrontServlet] Propriété non trouvée: " + part + " dans " + currentClass.getName());
                        return;
                    }
                }
            }
            
            // Lier la dernière propriété
            String lastPart = parts[parts.length - 1];
            bindSimpleProperty(currentObj, lastPart, value);
            
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de la propriété imbriquée: " + propertyPath);
            e.printStackTrace();
        }
    }
    
    /**
     * SPRINT 8 BIS : Convertit une valeur String vers un type cible
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            // Pour les primitives, retourner la valeur par défaut
            if (targetType.isPrimitive()) {
                if (targetType == int.class) return 0;
                else if (targetType == double.class) return 0.0;
                else if (targetType == boolean.class) return false;
                else if (targetType == long.class) return 0L;
                else if (targetType == float.class) return 0.0f;
                else if (targetType == short.class) return (short)0;
                else if (targetType == byte.class) return (byte)0;
                else if (targetType == char.class) return '\0';
            }
            return null;
        }
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == LocalDate.class) {
                return LocalDate.parse(value);
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(value);
            } else if (Enum.class.isAssignableFrom(targetType)) {
                return Enum.valueOf((Class<Enum>)targetType, value);
            }
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur de conversion: " + value + " -> " + targetType.getName());
        }
        
        return value;
    }
    
    /**
     * SPRINT 8 BIS : Lie les paramètres à une collection
     */
    private Object bindToCollection(Class<?> collectionType, HttpServletRequest request) {
        try {
            // Pour les tableaux
            if (collectionType.isArray()) {
                Class<?> componentType = collectionType.getComponentType();
                
                // Chercher les paramètres qui pourraient correspondre à un tableau
                // Par exemple, pour "hobbies[]" ou "hobbies"
                Enumeration<String> paramNames = request.getParameterNames();
                List<Object> values = new ArrayList<>();
                
                while (paramNames.hasMoreElements()) {
                    String paramName = paramNames.nextElement();
                    
                    // Si c'est le paramètre principal (sans suffixe)
                    if (paramName.equals(collectionType.getSimpleName().toLowerCase())) {
                        String[] paramValues = request.getParameterValues(paramName);
                        if (paramValues != null) {
                            for (String val : paramValues) {
                                values.add(convertValue(val, componentType));
                            }
                        }
                    }
                    // Si c'est un paramètre avec crochets (ex: "hobbies[]")
                    else if (paramName.matches(".*\\[\\d*\\]")) {
                        String baseName = paramName.replaceAll("\\[\\d*\\]", "");
                        if (baseName.equalsIgnoreCase(collectionType.getSimpleName())) {
                            String paramValue = request.getParameter(paramName);
                            values.add(convertValue(paramValue, componentType));
                        }
                    }
                }
                
                // Créer le tableau
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, values.get(i));
                }
                return array;
            }
            // Pour les List
            else if (List.class.isAssignableFrom(collectionType)) {
                List<Object> list = new ArrayList<>();
                
                // Même logique que pour les tableaux
                Enumeration<String> paramNames = request.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String paramName = paramNames.nextElement();
                    
                    // Pour simplifier, on suppose que le type élément est String
                    // Dans une version avancée, on pourrait déterminer le type générique
                    if (paramName.equals(collectionType.getSimpleName().toLowerCase())) {
                        String[] paramValues = request.getParameterValues(paramName);
                        if (paramValues != null) {
                            Collections.addAll(list, paramValues);
                        }
                    }
                }
                
                return list;
            }
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de la collection");
            e.printStackTrace();
        }
        
        // Retourner une collection vide par défaut
        if (List.class.isAssignableFrom(collectionType)) {
            return new ArrayList<>();
        }
        return null;
    }
    
    /**
     * SPRINT 8 BIS : Met en majuscule la première lettre
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * SPRINT 8 BIS : Trouve une méthode par son nom
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        
        // Chercher dans les méthodes héritées
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Method inheritedMethod = findMethod(superClass, methodName);
            if (inheritedMethod != null) {
                return inheritedMethod;
            }
        }
        
        return null;
    }
    
    /**
     * SPRINT 8 BIS : Trouve une méthode par son nom et ses paramètres
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return findMethod(clazz, methodName);
        }
    }
    
    /**
     * SPRINT 8 : Crée une Map avec tous les paramètres et attributs de la requête
     */
    private Map<String, Object> createRequestMap(HttpServletRequest request) {
        Map<String, Object> requestMap = new HashMap<>();
        
        // 1. Ajouter tous les paramètres de la requête (GET/POST)
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] values = request.getParameterValues(paramName);
            
            // Si un seul paramètre, stocker directement, sinon tableau
            if (values != null && values.length == 1) {
                // Essayer de convertir les types de base
                String value = values[0];
                requestMap.put(paramName, convertParameterValue(value));
            } else {
                requestMap.put(paramName, values);
            }
        }
        
        // 2. Ajouter tous les attributs de la requête
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            requestMap.put(attrName, request.getAttribute(attrName));
        }
        
        // 3. Ajouter les informations de la requête
        requestMap.put("request", request);
        requestMap.put("session", request.getSession(false));
        requestMap.put("contextPath", request.getContextPath());
        
        return requestMap;
    }
    
    /**
     * SPRINT 8 : Convertit une valeur de paramètre String en type approprié
     */
    private Object convertParameterValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Essayer Integer
        if (value.matches("-?\\d+")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Continuer avec d'autres types
            }
        }
        
        // Essayer Double
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Continuer avec d'autres types
            }
        }
        
        // Essayer Boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        
        // Par défaut, retourner String
        return value;
    }
    
    /**
     * SPRINT 8 : Retourne la valeur par défaut pour un type donné
     */
    private Object getDefaultValue(Class<?> type) {
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

    private String convertPathToRegex(String path) {
        return path.replaceAll("\\{[^/]+}", "([^/]+)");
    }

    private void scanControllers() {
        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            List<Class<?>> classes = getAllClasses(classesPath, "");

            List<RouteMapping> tempMappings = new ArrayList<>();

            for (Class<?> cls : classes) {
                if (!cls.isAnnotationPresent(Controller.class)) {
                    continue;
                }

                Object instance = cls.getDeclaredConstructor().newInstance();

                for (Method method : cls.getDeclaredMethods()) {

                    if (method.isAnnotationPresent(MyUrl.class)) {
                        String pattern = method.getAnnotation(MyUrl.class).value();
                        
                        String httpMethod = "GET";
                        if (method.isAnnotationPresent(HttpMethod.class)) {
                            httpMethod = method.getAnnotation(HttpMethod.class).value();
                        }
                        
                        tempMappings.add(new RouteMapping(pattern, method, instance, httpMethod));
                    }
                }
            }

            // TRIER LES ROUTES par spécificité
            tempMappings.sort((r1, r2) -> {
                String p1 = r1.getPattern();
                String p2 = r2.getPattern();
                
                int count1 = countDynamicParams(p1);
                int count2 = countDynamicParams(p2);
                
                if (count1 != count2) {
                    return Integer.compare(count1, count2);
                }
                
                return Integer.compare(p2.length(), p1.length());
            });

            mappings.addAll(tempMappings);

            System.out.println("\n========== ROUTES ENREGISTRÉES (SPRINT 8 BIS) ==========");
            for (RouteMapping rm : mappings) {
                System.out.println("[Route] " + rm.getHttpMethod() + " " + rm.getPattern() + 
                                 " -> " + rm.getMethod().getDeclaringClass().getSimpleName() + 
                                 "." + rm.getMethod().getName());
                
                // Afficher les paramètres de la méthode
                Parameter[] params = rm.getMethod().getParameters();
                if (params.length > 0) {
                    System.out.print("       Paramètres: ");
                    for (Parameter p : params) {
                        String type = p.getType().getSimpleName();
                        if (p.isAnnotationPresent(ModelAttribute.class)) {
                            String name = p.getAnnotation(ModelAttribute.class).value();
                            if (!name.isEmpty()) {
                                System.out.print("@ModelAttribute(\"" + name + "\") " + type + ", ");
                            } else {
                                System.out.print("@ModelAttribute " + type + ", ");
                            }
                        } else if (p.isAnnotationPresent(RequestParam.class)) {
                            String name = p.getAnnotation(RequestParam.class).value();
                            System.out.print("@RequestParam(\"" + name + "\") " + type + ", ");
                        } else if (Map.class.isAssignableFrom(p.getType())) {
                            System.out.print("Map<String, Object>, ");
                        } else {
                            System.out.print(type + ", ");
                        }
                    }
                    System.out.println();
                }
            }
            System.out.println("===================================================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int countDynamicParams(String pattern) {
        int count = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '{') {
                count++;
            }
        }
        return count;
    }

    private List<Class<?>> getAllClasses(String basePath, String pkg) throws Exception {

        List<Class<?>> classes = new ArrayList<>();
        File dir = new File(basePath);

        for (File file : Objects.requireNonNull(dir.listFiles())) {

            if (file.isDirectory()) {
                String subPkg = pkg.isEmpty() ? file.getName() : pkg + "." + file.getName();
                classes.addAll(getAllClasses(file.getAbsolutePath(), subPkg));
            }
            else if (file.getName().endsWith(".class")) {
                String className = pkg + "." + file.getName().replace(".class", "");

                if (pkg.isEmpty()) continue;

                try {
                    classes.add(Class.forName(className));
                } catch (Exception ignored) {}
            }
        }
        return classes;
    }
}