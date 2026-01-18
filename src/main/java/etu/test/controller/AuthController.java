package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.controller.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

@Controller
public class AuthController {

    @MyUrl("/auth/login")
    @HttpMethod("GET")
    public ModelView loginForm() {
        return new ModelView("login-form.jsp");
    }

    // Login simple pour les tests: passer username et roles (csv)
    @MyUrl("/auth/login")
    @HttpMethod("POST")
    public ModelView doLogin(HttpServletRequest request) {
        String user = request.getParameter("username");
        String roles = request.getParameter("roles");
        if (user == null || user.trim().isEmpty()) user = "anonymous";
        HttpSession s = request.getSession(true);
        s.setAttribute("user", user);
        List<String> roleList = roles == null || roles.trim().isEmpty() ? List.of() : Arrays.asList(roles.split(","));
        s.setAttribute("roles", roleList);

        ModelView mv = new ModelView("login-success.jsp");
        mv.addItem("msg", "Connecté en tant que: " + user + " roles=" + roleList);
        return mv;
    }

    @MyUrl("/auth/logout")
    @HttpMethod("POST")
    public ModelView logout(HttpServletRequest request) {
        try {
            HttpSession s = request.getSession(false);
            if (s != null) s.invalidate();
        } catch (Exception ignored) {}
        ModelView mv = new ModelView("login-success.jsp");
        mv.addItem("msg", "Déconnecté");
        return mv;
    }
}
