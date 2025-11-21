package etu.sprint.framework;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

public class RouteMapping {

    private String pattern;
    private Method method;
    private Object controller;

    private Pattern regexPattern;
    private List<String> variableNames = new ArrayList<>();

    public RouteMapping(String pattern, Method method, Object controller) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;

        compileRoute(pattern);
    }

    private void compileRoute(String route) {

        Matcher m = Pattern.compile("\\{([^/]+)}").matcher(route);
        while (m.find()) {
            variableNames.add(m.group(1));
        }

        String regex = route.replaceAll("\\{[^/]+}", "([^/]+)");
        this.regexPattern = Pattern.compile("^" + regex + "$");
    }

    public Pattern getRegexPattern() {
        return regexPattern;
    }

    public List<String> getVariableNames() {
        return variableNames;
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
