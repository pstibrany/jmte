package com.floreysoft.jmte;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.floreysoft.jmte.util.Util;

public class RendererRegistry {
    private final Map<String, NamedRenderer> namedRenderers = new HashMap<String, NamedRenderer>();
    private final Map<Class<?>, Set<NamedRenderer>> namedRenderersForClass = new HashMap<Class<?>, Set<NamedRenderer>>();

    private final Map<Class<?>, Renderer<?>> renderers = new HashMap<Class<?>, Renderer<?>>();
    private final Map<Class<?>, Renderer<?>> resolvedRendererCache = new HashMap<Class<?>, Renderer<?>>();

    public synchronized void registerNamedRenderer(NamedRenderer renderer) {
        namedRenderers.put(renderer.getName(), renderer);
        Set<Class<?>> supportedClasses = Util.asSet(renderer.getSupportedClasses());
        for (Class<?> clazz : supportedClasses) {
            Class<?> classInHierarchy = clazz;
            while (classInHierarchy != null) {
                addSupportedRenderer(classInHierarchy, renderer);
                classInHierarchy = classInHierarchy.getSuperclass();
            }
        }
    }

    public synchronized void deregisterNamedRenderer(NamedRenderer renderer) {
        namedRenderers.remove(renderer.getName());
        Set<Class<?>> supportedClasses = Util.asSet(renderer.getSupportedClasses());
        for (Class<?> clazz : supportedClasses) {
            Class<?> classInHierarchy = clazz;
            while (classInHierarchy != null) {
                Set<NamedRenderer> renderers = namedRenderersForClass.get(classInHierarchy);
                renderers.remove(renderer);
                classInHierarchy = classInHierarchy.getSuperclass();
            }
        }
    }

    private void addSupportedRenderer(Class<?> clazz, NamedRenderer renderer) {
        Collection<NamedRenderer> compatibleRenderers = getCompatibleRenderers(clazz);
        compatibleRenderers.add(renderer);
    }

    public synchronized Collection<NamedRenderer> getCompatibleRenderers(Class<?> inputType) {
        Set<NamedRenderer> renderers = namedRenderersForClass.get(inputType);
        if (renderers == null) {
            renderers = new HashSet<NamedRenderer>();
            namedRenderersForClass.put(inputType, renderers);
        }
        return renderers;
    }

    public synchronized Collection<NamedRenderer> getAllNamedRenderers() {
        Collection<NamedRenderer> values = namedRenderers.values();
        return values;
    }

    public NamedRenderer resolveNamedRenderer(String rendererName) {
        return namedRenderers.get(rendererName);
    }

    public synchronized <C> void registerRenderer(Class<C> clazz, Renderer<C> renderer) {
        renderers.put(clazz, renderer);
        resolvedRendererCache.clear();
    }

    public synchronized void deregisterRenderer(Class<?> clazz) {
        renderers.remove(clazz);
        resolvedRendererCache.clear();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized <C> Renderer<C> resolveRendererForClass(Class<C> clazz) {
        Renderer resolvedRenderer = resolvedRendererCache.get(clazz);
        if (resolvedRenderer != null) {
            return resolvedRenderer;
        }

        resolvedRenderer = renderers.get(clazz);
        if (resolvedRenderer == null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> interfaze : interfaces) {
                resolvedRenderer = resolveRendererForClass(interfaze);
                if (resolvedRenderer != null) {
                    break;
                }
            }
        }
        if (resolvedRenderer == null) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                resolvedRenderer = resolveRendererForClass(superclass);
            }
        }
        if (resolvedRenderer != null) {
            resolvedRendererCache.put(clazz, resolvedRenderer);
        }
        return resolvedRenderer;
    }
}
