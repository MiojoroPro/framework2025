package etu.test.controller;

import etu.sprint.framework.annotation.MyUrl;

public class HelloController {

    @MyUrl("/hello")
    public String sayHello() {
        return "Salut depuis ton framework !";
    }

    @MyUrl("/test")
    public String doTest() {
        return "Méthode test exécutée !";
    }
}
