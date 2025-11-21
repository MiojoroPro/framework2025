package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.controller.Controller;

@Controller
public class HelloController {

    @MyUrl("/hello")
    public ModelView sayHello() {
        ModelView mv = new ModelView("hello.jsp");
        mv.addItem("message", "Salut depuis ton framework !");
        return mv;
    }

    @MyUrl("/test")
    public String doTest() {
        return "Méthode test exécutée !";
    }

    @MyUrl("/hello/{name}")
    public ModelView hello(String name) {
        ModelView mv = new ModelView("test.jsp");
        mv.addItem("msg", "Route dynamique OK, bonjour " + name);
        return mv;
    }

    @MyUrl("/calc")
    public ModelView calc(@RequestParam("a") int a, @RequestParam("b") int b) {
        ModelView mv = new ModelView("test.jsp");
        mv.addItem("msg", "Somme = " + (a + b));
        return mv;
    }

    public String notMappedMethod() {
        return "Cette méthode n'est pas mappée à une URL.";
    }
}
