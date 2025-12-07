package etu.test.controller;

import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.ModelView;
import etu.sprint.framework.controller.Controller;

import java.util.Arrays;
import java.util.Map;

@Controller
public class UserController {
    
    @MyUrl("/user/save/{id}")
    @HttpMethod("POST")
    public ModelView save(Map<String, Object> formData, int id) {
        System.out.println("=== SPRINT 8: save() called ===");
        System.out.println("ID from URL: " + id);
        System.out.println("Form data: " + formData);
        
        ModelView mv = new ModelView("user-detail.jsp");
        
        // formData contient tous les paramètres POST/GET
        String username = (String) formData.get("username");
        String email = (String) formData.get("email");
        String ageStr = formData.get("age") != null ? formData.get("age").toString() : null;
        
        // Traitement des données...
        mv.addItem("id", id);
        mv.addItem("username", username);
        mv.addItem("email", email);
        
        if (ageStr != null) {
            try {
                int age = Integer.parseInt(ageStr);
                mv.addItem("age", age);
            } catch (NumberFormatException e) {
                mv.addItem("age", 0);
            }
        }
        
        // Ajouter tous les paramètres de la requête pour les afficher
        mv.addItem("allParams", formData);
        
        return mv;
    }
    
    @MyUrl("/user/search")
    public ModelView search(Map<String, Object> params) {
        System.out.println("=== SPRINT 8: search() called ===");
        System.out.println("Search params: " + params);
        
        ModelView mv = new ModelView("search-results.jsp");
        
        // params contient les paramètres de requête GET
        String keyword = (String) params.get("keyword");
        String category = (String) params.get("category");
        String pageStr = params.get("page") != null ? params.get("page").toString() : "1";
        
        int page = 1;
        try {
            page = Integer.parseInt(pageStr);
        } catch (NumberFormatException e) {
            page = 1;
        }
        
        mv.addItem("keyword", keyword);
        mv.addItem("category", category);
        mv.addItem("page", page);
        mv.addItem("results", "Résultats pour: " + keyword + " dans " + category);
        
        return mv;
    }
    
    @MyUrl("/user/details/{userId}")
    public ModelView details(
            @RequestParam("format") String format,
            Map<String, Object> extraParams,
            int userId) {
        
        System.out.println("=== SPRINT 8: details() called ===");
        System.out.println("User ID: " + userId);
        System.out.println("Format: " + format);
        System.out.println("Extra params: " + extraParams);
        
        ModelView mv = new ModelView("user-details.jsp");
        
        // format vient de @RequestParam("format")
        // extraParams contient d'autres paramètres de requête
        // userId vient de l'URL /user/details/{userId}
        
        mv.addItem("userId", userId);
        mv.addItem("format", format);
        mv.addItem("extraInfo", extraParams);
        
        // Exemple d'accès à un paramètre spécifique
        String viewType = (String) extraParams.get("view");
        if (viewType != null) {
            mv.addItem("viewType", viewType);
        }
        
        return mv;
    }
    
    @MyUrl("/user/list")
    @HttpMethod("GET")
    public ModelView listUsers(Map<String, Object> params) {
        System.out.println("=== SPRINT 8: listUsers() called ===");
        System.out.println("Params: " + params);
        
        ModelView mv = new ModelView("user-list.jsp");
        
        // Récupérer les paramètres de pagination et tri
        String sortBy = (String) params.get("sort");
        String order = (String) params.get("order");
        String limitStr = params.get("limit") != null ? params.get("limit").toString() : "10";
        
        int limit = 10;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException e) {
            limit = 10;
        }
        
        mv.addItem("sortBy", sortBy != null ? sortBy : "id");
        mv.addItem("order", order != null ? order : "asc");
        mv.addItem("limit", limit);
        mv.addItem("users", Arrays.asList("User1", "User2", "User3"));
        
        return mv;
    }
}