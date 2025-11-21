package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.controller.Controller;

public class FrontServlet extends HttpServlet {

    private List<RouteMapping> mappings = new ArrayList<>();
    private boolean isScanned = false;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("[FrontServlet] Sprint 6 + 6bis chargé !");
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

        RouteMapping matched = null;
        Map<String, String> pathVariables = new HashMap<>();

        // ---------------------------
        // MATCHING ROUTES + VARIABLES
        // ---------------------------
        for (RouteMapping rm : mappings) {

            Matcher matcher = rm.getRegexPattern().matcher(path);

            if (matcher.matches()) {
                matched = rm;

                // extraire les variables {name}
                List<String> varNames = rm.getVariableNames();

                for (int i = 0; i < varNames.size(); i++) {
                    pathVariables.put(varNames.get(i), matcher.group(i + 1));
                }
                break;
            }
        }

        PrintWriter out = response.getWriter();

        if (matched == null) {
            out.println("<h1>404 - Aucun mapping pour " + path + "</h1>");
            return;
        }

        // ---------------------------
        // PREPARATION DES ARGUMENTS
        // ---------------------------

        Object controller = matched.getController();
        Method method = matched.getMethod();
        Parameter[] params = method.getParameters();

        Object[] args = new Object[params.length];

        try {

            for (int i = 0; i < params.length; i++) {

                Parameter p = params[i];
                Object value = null;

                // 1. Priorité @RequestParam
                if (p.isAnnotationPresent(RequestParam.class)) {

                    String paramName = p.getAnnotation(RequestParam.class).value();
                    String raw = request.getParameter(paramName);

                    if (raw == null)
                        throw new RuntimeException("Paramètre manquant : " + paramName);

                    value = convert(raw, p.getType());
                }

                // 2. Sinon, variable de route {name}
                else if (pathVariables.containsKey(p.getName())) {
                    String raw = pathVariables.get(p.getName());
                    value = convert(raw, p.getType());
                }

                // 3. Aucun paramètre correspondant
                else {
                    throw new RuntimeException("Impossible de binder l'argument : " + p.getName());
                }

                args[i] = value;
            }

            // ---------------------------
            // INVOKE METHODE
            // ---------------------------

            Object result = method.invoke(controller, args);

            // ---------------------------
            // GESTION MODELVIEW
            // ---------------------------
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;

                if (mv.isRedirect()) {
                    response.sendRedirect(mv.getView());
                    return;
                }

                // injecter les données dans la request
                for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                // forward vers JSP
                request.getRequestDispatcher("/WEB-INF/views/" + mv.getView())
                        .forward(request, response);
                return;
            }

            // Si la méthode renvoie une String simple
            out.println("<h3>Returned: " + result + "</h3>");

        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    // ---------------------------
    // FONCTION DE CONVERSION
    // ---------------------------
    private Object convert(String v, Class<?> type) {
        if (type == int.class || type == Integer.class) return Integer.parseInt(v);
        if (type == double.class || type == Double.class) return Double.parseDouble(v);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(v);
        return v;
    }

    // ---------------------------
    // SCAN DES CONTROLLERS
    // ---------------------------
    private void scanControllers() {
        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            List<Class<?>> classes = getAllClasses(classesPath, "");

            for (Class<?> cls : classes) {
                if (!cls.isAnnotationPresent(Controller.class)) continue;

                Object instance = cls.getDeclaredConstructor().newInstance();

                for (Method method : cls.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(MyUrl.class)) {

                        String route = method.getAnnotation(MyUrl.class).value();

                        RouteMapping rm = new RouteMapping(route, method, instance);
                        mappings.add(rm);

                        System.out.println("[Route] " + route + " -> " + cls.getName() + "." + method.getName());
                    }
                }
            }

        } catch (Exception ignored) {}
    }

    // ---------------------------
    // CHARGEMENT DES CLASSES
    // ---------------------------
    private List<Class<?>> getAllClasses(String basePath, String pkg) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        File dir = new File(basePath);

        for (File file : Objects.requireNonNull(dir.listFiles())) {

            if (file.isDirectory()) {
                String subPkg = pkg.isEmpty() ? file.getName() : pkg + "." + file.getName();
                classes.addAll(getAllClasses(file.getAbsolutePath(), subPkg));
            }
            else if (file.getName().endsWith(".class") && !pkg.isEmpty()) {
                String className = pkg + "." + file.getName().replace(".class", "");
                try { classes.add(Class.forName(className)); }
                catch (Exception ignored) {}
            }
        }
        return classes;
    }
}
