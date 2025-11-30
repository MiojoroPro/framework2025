package etu.sprint.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation pour spécifier la méthode HTTP d'une route
 * Exemple d'utilisation :
 * 
 * @MyUrl("/user/create")
 * @HttpMethod("POST")
 * public ModelView createUser() { ... }
 * 
 * Si l'annotation n'est pas présente, la méthode HTTP par défaut est "GET"
 */
@Retention(RetentionPolicy.RUNTIME)  // L'annotation est accessible au runtime via réflexion
@Target(ElementType.METHOD)          // Cette annotation s'applique uniquement sur les méthodes
public @interface HttpMethod {
    
    /**
     * La méthode HTTP : "GET", "POST", "PUT", "DELETE", "PATCH", etc.
     * Valeur par défaut : "GET"
     */
    String value() default "GET";
}