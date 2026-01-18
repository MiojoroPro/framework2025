package etu.sprint.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Wrapper Map<String,Object> backed by HttpSession (lazy-create on write).
 * - get/containsKey/read operations do NOT create a session if absent
 * - put/remove/invalidate will create or use existing session
 */
public class SessionMap implements Map<String,Object> {
    private final HttpServletRequest request;

    public SessionMap(HttpServletRequest request) {
        this.request = request;
    }

    private HttpSession session(boolean create) {
        return request.getSession(create);
    }

    @Override
    public int size() {
        HttpSession s = session(false);
        if (s == null) return 0;
        Enumeration<String> en = s.getAttributeNames();
        int c = 0;
        while (en.hasMoreElements()) { en.nextElement(); c++; }
        return c;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        HttpSession s = session(false);
        return s != null && s.getAttribute(String.valueOf(key)) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        HttpSession s = session(false);
        if (s == null) return false;
        Enumeration<String> en = s.getAttributeNames();
        while (en.hasMoreElements()) {
            String k = en.nextElement();
            Object v = s.getAttribute(k);
            if (Objects.equals(v, value)) return true;
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        HttpSession s = session(false);
        return s == null ? null : s.getAttribute(String.valueOf(key));
    }

    @Override
    public Object put(String key, Object value) {
        HttpSession s = session(true);
        Object prev = s.getAttribute(key);
        s.setAttribute(key, value);
        return prev;
    }

    @Override
    public Object remove(Object key) {
        HttpSession s = session(false);
        if (s == null) return null;
        Object prev = s.getAttribute(String.valueOf(key));
        s.removeAttribute(String.valueOf(key));
        return prev;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        HttpSession s = session(true);
        for (Map.Entry<? extends String, ?> e : m.entrySet()) {
            s.setAttribute(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        HttpSession s = session(false);
        if (s == null) return;
        Enumeration<String> en = s.getAttributeNames();
        List<String> keys = new ArrayList<>();
        while (en.hasMoreElements()) keys.add(en.nextElement());
        for (String k : keys) s.removeAttribute(k);
    }

    @Override
    public Set<String> keySet() {
        HttpSession s = session(false);
        if (s == null) return Collections.emptySet();
        Set<String> set = new LinkedHashSet<>();
        Enumeration<String> en = s.getAttributeNames();
        while (en.hasMoreElements()) set.add(en.nextElement());
        return set;
    }

    @Override
    public Collection<Object> values() {
        HttpSession s = session(false);
        if (s == null) return Collections.emptyList();
        List<Object> list = new ArrayList<>();
        Enumeration<String> en = s.getAttributeNames();
        while (en.hasMoreElements()) list.add(s.getAttribute(en.nextElement()));
        return list;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        HttpSession s = session(false);
        if (s == null) return Collections.emptySet();
        Set<Entry<String,Object>> set = new LinkedHashSet<>();
        Enumeration<String> en = s.getAttributeNames();
        while (en.hasMoreElements()) {
            final String k = en.nextElement();
            final Object v = s.getAttribute(k);
            set.add(new AbstractMap.SimpleImmutableEntry<>(k, v));
        }
        return set;
    }
}
