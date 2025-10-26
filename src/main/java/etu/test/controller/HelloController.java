package etu.test.controller;

import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.controller.Controller;

@Controller
public class HelloController {

    @MyUrl("/hello")
    public String sayHello() {
        return "Salut depuis ton framework !";
    }

    @MyUrl("/test")
    public String doTest() {
        return "Méthode test exécutée !";
    }

    public String notMappedMethod() {
        return "Cette méthode n'est pas mappée à une URL.";
    }
}
