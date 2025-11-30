package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.controller.Controller;

/**
 * FrontServlet : Le contrôleur frontal du framework
 * VERSION CORRIGÉE : Gère l'ordre des routes ET les redirections avec context path
 */
public class FrontServlet extends HttpServlet {

    private List<RouteMapping> mappings = new ArrayList<>();
    private boolean isScanned = false;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("[FrontServlet] Initialisation OK");
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

            // Build method arguments
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < args.length; i++) {
                if (i < extractedParams.length && extractedParams[i] != null) {
                    if (paramTypes[i] == int.class || paramTypes[i] == Integer.class) {
                        args[i] = Integer.parseInt(extractedParams[i]);
                    } else {
                        args[i] = extractedParams[i];
                    }
                } else {
                    if (paramTypes[i] == int.class) {
                        args[i] = 0;
                    } else if (paramTypes[i] == Integer.class) {
                        args[i] = null;
                    } else {
                        args[i] = null;
                    }
                }
            }

            Object result = method.invoke(controller, args);

            // --- HANDLE ModelView ---
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;

                // REDIRECT ?
                if (mv.isRedirect()) {
                    String redirectUrl = mv.getView();
                    
                    // ========================================
                    // *** CORRECTION : GESTION DU CONTEXT PATH ***
                    // ========================================
                    
                    // Si l'URL commence par "/" (chemin absolu dans l'app)
                    // ET ne contient pas déjà le context path
                    // → Ajouter le context path automatiquement
                    if (redirectUrl.startsWith("/") && !redirectUrl.startsWith(ctx)) {
                        redirectUrl = ctx + redirectUrl;
                        System.out.println("[Redirect] " + mv.getView() + " → " + redirectUrl);
                    }
                    // Si l'URL ne commence pas par "/" (URL relative ou complète)
                    // → Laisser tel quel
                    
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

            System.out.println("\n========== ROUTES ENREGISTRÉES ==========");
            for (RouteMapping rm : mappings) {
                System.out.println("[Route] " + rm.getHttpMethod() + " " + rm.getPattern() + 
                                 " -> " + rm.getMethod().getDeclaringClass().getSimpleName() + 
                                 "." + rm.getMethod().getName());
            }
            System.out.println("=========================================\n");

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

/*
 * ========================================
 * EXPLICATION DU PROBLÈME DE REDIRECTION
 * ========================================
 * 
 * PROBLÈME :
 * Dans le controller :
 *   mv.setView("/user/123");
 *   mv.setRedirect(true);
 * 
 * Le framework fait :
 *   response.sendRedirect("/user/123");
 * 
 * → Redirige vers http://localhost:8989/user/123
 * 
 * MAIS on voulait :
 * → http://localhost:8989/Framework-Test/user/123
 * 
 * CAUSE :
 * response.sendRedirect() avec un chemin commençant par "/"
 * est relatif à la RACINE DU SERVEUR, pas à l'application !
 * 
 * SOLUTION :
 * Ajouter automatiquement le context path (/Framework-Test)
 * avant de faire la redirection.
 * 
 * EXEMPLES :
 * 
 * Dans le controller :        Framework fait :
 * mv.setView("/user/123")  →  redirect vers /Framework-Test/user/123 ✅
 * mv.setView("/users")     →  redirect vers /Framework-Test/users ✅
 * mv.setView("users")      →  redirect vers users (relatif) ⚠️
 * mv.setView("http://...")  →  redirect vers http://... (externe) ✅
 * 
 * ========================================
 * TYPES DE REDIRECTION SUPPORTÉS
 * ========================================
 * 
 * 1. REDIRECTION INTERNE (chemin absolu dans l'app) :
 *    mv.setView("/user/123");
 *    → /Framework-Test/user/123
 * 
 * 2. REDIRECTION RELATIVE :
 *    mv.setView("success");
 *    → Relatif à l'URL courante (pas recommandé)
 * 
 * 3. REDIRECTION EXTERNE :
 *    mv.setView("http://google.com");
 *    → Vers un autre site
 * 
 * RECOMMANDATION :
 * Toujours utiliser des chemins absolus commençant par "/"
 * Le framework ajoutera automatiquement le context path !
 */