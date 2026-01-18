package etu.sprint.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Helper simple pour récupérer l'utilisateur courant et ses rôles depuis la session.
 * - Session attributes used:
 *   - "user" -> String username
 *   - "roles" -> java.util.List<String>
 */
public final class SecurityUtil {
    private SecurityUtil() {}

    public static String getCurrentUser(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null) return null;
        Object u = s.getAttribute("user");
        return u == null ? null : String.valueOf(u);
    }

    public static List<String> getRoles(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null) return Collections.emptyList();
        Object r = s.getAttribute("roles");
        if (r instanceof List) {
            List<?> raw = (List<?>) r;
            List<String> out = new ArrayList<>();
            for (Object o : raw) if (o != null) out.add(String.valueOf(o));
            return out;
        }
        if (r instanceof String) {
            String str = (String) r;
            String[] parts = str.split(",");
            List<String> out = new ArrayList<>();
            for (String p : parts) if (!p.trim().isEmpty()) out.add(p.trim());
            return out;
        }
        return Collections.emptyList();
    }

    public static boolean hasRole(HttpServletRequest request, String role) {
        for (String r : getRoles(request)) {
            if (r.equalsIgnoreCase(role)) return true;
        }
        return false;
    }

    public static boolean hasAnyRole(HttpServletRequest request, String[] roles) {
        if (roles == null || roles.length == 0) return false;
        List<String> userRoles = getRoles(request);
        for (String need : roles) {
            for (String have : userRoles) {
                if (have.equalsIgnoreCase(need)) return true;
            }
        }
        return false;
    }
}
