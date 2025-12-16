package etu.sprint.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation pour indiquer qu'une méthode de contrôleur retourne du JSON
 * SPRINT 9 : Support des API REST
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSON {
    /**
     * Statut HTTP à retourner (par défaut 200 OK)
     */
    int status() default 200;
    
    /**
     * Content-Type à utiliser (par défaut application/json)
     */
    String contentType() default "application/json";
}