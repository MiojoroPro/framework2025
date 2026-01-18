package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.annotation.SessionMap;
import etu.sprint.framework.annotation.SessionParam;
import etu.sprint.framework.controller.Controller;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class SessionController {

    @MyUrl("/session/form")
    @HttpMethod("GET")
    public ModelView form() {
        return new ModelView("session-form.jsp");
    }

    @MyUrl("/session/set")
    @HttpMethod("POST")
    public ModelView set(@RequestParam("k") String key, @RequestParam("v") String value, @SessionMap Map<String,Object> session) {
        session.put(key, value);
        ModelView mv = new ModelView("session-view.jsp");
        mv.addItem("msg", "Ajouté '"+key+"' = '"+value+"' en session");
        mv.addItem("session", session);
        return mv;
    }

    @MyUrl("/session/get")
    @HttpMethod("GET")
    public ModelView get(@RequestParam("k") String key, @SessionParam("k") String dummy, @SessionMap Map<String,Object> session) {
        // démonstration : récupération par convention/annotation
        Object v = session.get(key);
        ModelView mv = new ModelView("session-view.jsp");
        mv.addItem("msg", "Clé: " + key + " → valeur: " + v);
        mv.addItem("session", session);
        return mv;
    }

    @MyUrl("/session/remove")
    @HttpMethod("POST")
    public ModelView remove(@RequestParam("k") String key, @SessionMap Map<String,Object> session) {
        Object prev = session.remove(key);
        ModelView mv = new ModelView("session-view.jsp");
        mv.addItem("msg", "Suppression: " + key + " (ancienne valeur: " + prev + ")");
        mv.addItem("session", session);
        return mv;
    }

    @MyUrl("/session/invalidate")
    @HttpMethod("POST")
    public ModelView invalidate(HttpSession session) {
        session.invalidate();
        ModelView mv = new ModelView("session-view.jsp");
        mv.addItem("msg", "Session invalidée");
        mv.addItem("session", new java.util.HashMap<>());
        return mv;
    }

    @MyUrl("/session/view")
    @HttpMethod("GET")
    public ModelView view(@SessionMap Map<String,Object> session) {
        ModelView mv = new ModelView("session-view.jsp");
        mv.addItem("session", session);
        return mv;
    }
}
