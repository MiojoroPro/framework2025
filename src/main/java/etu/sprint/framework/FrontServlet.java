package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import etu.sprint.framework.annotation.*;
import etu.sprint.framework.controller.Controller;

public class FrontServlet extends HttpServlet {

    private Map<String, Method> urlMappings = new HashMap<>();
    private Map<String, Object> controllerInstances = new HashMap<>();
    private boolean isScanned = false;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("[FrontServlet] Initialisation terminée, le scan se fera à la première requête.");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (!isScanned) {
            synchronized (this) {
                if (!isScanned) {
                    System.out.println("[FrontServlet] Premier accès : scan des classes...");
                    try {
                        String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
                        List<Class<?>> classes = getAllClasses(classesPath, "");
                        scanAndRegisterControllers(classes);
                        isScanned = true;
                        System.out.println("[FrontServlet] Scan terminé avec succès !");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Method m = urlMappings.get(path);
        if (m != null) {
            try {
                Object controller = controllerInstances.get(path);
                Class<?> controllerClass = controller.getClass();

                if (controllerClass.isAnnotationPresent(Controller.class)) {
                    m.setAccessible(true);
                    Object result = m.invoke(controller);

                    out.println("<html><body>");
                    out.println("<h2>Résultat du contrôleur :</h2>");
                    out.println("<p><strong>Contrôleur :</strong> " + controllerClass.getSimpleName() + "</p>");
                    out.println("<p><strong>Méthode :</strong> " + m.getName() + "</p>");
                    out.println("<p><strong>Résultat :</strong> " + result + "</p>");
                    out.println("</body></html>");
                } else {
                    // Si la classe n'est pas annotée @Controller, on ne fait rien
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page non trouvée");
                }

            } catch (Exception e) {
                e.printStackTrace(out);
            }
        } else {
            response.setContentType("text/html; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            out.println("<html><body>");
            out.println("<h1>Page non trouvée</h1>");
            out.println("<p>Vous avez demandé : " + path + "</p>");
            out.println("</body></html>");
        }
    }

    private void scanAndRegisterControllers(List<Class<?>> classes) throws Exception {
        for (Class<?> cls : classes) {
            if (!cls.isAnnotationPresent(Controller.class)) continue; // On ne prend que les contrôleurs

            Object instance = cls.getDeclaredConstructor().newInstance();

            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(MyUrl.class)) {
                    MyUrl myUrl = m.getAnnotation(MyUrl.class);
                    String key = myUrl.value();
                    urlMappings.put(key, m);
                    controllerInstances.put(key, instance);
                    System.out.println("Mapping ajouté : " + key + " -> " + m.getName() + " (" + cls.getName() + ")");
                }
            }
        }
    }

    private List<Class<?>> getAllClasses(String basePath, String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        File baseDir = new File(basePath);

        for (File file : Objects.requireNonNull(baseDir.listFiles())) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                classes.addAll(getAllClasses(file.getAbsolutePath(), subPackage));
            } else if (file.getName().endsWith(".class")) {
                String className = (packageName.isEmpty() ? "" : packageName + ".") + file.getName().replace(".class", "");
                try {
                    classes.add(Class.forName(className));
                    System.out.println("Classe trouvée : " + className);
                } catch (ClassNotFoundException e) {
                    System.err.println("Classe non trouvée : " + className);
                }
            }
        }
        return classes;
    }
}
