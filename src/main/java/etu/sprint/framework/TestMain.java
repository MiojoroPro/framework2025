package etu.sprint.framework;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import etu.sprint.framework.controller.*;
import etu.sprint.framework.annotation.MyUrl;

public class TestMain {

    public static void main(String[] args) throws Exception {
        String packageToScan = "etu.test.controller"; // le package où chercher

        List<Class<?>> classes = getClasses(packageToScan);

        Map<String, Method> urlMappings = new HashMap<>();
        Map<String, Object> controllerInstances = new HashMap<>();

        for (Class<?> cls : classes) {
            // 🔍 Vérifie si la classe a l’annotation @Controller
            if (!cls.isAnnotationPresent(Controller.class)) {
                continue;
            }

            Object instance = cls.getDeclaredConstructor().newInstance();

            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(MyUrl.class)) {
                    MyUrl myUrl = m.getAnnotation(MyUrl.class);
                    String key = myUrl.value();
                    urlMappings.put(key, m);
                    controllerInstances.put(key, instance);
                    System.out.println("Mapping ajouté : " + key + " -> " + cls.getName() + "#" + m.getName());
                }
            }
        }

        System.out.println("--- Invocation des handlers trouvés ---");
        for (String path : urlMappings.keySet()) {
            Method m = urlMappings.get(path);
            Object instance = controllerInstances.get(path);
            try {
                Object result = m.invoke(instance);
                System.out.println(path + " -> " + result);
            } catch (Exception e) {
                System.out.println("Erreur lors de l'invocation de " + path + " : ");
                e.printStackTrace(System.out);
            }
        }
    }

    // 🔁 Méthode utilitaire pour récupérer toutes les classes d’un package
    private static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            File dir = new File(resources.nextElement().toURI());
            if (!dir.exists()) continue;
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().replace(".class", "");
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }
}
