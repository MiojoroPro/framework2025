package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.*;
import etu.sprint.framework.controller.Controller;

@Controller
public class ExampleController {

    // GET /users - Afficher la liste
    @GetMapping("/users")
    public ModelView listUsers() {
        ModelView mv = new ModelView("users-list.jsp");
        mv.addItem("message", "Liste des utilisateurs (GET)");
        mv.addItem("method", "GET");
        return mv;
    }

    // POST /users - Créer un utilisateur
    @PostMapping("/users")
    public ModelView createUser(
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email
    ) {
        ModelView mv = new ModelView("user-created.jsp");
        mv.addItem("message", "Utilisateur créé avec succès (POST)");
        mv.addItem("method", "POST");
        mv.addItem("nom", nom);
        mv.addItem("prenom", prenom);
        mv.addItem("email", email);
        return mv;
    }

    // GET /users/{id} - Détails d'un utilisateur
    @GetMapping("/users/{id}")
    public ModelView getUser(int id) {
        ModelView mv = new ModelView("user-detail.jsp");
        mv.addItem("userId", id);
        mv.addItem("message", "Détails de l'utilisateur #" + id + " (GET)");
        mv.addItem("method", "GET");
        return mv;
    }

    // PUT /users/{id} - Modifier un utilisateur
    @PutMapping("/users/{id}")
    public ModelView updateUser(
            int id,
            @RequestParam("nom") String nom,
            @RequestParam("email") String email
    ) {
        ModelView mv = new ModelView("user-updated.jsp");
        mv.addItem("userId", id);
        mv.addItem("message", "Utilisateur #" + id + " mis à jour (PUT)");
        mv.addItem("method", "PUT");
        mv.addItem("nom", nom);
        mv.addItem("email", email);
        return mv;
    }

    // DELETE /users/{id} - Supprimer un utilisateur
    @DeleteMapping("/users/{id}")
    public ModelView deleteUser(int id) {
        ModelView mv = new ModelView("user-deleted.jsp");
        mv.addItem("userId", id);
        mv.addItem("message", "Utilisateur #" + id + " supprimé (DELETE)");
        mv.addItem("method", "DELETE");
        return mv;
    }

    // GET /users/search - Rechercher des utilisateurs
    @RequestMapping(value = "/users/search", method = "GET")
    public ModelView searchUsers(
            @RequestParam("query") String query,
            @RequestParam("ville") String ville
    ) {
        ModelView mv = new ModelView("users-search.jsp");
        mv.addItem("message", "Recherche d'utilisateurs (GET via RequestMapping)");
        mv.addItem("method", "GET");
        mv.addItem("query", query);
        mv.addItem("ville", ville);
        return mv;
    }

    // GET /legacy - Route legacy
    @MyUrl("/legacy")
    public ModelView legacyRoute() {
        ModelView mv = new ModelView("legacy.jsp");
        mv.addItem("message", "Route legacy avec @MyUrl (GET par défaut)");
        mv.addItem("method", "GET");
        return mv;
    }
}