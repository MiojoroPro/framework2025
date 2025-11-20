package etu.sprint.framework;

import java.lang.reflect.Method;

public class RouteMapping {

    private String pattern;     // ex : /zavatra/{v}
    private Method method;
    private Object controller;

    public RouteMapping(String pattern, Method method, Object controller) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
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
}
