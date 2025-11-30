package etu.test.controller;

import etu.sprint.framework.ModelView;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.MyUrl;
import etu.sprint.framework.annotation.RequestParam;
import etu.sprint.framework.controller.Controller;

/**
 * Controller de test pour le Sprint 7
 * Teste toutes les fonctionnalités GET/POST
 */
@Controller
public class TestController {

    // ========================================
    // GESTION DE LA LISTE D'UTILISATEURS
    // ========================================
    
    /**
     * GET /users
     * Affiche la liste de tous les utilisateurs
     */
    @MyUrl("/users")
    @HttpMethod("GET")
    public ModelView listUsers() {
        System.out.println("[TEST] GET /users - Affichage de la liste");
        
        ModelView mv = new ModelView("users-list.jsp");
        
        // Dans un vrai cas, on ajouterait la liste depuis la BDD
        // List<User> users = userService.getAllUsers();
        // mv.addItem("users", users);
        
        return mv;
    }

    // ========================================
    // CRÉATION D'UTILISATEUR
    // ========================================
    
    /**
     * GET /user/create
     * Affiche le formulaire de création
     */
    @MyUrl("/user/create")
    @HttpMethod("GET")
    public ModelView showCreateForm() {
        System.out.println("[TEST] GET /user/create - Affichage du formulaire");
        
        ModelView mv = new ModelView("user-form.jsp");
        mv.addItem("title", "Créer un nouvel utilisateur");
        mv.addItem("buttonText", "Créer");
        
        return mv;
    }

    /**
     * POST /user/create
     * Traite la soumission du formulaire de création
     */
    @MyUrl("/user/create")
    @HttpMethod("POST")
    public ModelView createUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("age") int age) {
        
        System.out.println("[TEST] POST /user/create");
        System.out.println("  - Nom: " + name);
        System.out.println("  - Email: " + email);
        System.out.println("  - Âge: " + age);
        
        // Ici : sauvegarder en base de données
        // User user = userService.createUser(name, email, age);
        
        // Simuler un ID généré
        int newUserId = 123;
        
        // IMPORTANT : Pattern POST-REDIRECT-GET
        // On redirige vers la page de détails pour éviter le double submit
        ModelView mv = new ModelView("redirect");
        mv.setView(String.format("/user/%d", newUserId));
        mv.setRedirect(true);
        
        return mv;
    }

    // ========================================
    // AFFICHAGE D'UN UTILISATEUR
    // ========================================
    
    /**
     * GET /user/{id}
     * Affiche les détails d'un utilisateur
     */
    @MyUrl("/user/{id}")
    @HttpMethod("GET")
    public ModelView showUser(int id) {
        System.out.println("[TEST] GET /user/" + id);
        
        // Ici : récupérer depuis la BDD
        // User user = userService.getUserById(id);
        
        ModelView mv = new ModelView("user-detail.jsp");
        mv.addItem("userId", id);
        mv.addItem("userName", "John Doe #" + id);
        mv.addItem("userEmail", "user" + id + "@example.com");
        mv.addItem("userAge", 25 + id);
        
        return mv;
    }

    // ========================================
    // MODIFICATION D'UTILISATEUR
    // ========================================
    
    /**
     * POST /user/{id}/update
     * Met à jour un utilisateur
     */
    @MyUrl("/user/{id}/update")
    @HttpMethod("POST")
    public ModelView updateUser(
            int id,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("age") int age) {
        
        System.out.println("[TEST] POST /user/" + id + "/update");
        System.out.println("  - Nouveau nom: " + name);
        System.out.println("  - Nouvel email: " + email);
        System.out.println("  - Nouvel âge: " + age);
        
        // Ici : mettre à jour en BDD
        // userService.updateUser(id, name, email, age);
        
        // Rediriger vers la page de détails
        ModelView mv = new ModelView("redirect");
        mv.setView("/user/" + id);
        mv.setRedirect(true);
        
        return mv;
    }

    // ========================================
    // SUPPRESSION D'UTILISATEUR
    // ========================================
    
    /**
     * POST /user/{id}/delete
     * Supprime un utilisateur
     */
    @MyUrl("/user/{id}/delete")
    @HttpMethod("POST")
    public ModelView deleteUser(int id) {
        System.out.println("[TEST] POST /user/" + id + "/delete");
        
        // Ici : supprimer de la BDD
        // userService.deleteUser(id);
        
        // Rediriger vers la liste
        ModelView mv = new ModelView("redirect");
        mv.setView("/users");
        mv.setRedirect(true);
        
        return mv;
    }

    // ========================================
    // TESTS SUPPLÉMENTAIRES
    // ========================================
    
    /**
     * GET /test/get-post
     * Page de test pour comprendre la différence GET/POST
     */
    @MyUrl("/test/get-post")
    @HttpMethod("GET")
    public ModelView testGetPostPage() {
        System.out.println("[TEST] GET /test/get-post");
        
        ModelView mv = new ModelView("test-get-post.jsp");
        return mv;
    }

    /**
     * POST /test/get-post
     * Traite le formulaire de test
     */
    @MyUrl("/test/get-post")
    @HttpMethod("POST")
    public ModelView testGetPostSubmit(@RequestParam("message") String message) {
        System.out.println("[TEST] POST /test/get-post");
        System.out.println("  - Message reçu: " + message);
        
        ModelView mv = new ModelView("success.jsp");
        mv.addItem("message", "Message POST reçu : " + message);
        
        return mv;
    }

    // ========================================
    // TEST DES AUTRES MÉTHODES HTTP
    // ========================================
    
    /**
     * PUT /user/{id}
     * Remplace complètement un utilisateur (REST)
     */
    @MyUrl("/user/{id}")
    @HttpMethod("PUT")
    public ModelView replaceUser(
            int id,
            @RequestParam("name") String name,
            @RequestParam("email") String email) {
        
        System.out.println("[TEST] PUT /user/" + id);
        System.out.println("  - Remplacement avec: " + name + ", " + email);
        
        ModelView mv = new ModelView("success.jsp");
        mv.addItem("message", "Utilisateur #" + id + " remplacé avec succès");
        
        return mv;
    }

    /**
     * DELETE /user/{id}
     * Supprime un utilisateur (REST)
     */
    @MyUrl("/user/{id}")
    @HttpMethod("DELETE")
    public ModelView deleteUserRest(int id) {
        System.out.println("[TEST] DELETE /user/" + id);
        
        ModelView mv = new ModelView("redirect");
        mv.setView("/users");
        mv.setRedirect(true);
        
        return mv;
    }

    // ========================================
    // PAGE D'ACCUEIL POUR LES TESTS
    // ========================================
    
    /**
     * GET /
     * Page d'accueil avec liens de test
     */
    @MyUrl("/")
    @HttpMethod("GET")
    public ModelView home() {
        System.out.println("[TEST] GET / - Page d'accueil");
        
        ModelView mv = new ModelView("home.jsp");
        return mv;
    }
}

