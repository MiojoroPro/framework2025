package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.MyUrl;
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

    public String notMappedMethod() {
        return "Cette méthode n'est pas mappée à une URL.";
    }
}
