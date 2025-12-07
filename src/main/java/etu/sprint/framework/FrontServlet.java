package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.controller.Controller;

/**
 * FrontServlet : Le contrôleur frontal du framework
 * VERSION SPRINT 8 : Support du paramètre Map<String, Object> dans les méthodes de contrôleur
 */
public class FrontServlet extends HttpServlet {

    private List<RouteMapping> mappings = new ArrayList<>();
    private boolean isScanned = false;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("[FrontServlet] Initialisation OK - Sprint 8 avec support Map");
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

            // Construction des arguments de la méthode (SPRINT 8)
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
     * SPRINT 8 : Construit les arguments pour une méthode de contrôleur
     * Gère les paramètres Map<String, Object>, @RequestParam et paramètres d'URL
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
            
            // Cas 1: Paramètre Map<String, Object>
            if (Map.class.isAssignableFrom(paramType)) {
                Map<String, Object> requestMap = createRequestMap(request);
                args[i] = requestMap;
            }
            // Cas 2: Paramètre avec annotation @RequestParam
            else if (param.isAnnotationPresent(RequestParam.class)) {
                String paramName = param.getAnnotation(RequestParam.class).value();
                String value = request.getParameter(paramName);
                args[i] = ParamHandler.convert(value, paramType);
            }
            // Cas 3: Paramètre extrait de l'URL
            else if (extractedIndex < extractedParams.length) {
                args[i] = ParamHandler.convert(extractedParams[extractedIndex], paramType);
                extractedIndex++;
            }
            // Cas 4: Valeur par défaut
            else {
                args[i] = getDefaultValue(paramType);
            }
        }
        
        return args;
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

            System.out.println("\n========== ROUTES ENREGISTRÉES (SPRINT 8) ==========");
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
                        if (p.isAnnotationPresent(RequestParam.class)) {
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