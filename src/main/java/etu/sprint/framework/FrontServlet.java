// File name: FrontServlet.java (Sprint 10 - Support Upload Fichier)
package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.MultipartConfig;

import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.annotation.ModelAttribute;
import etu.sprint.framework.annotation.JSON;
import etu.sprint.framework.annotation.FileParam;
import etu.sprint.framework.controller.Controller;

/**
 * FrontServlet : Le contrôleur frontal du framework
 * VERSION SPRINT 10 : Support des API REST + Upload de fichiers
 */
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB max par fichier
    maxRequestSize = 1024 * 1024 * 50,   // 50MB max par requête
    fileSizeThreshold = 1024 * 1024      // 1MB avant écriture sur disque
)
public class FrontServlet extends HttpServlet {

    private List<RouteMapping> mappings = new ArrayList<>();
    private boolean isScanned = false;
    
    // Configuration pour l'upload
    private String uploadTempDir;

    @Override
    public void init() throws ServletException {
        super.init();
        
        // Créer un répertoire temporaire pour l'upload
        uploadTempDir = getServletContext().getRealPath("/WEB-INF/temp-uploads");
        File tempDir = new File(uploadTempDir);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        System.out.println("[FrontServlet] Initialisation OK - Sprint 10 avec Upload Fichier");
        System.out.println("[FrontServlet] Répertoire temporaire upload: " + uploadTempDir);
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

        // --- VÉRIFIER SI C'EST UN UPLOAD DE FICHIER ---
        Map<String, Object> multipartData = null;
        if (isMultipartRequest(request)) {
            try {
                multipartData = parseMultipartRequest(request);
                System.out.println("[FrontServlet] Requête multipart détectée, fichiers: " + 
                                 ((Map<?, ?>) multipartData.get("files")).size());
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                                 "Erreur lors du traitement du fichier: " + e.getMessage());
                return;
            }
        }

        // --- EXECUTE CONTROLLER METHOD ---
        try {
            Method method = matched.getMethod();
            Object controller = matched.getController();

            // Construction des arguments de la méthode
            Object[] args = buildMethodArguments(method, extractedParams, request, multipartData);

            Object result = method.invoke(controller, args);

            // --- SPRINT 9: VÉRIFIER SI C'EST UNE API JSON ---
            if (method.isAnnotationPresent(JSON.class)) {
                handleJsonResponse(method, result, response);
                return;
            }

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
     * SPRINT 10 : Vérifie si la requête contient des fichiers
     */
    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    /**
     * SPRINT 10 : Parse une requête multipart et extrait fichiers + paramètres
     */
    private Map<String, Object> parseMultipartRequest(HttpServletRequest request) 
            throws ServletException, IOException {
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String[]> parameters = new HashMap<>();
        Map<String, byte[]> files = new HashMap<>();
        Map<String, String> fileNames = new HashMap<>();
        Map<String, String> fileContentTypes = new HashMap<>();
        Map<String, Long> fileSizes = new HashMap<>();
        
        try {
            // Utiliser Servlet 3.0+ Part API
            Collection<Part> parts = request.getParts();
            
            for (Part part : parts) {
                String fieldName = part.getName();
                String fileName = part.getSubmittedFileName();
                
                if (fileName != null && !fileName.isEmpty()) {
                    // C'est un fichier
                    InputStream inputStream = part.getInputStream();
                    byte[] fileBytes = readAllBytes(inputStream);
                    
                    files.put(fieldName, fileBytes);
                    fileNames.put(fieldName, fileName);
                    fileContentTypes.put(fieldName, part.getContentType());
                    fileSizes.put(fieldName, part.getSize());
                    
                    System.out.println("[FrontServlet] Fichier reçu: " + fileName + 
                                     " (" + fileBytes.length + " bytes, " + 
                                     part.getContentType() + ")");
                    
                    // Nettoyer le fichier temporaire
                    part.delete();
                    
                } else {
                    // C'est un paramètre normal
                    InputStream inputStream = part.getInputStream();
                    String value = new String(readAllBytes(inputStream), 
                                            request.getCharacterEncoding());
                    parameters.put(fieldName, new String[]{value});
                }
            }
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du parsing multipart: " + e.getMessage());
            
            // Fallback: traiter comme une requête normale
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name = paramNames.nextElement();
                parameters.put(name, request.getParameterValues(name));
            }
        }
        
        result.put("parameters", parameters);
        result.put("files", files);
        result.put("fileNames", fileNames);
        result.put("fileContentTypes", fileContentTypes);
        result.put("fileSizes", fileSizes);
        
        return result;
    }
    
