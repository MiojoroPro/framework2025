package etu.sprint.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultipartConfig {
    long maxFileSize() default 1024 * 1024; // 1MB par défaut
    long maxRequestSize() default 1024 * 1024 * 10; // 10MB par défaut
    String location() default ""; // Répertoire temporaire
}