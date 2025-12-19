package etu.sprint.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FileParam {
    String value(); // Nom du paramètre fichier
}