/*
 * ========================================
 * COMMENT TESTER CE CONTROLLER ?
 * ========================================
 * 
 * 1. Déployer l'application sur Tomcat
 * 2. Ouvrir le navigateur
 * 3. Aller sur http://localhost:8080/Framework-Test/
 * 
 * TESTS À EFFECTUER :
 * 
 * ✅ Test 1 : Liste des utilisateurs
 *    URL: http://localhost:8080/Framework-Test/users
 *    Méthode: GET
 *    Résultat: Doit afficher users-list.jsp
 * 
 * ✅ Test 2 : Afficher formulaire de création
 *    URL: http://localhost:8080/Framework-Test/user/create
 *    Méthode: GET
 *    Résultat: Doit afficher user-form.jsp
 * 
 * ✅ Test 3 : Soumettre le formulaire
 *    - Remplir le formulaire
 *    - Cliquer sur "Créer"
 *    Méthode: POST vers /user/create
 *    Résultat: Doit rediriger vers /user/123
 * 
 * ✅ Test 4 : Voir un utilisateur
 *    URL: http://localhost:8080/Framework-Test/user/1
 *    Méthode: GET
 *    Résultat: Doit afficher user-detail.jsp
 * 
 * ✅ Test 5 : Modifier un utilisateur
 *    - Sur la page user-detail.jsp
 *    - Cliquer sur "Modifier"
 *    - Changer les valeurs
 *    - Cliquer sur "Enregistrer"
 *    Méthode: POST vers /user/1/update
 *    Résultat: Doit rediriger vers /user/1
 * 
 * ✅ Test 6 : Supprimer un utilisateur
 *    - Sur la page user-detail.jsp
 *    - Cliquer sur "Supprimer"
 *    Méthode: POST vers /user/1/delete
 *    Résultat: Doit rediriger vers /users
 * 
 * VÉRIFICATION DANS LA CONSOLE :
 * Vous devriez voir des logs comme :
 * [TEST] GET /users - Affichage de la liste
 * [TEST] POST /user/create
 *   - Nom: John
 *   - Email: john@test.com
 *   - Âge: 25
 * 
 * ========================================
 * VÉRIFICATION QUE LE SPRINT 7 FONCTIONNE
 * ========================================
 * 
 * Si vous voyez dans les logs Tomcat :
 * [Route] GET /users -> TestController.listUsers
 * [Route] POST /user/create -> TestController.createUser
 * [Route] GET /user/{id} -> TestController.showUser
 * [Route] POST /user/{id}/update -> TestController.updateUser
 * [Route] POST /user/{id}/delete -> TestController.deleteUser
 * 
 * → Le Sprint 7 fonctionne correctement ! ✅
 */