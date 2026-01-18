package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.AllowAnonymous;
import etu.sprint.framework.annotation.Authenticated;
import etu.sprint.framework.annotation.RolesAllowed;
import etu.sprint.framework.controller.Controller;

@Controller
public class SecureController {

    @MyUrl("/secure/open")
    @HttpMethod("GET")
    @AllowAnonymous
    public ModelView open() {
        ModelView mv = new ModelView("secure-view.jsp");
        mv.addItem("msg", "Ressource publique (anonymous)");
        return mv;
    }

    @MyUrl("/secure/user")
    @HttpMethod("GET")
    @Authenticated
    public ModelView forUser() {
        ModelView mv = new ModelView("secure-view.jsp");
        mv.addItem("msg", "Ressource protégée — utilisateur connecté requis");
        return mv;
    }

    @MyUrl("/secure/admin")
    @HttpMethod("GET")
    @RolesAllowed({"ADMIN"})
    public ModelView forAdmin() {
        ModelView mv = new ModelView("secure-view.jsp");
        mv.addItem("msg", "Ressource protégée — rôle ADMIN requis");
        return mv;
    }
}
