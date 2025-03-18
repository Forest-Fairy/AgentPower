package org.agentpower.plugins;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器，打破双亲委派机制以提高插件包的类的优先级，实现重复类以插件包的优先
 */
public class AgentPowerPluginClassLoader extends URLClassLoader {
    public AgentPowerPluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // If still not found, then invoke findClass in order
                // to find the class.
                long t1 = System.nanoTime();
                try {
                    c = findClass(name);
                    jdk.internal.perf.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    jdk.internal.perf.PerfCounter.getFindClassTime().increment();
                } catch (ClassNotFoundException e) {
                    // failed to find the class then load from parent
                    c = this.getParent().loadClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

}