    /**
     * SPRINT 10 : Lit tous les bytes d'un InputStream
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * SPRINT 9 : Gère les réponses JSON
     */
    private void handleJsonResponse(Method method, Object result, HttpServletResponse response) 
            throws IOException {
        
        JSON jsonAnnotation = method.getAnnotation(JSON.class);
        
        // Définir le statut HTTP
        response.setStatus(jsonAnnotation.status());
        
        // Définir le Content-Type
        response.setContentType(jsonAnnotation.contentType() + "; charset=UTF-8");
        
        // Sérialiser le résultat en JSON
        String jsonResult;
        if (result == null) {
            jsonResult = "null";
        } else if (result instanceof String) {
            // Si c'est déjà une chaîne, vérifier si c'est déjà du JSON
            String strResult = (String) result;
            if (strResult.trim().startsWith("{") || strResult.trim().startsWith("[")) {
                jsonResult = strResult;
            } else {
                jsonResult = "\"" + escapeJsonString(strResult) + "\"";
            }
        } else {
            // Sérialiser l'objet
            jsonResult = JsonSerializer.toJson(result);
        }
        
        // Écrire la réponse
        PrintWriter out = response.getWriter();
        out.print(jsonResult);
        out.flush();
        
        // Log pour le débogage
        System.out.println("[JSON API] Retour JSON: " + 
                          method.getDeclaringClass().getSimpleName() + "." + 
                          method.getName() + " -> " + 
                          (jsonResult.length() > 100 ? jsonResult.substring(0, 100) + "..." : jsonResult));
    }
    
    /**
     * SPRINT 9 : Échappe une chaîne pour JSON
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    /**
     * SPRINT 10 : Construit les arguments pour une méthode de contrôleur avec support fichiers
     */
    private Object[] buildMethodArguments(
            Method method, 
            String[] extractedParams, 
            HttpServletRequest request,
            Map<String, Object> multipartData) {
        
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        int extractedIndex = 0;
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // Cas 1: @FileParam annotation (SPRINT 10)
            if (param.isAnnotationPresent(FileParam.class)) {
                String paramName = param.getAnnotation(FileParam.class).value();
                
                if (multipartData != null) {
                    Map<String, byte[]> files = (Map<String, byte[]>) multipartData.get("files");
                    if (files != null && files.containsKey(paramName)) {
                        args[i] = files.get(paramName);
                        continue;
                    }
                }
                args[i] = getDefaultValue(paramType);
            }
            
            // Cas 2: @ModelAttribute annotation avec fichiers (SPRINT 10)
            else if (param.isAnnotationPresent(ModelAttribute.class)) {
                // Si le paramètre est une Map, utiliser createRequestMap étendu
                if (Map.class.isAssignableFrom(paramType)) {
                    Map<String, Object> requestMap = createExtendedRequestMap(request, multipartData);
                    args[i] = requestMap;
                } 
                // Si c'est une liste ou un tableau
                else if (paramType.isArray() || List.class.isAssignableFrom(paramType)) {
                    args[i] = bindToCollection(paramType, request, multipartData);
                }
                // Sinon, binder à un objet avec support fichiers
                else {
                    args[i] = bindToObjectWithFiles(paramType, request, multipartData);
                }
            }
            
            // Cas 3: Paramètre Map<String, Object> (reçoit tout)
            else if (Map.class.isAssignableFrom(paramType)) {
                Map<String, Object> requestMap = createExtendedRequestMap(request, multipartData);
                args[i] = requestMap;
            }
            
            // Cas 4: Paramètre avec annotation @RequestParam
            else if (param.isAnnotationPresent(RequestParam.class)) {
                String paramName = param.getAnnotation(RequestParam.class).value();
                String value = null;
                
                // Chercher d'abord dans les paramètres multipart
                if (multipartData != null) {
                    Map<String, String[]> multipartParams = 
                        (Map<String, String[]>) multipartData.get("parameters");
                    if (multipartParams != null && multipartParams.containsKey(paramName)) {
                        String[] values = multipartParams.get(paramName);
                        value = values != null && values.length > 0 ? values[0] : null;
                    }
                }
                
                // Fallback sur request.getParameter()
                if (value == null) {
                    value = request.getParameter(paramName);
                }
                
                args[i] = ParamHandler.convert(value, paramType);
            }
            
            // Cas 5: Paramètre extrait de l'URL
            else if (extractedIndex < extractedParams.length) {
                args[i] = ParamHandler.convert(extractedParams[extractedIndex], paramType);
                extractedIndex++;
            }
            
            // Cas 6: Valeur par défaut
            else {
                args[i] = getDefaultValue(paramType);
            }
        }
        
