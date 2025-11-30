package etu.sprint.framework;

import java.lang.reflect.Method;

public class RouteMapping {

    private String pattern;     // ex : /zavatra/{v}
    private Method method;
    private Object controller;
    private String httpMethod;  // GET, POST, PUT, DELETE, etc.

    public RouteMapping(String pattern, Method method, Object controller, String httpMethod) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        this.httpMethod = httpMethod;
    }

    public String getPattern() {
        return pattern;
    }

    public Method getMethod() {
        return method;
    }

    public Object getController() {
        return controller;
    }

    public String getHttpMethod() {
        return httpMethod;
    }
}