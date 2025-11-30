package etu.sprint.framework;

import java.lang.reflect.Method;

/**
 * Classe qui représente une route (mapping URL -> Méthode Controller)
 * 
 * Une RouteMapping contient toutes les informations nécessaires pour :
 * 1. Identifier une route (pattern + méthode HTTP)
 * 2. Exécuter le bon code (méthode du controller)
 * 
 * Exemple :
 * Pattern = "/user/{id}"
 * HttpMethod = "GET"
 * Method = getUserById()
 * Controller = instance de UserController
 */
public class RouteMapping {

    // Le pattern de l'URL (ex: "/user/{id}", "/product/list")
    private String pattern;
    
    // La méthode Java à invoquer (ex: getUserById, createProduct)
    private Method method;
    
    // L'instance du controller qui contient la méthode (ex: new UserController())
    private Object controller;
    
    // La méthode HTTP requise (ex: "GET", "POST", "PUT", "DELETE")
    private String httpMethod;

    /**
     * Constructeur
     * 
     * @param pattern Le pattern de l'URL (ex: "/user/{id}")
     * @param method La méthode Java à invoquer
     * @param controller L'instance du controller
     * @param httpMethod La méthode HTTP (GET, POST, etc.)
     */
    public RouteMapping(String pattern, Method method, Object controller, String httpMethod) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        this.httpMethod = httpMethod;
    }

    // ========== GETTERS ==========

    /**
     * Retourne le pattern de l'URL
     * Ex: "/user/{id}"
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Retourne la méthode Java à invoquer
     * Ex: Method représentant getUserById()
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Retourne l'instance du controller
     * Ex: instance de UserController
     */
    public Object getController() {
        return controller;
    }

    /**
     * Retourne la méthode HTTP requise
     * Ex: "GET", "POST"
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    // ========== MÉTHODE DE VÉRIFICATION ==========

    /**
     * Vérifie si cette route correspond à la méthode HTTP de la requête
     * 
     * Exemple :
     * Si cette route est configurée pour "POST" et que requestMethod = "POST" → true
     * Si cette route est configurée pour "POST" et que requestMethod = "GET" → false
     * 
     * La comparaison est insensible à la casse (POST = post = Post)
     * 
     * @param requestMethod La méthode HTTP de la requête entrante (ex: "GET", "POST")
     * @return true si la méthode correspond, false sinon
     */
    public boolean matchesHttpMethod(String requestMethod) {
        return this.httpMethod.equalsIgnoreCase(requestMethod);
    }
}