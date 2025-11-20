package etu.sprint.framework;

import java.util.HashMap;
import java.util.Map;

public class ModelView {

    private String view;                       // Nom de la vue (JSP ou URL)
    private Map<String, Object> data = new HashMap<>(); // Données envoyées à la vue
    private boolean isRedirect = false;        // Indique si c'est une redirection

    // Constructeur par défaut avec vue
    public ModelView(String view) {
        this.view = view;
    }

    // --- GETTERS & SETTERS ---

    public String getView() {
        return view;
    }

    // Sprint 5 : permettre de changer la vue
    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // Sprint 5 : gestion du redirect
    public boolean isRedirect() {
        return isRedirect;
    }

    public void setRedirect(boolean redirect) {
        this.isRedirect = redirect;
    }

    // --- MÉTHODES POUR LES DONNÉES ---

    // Ajouter un seul attribut
    public void addItem(String key, Object value) {
        data.put(key, value);
    }

    // Sprint 5 : ajouter plusieurs attributs d'un coup
    public void addItems(Map<String, Object> map) {
        if (map != null) {
            data.putAll(map);
        }
    }
}
