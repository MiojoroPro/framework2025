package etu.sprint.framework;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import etu.sprint.framework.annotation.*;
public class FrontServlet extends HttpServlet {

    // Nouveaux ajouts
    private Map<String, Method> urlMappings = new HashMap<>();
    private Map<String, Object> controllerInstances = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String packageToScan = "etu.test.controller";

            List<Class<?>> classes = getClasses(packageToScan);

            for (Class<?> cls : classes) {
                Object instance = cls.getDeclaredConstructor().newInstance();

                for (Method m : cls.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(MyUrl.class)) {
                        MyUrl myUrl = m.getAnnotation(MyUrl.class);
                        String key = myUrl.value();
                        urlMappings.put(key, m);
                        controllerInstances.put(key, instance);
                        System.out.println("Mapping ajouté : " + key + " -> " + m.getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        String realPath = getServletContext().getRealPath(path);
        File file = new File(realPath);

        if (file.exists() && file.isFile()) {
            ServletContext context = getServletContext();
            String mime = context.getMimeType(realPath);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            response.setContentType(mime);

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return;
        }

        Method m = urlMappings.get(path);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (m != null) {
            try {
                Object controller = controllerInstances.get(path);
                Object result = m.invoke(controller);

                out.println("<html><body>");
                out.println("<h2>Résultat du contrôleur :</h2>");
                out.println("<p>" + result + "</p>");
                out.println("</body></html>");

            } catch (Exception e) {
                e.printStackTrace(out);
            }
        } else {
            out.println("<html><body>");
            out.println("<h1>Page non trouvée</h1>");
            out.println("<p>Vous avez demandé : " + path + "</p>");
            out.println("</body></html>");
        }
    }

    private List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            File dir = new File(resources.nextElement().toURI());
            if (!dir.exists()) continue;
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().replace(".class", "");
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }
}