        return args;
    }
    
    /**
     * SPRINT 10 : Crée une Map étendue avec paramètres + fichiers
     */
    private Map<String, Object> createExtendedRequestMap(
            HttpServletRequest request, 
            Map<String, Object> multipartData) {
        
        Map<String, Object> requestMap = createRequestMap(request);
        
        // Ajouter les données multipart si présentes
        if (multipartData != null) {
            requestMap.putAll(multipartData);
            
            // Ajouter un flag pour indiquer qu'il y a des fichiers
            requestMap.put("hasFiles", true);
            
            // Ajouter un accès facile aux fichiers
            Map<String, byte[]> files = (Map<String, byte[]>) multipartData.get("files");
            if (files != null && !files.isEmpty()) {
                requestMap.put("uploadedFiles", files.keySet());
            }
        } else {
            requestMap.put("hasFiles", false);
        }
        
        return requestMap;
    }
    
    /**
     * SPRINT 10 : Lie les paramètres de requête à un objet avec support fichiers
     */
    private Object bindToObjectWithFiles(
            Class<?> targetClass, 
            HttpServletRequest request, 
            Map<String, Object> multipartData) {
        
        try {
            Object instance = targetClass.getDeclaredConstructor().newInstance();
            
            // Pour chaque paramètre de la requête (normaux)
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
            
            // Traiter les paramètres multipart (s'il y en a)
            if (multipartData != null) {
                Map<String, String[]> multipartParams = 
                    (Map<String, String[]>) multipartData.get("parameters");
                
                if (multipartParams != null) {
                    for (Map.Entry<String, String[]> entry : multipartParams.entrySet()) {
                        String paramName = entry.getKey();
                        String[] values = entry.getValue();
                        String paramValue = values != null && values.length > 0 ? values[0] : null;
                        
                        if (paramName.contains(".")) {
                            bindNestedProperty(instance, paramName, paramValue);
                        } else {
                            bindSimpleProperty(instance, paramName, paramValue);
                        }
                    }
                }
                
                // Gérer les fichiers (SPRINT 10)
                Map<String, byte[]> files = (Map<String, byte[]>) multipartData.get("files");
                if (files != null) {
                    for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                        String fieldName = entry.getKey();
                        byte[] fileBytes = entry.getValue();
                        
                        // Essayer de trouver un setter pour le fichier
                        String setterName = "set" + capitalize(fieldName);
                        Method setter = findMethod(targetClass, setterName, byte[].class);
                        
                        if (setter != null) {
                            System.out.println("[FrontServlet] Appel setter " + setterName + 
                                             " avec " + fileBytes.length + " bytes");
                            setter.invoke(instance, fileBytes);
                        } else {
                            // Si pas de setter spécifique, essayer de stocker dans un champ
                            try {
                                Field field = targetClass.getDeclaredField(fieldName);
                                if (field.getType() == byte[].class) {
                                    field.setAccessible(true);
                                    field.set(instance, fileBytes);
                                }
                            } catch (NoSuchFieldException e) {
                                // Ignorer si pas de champ correspondant
                            }
                        }
                    }
                }
            }
            
            return instance;
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de l'objet avec fichiers " + 
                             targetClass.getName());
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
                        System.err.println("[FrontServlet] Propriété non trouvée: " + part + 
                                         " dans " + currentClass.getName());
                        return;
                    }
                }
            }
            
            // Lier la dernière propriété
            String lastPart = parts[parts.length - 1];
            bindSimpleProperty(currentObj, lastPart, value);
            
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du binding de la propriété imbriquée: " + 
                             propertyPath);
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
            System.err.println("[FrontServlet] Erreur de conversion: " + value + " -> " + 
                             targetType.getName());
        }
        
        return value;
    }
    
    /**
     * SPRINT 8 BIS : Lie les paramètres à une collection
     */
    private Object bindToCollection(
            Class<?> collectionType, 
            HttpServletRequest request, 
            Map<String, Object> multipartData) {
        
        try {
            // Pour les tableaux
            if (collectionType.isArray()) {
                Class<?> componentType = collectionType.getComponentType();
                
                // Chercher les paramètres qui pourraient correspondre à un tableau
                List<Object> values = new ArrayList<>();
                
                // Chercher dans les paramètres normaux
                Enumeration<String> paramNames = request.getParameterNames();
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
                
                // Chercher dans les paramètres multipart
                if (multipartData != null) {
                    Map<String, String[]> multipartParams = 
                        (Map<String, String[]>) multipartData.get("parameters");
                    
                    if (multipartParams != null) {
                        for (Map.Entry<String, String[]> entry : multipartParams.entrySet()) {
                            String paramName = entry.getKey();
                            String[] paramValues = entry.getValue();
                            
                            if (paramName.equals(collectionType.getSimpleName().toLowerCase())) {
                                if (paramValues != null) {
                                    for (String val : paramValues) {
                                        values.add(convertValue(val, componentType));
                                    }
                                }
                            }
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
                
                // Ajouter depuis multipart
                if (multipartData != null) {
                    Map<String, String[]> multipartParams = 
                        (Map<String, String[]>) multipartData.get("parameters");
                    
                    if (multipartParams != null) {
                        for (Map.Entry<String, String[]> entry : multipartParams.entrySet()) {
                            String paramName = entry.getKey();
                            if (paramName.equals(collectionType.getSimpleName().toLowerCase())) {
                                String[] paramValues = entry.getValue();
                                if (paramValues != null) {
                                    Collections.addAll(list, paramValues);
                                }
                            }
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
     * Crée une Map avec tous les paramètres et attributs de la requête
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
     * Convertit une valeur de paramètre String en type approprié
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
     * Retourne la valeur par défaut pour un type donné
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

            System.out.println("\n========== ROUTES ENREGISTRÉES (SPRINT 10 - Upload Fichier) ==========");
            for (RouteMapping rm : mappings) {
                System.out.println("[Route] " + rm.getHttpMethod() + " " + rm.getPattern() + 
                                 " -> " + rm.getMethod().getDeclaringClass().getSimpleName() + 
                                 "." + rm.getMethod().getName());
                
                // Indiquer si c'est une API JSON
                if (rm.getMethod().isAnnotationPresent(JSON.class)) {
                    JSON json = rm.getMethod().getAnnotation(JSON.class);
                    System.out.println("       [API REST] Statut: " + json.status() + 
                                     ", Content-Type: " + json.contentType());
                }
                
                // Afficher les paramètres de la méthode
                Parameter[] params = rm.getMethod().getParameters();
                if (params.length > 0) {
                    System.out.print("       Paramètres: ");
                    for (Parameter p : params) {
                        String type = p.getType().getSimpleName();
                        
                        // SPRINT 10: Afficher @FileParam
                        if (p.isAnnotationPresent(FileParam.class)) {
                            String name = p.getAnnotation(FileParam.class).value();
                            System.out.print("@FileParam(\"" + name + "\") " + type + ", ");
                        }
                        else if (p.isAnnotationPresent(ModelAttribute.class)) {
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
            System.out.println("=============================================================\n");

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
    
    @Override
    public void destroy() {
        // Nettoyer le répertoire temporaire
        try {
            File tempDir = new File(uploadTempDir);
            if (tempDir.exists()) {
                for (File file : tempDir.listFiles()) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FrontServlet] Erreur lors du nettoyage du répertoire temporaire");
        }
        
        super.destroy();
    }
}