package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import etu.sprint.framework.annotation.*;
import etu.sprint.framework.controller.Controller;

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
        String httpMethod = request.getMethod(); // GET, POST, PUT, DELETE, etc.

        RouteMapping matched = null;
        String[] extractedParams = null;

        // --- MATCH ROUTE (STATIC + DYNAMIC + HTTP METHOD) ---
        for (RouteMapping rm : mappings) {

            String regex = convertPathToRegex(rm.getPattern());
            Pattern p = Pattern.compile("^" + regex + "$");
            Matcher m = p.matcher(path);

            if (m.matches() && rm.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                matched = rm;

                // Extract params
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
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            int urlParamIndex = 0;
            for (int i = 0; i < parameters.length; i++) {
                args[i] = ParamHandler.resolveParameter(
                    parameters[i], 
                    request, 
                    urlParamIndex < extractedParams.length ? extractedParams[urlParamIndex++] : null
                );
            }

            Object result = method.invoke(controller, args);

            // --- HANDLE ModelView ---
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;

                // REDIRECT ?
                if (mv.isRedirect()) {
                    response.sendRedirect(mv.getView());
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

    // --- Convert /user/{id} → /user/([^/]+) ---
    private String convertPathToRegex(String path) {
        return path.replaceAll("\\{[^/]+}", "([^/]+)");
    }


    // --- Scan all controllers and routes ---
    private void scanControllers() {
        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            List<Class<?>> classes = getAllClasses(classesPath, "");

            for (Class<?> cls : classes) {
                if (!cls.isAnnotationPresent(Controller.class)) {
                    continue;
                }

                Object instance = cls.getDeclaredConstructor().newInstance();

                for (Method method : cls.getDeclaredMethods()) {

                    String pattern = null;
                    String httpMethod = null;

                    // Check for @GetMapping
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        pattern = method.getAnnotation(GetMapping.class).value();
                        httpMethod = "GET";
                    }
                    // Check for @PostMapping
                    else if (method.isAnnotationPresent(PostMapping.class)) {
                        pattern = method.getAnnotation(PostMapping.class).value();
                        httpMethod = "POST";
                    }
                    // Check for @PutMapping
                    else if (method.isAnnotationPresent(PutMapping.class)) {
                        pattern = method.getAnnotation(PutMapping.class).value();
                        httpMethod = "PUT";
                    }
                    // Check for @DeleteMapping
                    else if (method.isAnnotationPresent(DeleteMapping.class)) {
                        pattern = method.getAnnotation(DeleteMapping.class).value();
                        httpMethod = "DELETE";
                    }
                    // Check for @RequestMapping
                    else if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping rm = method.getAnnotation(RequestMapping.class);
                        pattern = rm.value();
                        httpMethod = rm.method();
                    }
                    // Backward compatibility with @MyUrl (defaults to GET)
                    else if (method.isAnnotationPresent(MyUrl.class)) {
                        pattern = method.getAnnotation(MyUrl.class).value();
                        httpMethod = "GET";
                    }

                    if (pattern != null && httpMethod != null) {
                        mappings.add(new RouteMapping(pattern, method, instance, httpMethod));
                        System.out.println("[Route] " + httpMethod + " " + pattern + " -> " + cls.getName() + "." + method.getName());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // --- Load all .class files recursively ---
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