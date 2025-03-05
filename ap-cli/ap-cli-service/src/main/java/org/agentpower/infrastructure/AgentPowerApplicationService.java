package org.agentpower.infrastructure;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import lombok.val;
import org.agentpower.client.service.AgentPowerClientServiceImpl;
import org.agentpower.common.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@Component
public class AgentPowerApplicationService implements ApplicationListener<ApplicationContextInitializedEvent> {
    private static final Logger log = LoggerFactory.getLogger(AgentPowerApplicationService.class);
    private static final Map<String, PluginState> PLUGIN_STATUS_LOCK = new ConcurrentHashMap<>();
    private static final Map<String, Tuples._3<URLClassLoader, List<String>, Long>> PLUGIN_MAP = new ConcurrentHashMap<>();
    private static final File PLUGIN_DIR = new File("./plugins");
    private static final File TMP_DIR = new File(PLUGIN_DIR, "tmp");
    private static final File UNINSTALLED_DIR = Optional.of(new File("uninstalled"))
            .map(file -> {
                if (!file.exists()) {
                    file.mkdirs();
                }
                return file;
            }).get();
    private final ExecutorService executorService;
    private ConfigurableApplicationContext applicationContext;
    private AgentPowerClientServiceImpl clientService;

    private final Integer refreshTime;

    public AgentPowerApplicationService(
            @Value("${agent-power.plugins.refresh.duration:5}")
            Integer refreshTime) {
        this.executorService = Executors.newFixedThreadPool(5);
        if (refreshTime < 5) {
            refreshTime = 5;
        }
        this.refreshTime = refreshTime;
    }

    /**
     * 列出已安装的插件
     * @return 插件包
     */
    public List<String> listInstalledPlugins() {
        return List.copyOf(PLUGIN_MAP.keySet());
    }

    /**
     * 列出已卸载的插件
     * @return 插件包
     */
    public List<String> listUninstalledPlugins() {
        String[] jars = UNINSTALLED_DIR.list((file, name) -> Optional.of(name.lastIndexOf("."))
                .filter(index -> index > 0)
                .map(index -> name.substring(index + 1))
                .map(suffix -> suffix.equalsIgnoreCase("jar"))
                .orElse(false));
        return jars == null ? Collections.emptyList() : Arrays.asList(jars);
    }

    /**
     * 导入插件
     * @param jarFileName 插件包名
     * @param inputStream 插件包资源
     * @return 错误信息
     */
    public String importPlugin(String jarFileName, InputStream inputStream) {
        PluginState pluginState = PLUGIN_STATUS_LOCK.get(jarFileName);
        if (pluginState != null) {
            return "操作失败，插件包正在" + pluginState.message;
        }
        if (PLUGIN_MAP.containsKey(jarFileName)) {
            return "导入失败，插件包已存在，请先卸载";
        }
        try {
            File targetFile = new File(PLUGIN_DIR, jarFileName);
            if (targetFile.exists()) {
                // 卸载
                FileInputStream fis = new FileInputStream(targetFile);
                try (FileLock lock = fis.getChannel().lock()) {
                    // 当拿到这把锁的时候 有两种情况 一种是插件已安装 一种是插件还未被检测到 未安装
                    if (PLUGIN_MAP.containsKey(jarFileName)) {
                        // 插件已安装 提示先卸载
                        return "导入失败，插件包已存在，请先卸载";
                    }
                    // 插件未被安装 直接删除文件
                    fis.getChannel().truncate(0);
                }
            }
            targetFile.delete();
            FileUtil.writeFromStream(inputStream, targetFile);
        } catch (Exception e) {
            return "导入失败，写入文件出错：" + e.getMessage();
        }
        return null;
    }

    /**
     * 卸载插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String uninstallPlugin(String jarFileName) {
        PluginState pluginState = PLUGIN_STATUS_LOCK.get(jarFileName);
        if (pluginState != null) {
            return "操作失败，插件包正在" + pluginState.message;
        }
        try {
            removeToolJar(jarFileName);
        } catch (Exception e) {
            return "卸载失败，卸载出错：" + e.getMessage();
        }
        return null;
    }

    /**
     * 还原插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String restorePlugin(String jarFileName) {
        PluginState pluginState = PLUGIN_STATUS_LOCK.get(jarFileName);
        if (pluginState != null) {
            return "操作失败，插件包正在" + pluginState.message;
        }
        try {
            // 复制插件包到tmp目录
            File jarFile = new File(UNINSTALLED_DIR, jarFileName);
            File tmpFile = new File(TMP_DIR, jarFileName);
            if (! jarFile.exists()) {
                return "还原失败，插件包不存在";
            }
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            jarFile.renameTo(tmpFile);
            File pluginFile = new File(PLUGIN_DIR, jarFileName);
            if (pluginFile.exists()) {
                return "还原失败，插件包已存在，请先卸载";
            }
            tmpFile.renameTo(pluginFile);
        } catch (Exception e) {
            return "还原失败，还原出错：" + e.getMessage();
        }
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        applicationContext = event.getApplicationContext();
        val functionMap = applicationContext.getBeansOfType(AgentPowerFunction.class);
        clientService = applicationContext.getBean(AgentPowerClientServiceImpl.class);
        functionMap.forEach(clientService::addTool);

        // 启动初始化
        // 1 遍历 plugin 目录 注册里面的jar包
        if (PLUGIN_DIR.exists() && PLUGIN_DIR.isDirectory()) {
            Optional.ofNullable(PLUGIN_DIR.listFiles((dir, name) -> name.endsWith(".jar"))).stream()
                    .flatMap(Arrays::stream)
                    .map(File::getAbsolutePath)
                    .forEach(this::addToolJar);
        }
        // 2 监听 plugin 目录
        startWatch();

        // TODO 插件自动安装与卸载
        //  2 提供jar的卸载方法
        //      卸载时通过jar文件名卸载
        //      卸载时先获取到相关的bean 调用 clientService 的 removeTool 方法
        //      最终关闭jar包的类加载器，并将jar迁移到 uninstalled 目录下
    }

    private void startWatch() {
        new Thread(() -> {
            // TODO 循环遍历 plugin 目录，如果 对于已存在的jar文件，对比文件大小信息，出现变化则进行热更新
            //      对每个遍历到的文件操作时通过线程池以加快对比和更新速度等
            while (true) {
                Optional.ofNullable(PLUGIN_DIR.listFiles((dir, name) -> name.endsWith(".jar"))).stream()
                        .flatMap(Arrays::stream)
                        .forEach(file ->
                                executorService.submit(() -> {
                                    try {
                                        compareAndUpdate(file);
                                    } catch (Throwable throwable) {
                                        logError(throwable, "插件目录下文件 {} 处理出错： {}",
                                                file.getName(), throwable.getMessage());
                                    }
                                })
                        );
                // 每次睡眠五秒
                Unsafe.getUnsafe().park(true,
                        TimeUnit.SECONDS.convert(refreshTime, TimeUnit.SECONDS));
            }
        }).start();
    }

    /**
     * 比较并更新插件
     * @param jarFile 插件jar包文件
     */
    private void compareAndUpdate(File jarFile) {
        val jarFileName = jarFile.getName();
        val existJarInfo = PLUGIN_MAP.get(jarFileName);
        if (existJarInfo != null) {
            if (jarFile.length() == existJarInfo.t2()) {
                return;
            }
            // 卸载插件
            removeToolJar(jarFileName);
        }
        // 安装插件
        addToolJar(jarFileName);
    }

    /**
     * 安装jar插件
     *      缓存jar包的类加载器，并遍历其中的类，验证是否需要注册到spring容器中
     *      当bean实现了AgentPowerFunction时，需要调用 clientService 的 addTool 方法
     * @param toolJarPath jar包路径
     */
    private void addToolJar(String toolJarPath) {
        try {
            val file = new File(toolJarPath);
            FileInputStream fis = new FileInputStream(file);
            try (FileLock lock = fis.getChannel().lock()) {
                List<String> loadedClassNames = this.getClassNamesInJar(fis);
                if (CollectionUtils.isEmpty(loadedClassNames)) {
                    throw new IllegalStateException(toolJarPath + " jar包中未找到任何类");
                }
                URL[] urls = {file.toURI().toURL()};
                URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
                Tuples._3<URLClassLoader, List<String>, Long> jarInfo = new Tuples._3<>(classLoader, new LinkedList<>(), file.length());
                Optional.ofNullable(PLUGIN_MAP.put(toolJarPath, jarInfo))
                        .ifPresent(existJar -> {
                            throw new IllegalStateException(" 插件已存在");
                        });
                Map<String, Class<?>> springClasses = getSpringBeanClassesToRegister(loadedClassNames, classLoader);
                // noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (jarInfo) {
                    registerBeans(jarInfo.t1(), springClasses);
                }
            }
        } catch (Exception e) {
            logError(e, "插件jar包 {} 安装出错：{}", toolJarPath, e.getMessage());
            removeToolJar(toolJarPath);
        }
    }

    /**
     * 卸载jar插件
     * @param toolJarPath jar包路径
     */
    private void removeToolJar(String toolJarPath) {
        Optional.ofNullable(PLUGIN_MAP.get(toolJarPath))
                .ifPresent(jarInfo -> {
                    // noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (jarInfo) {
                        PLUGIN_STATUS_LOCK.put(toolJarPath, PluginState.UNINSTALLING);
                        URLClassLoader classLoader = jarInfo.t0();
                        try {
                            unregisterBeans(jarInfo.t1());
                        } catch (Exception e) {
                            logError(e, "卸载插件出错： {}", e.getMessage());
                        } finally {
                            PLUGIN_MAP.remove(toolJarPath);
                            try {
                                classLoader.close();
                            } catch (IOException e) {
                                logError(e, "卸载插件出错： {}", e.getMessage());
                            }
                            // 将文件迁移到uninstalled目录下
                            moveToUninstalled(toolJarPath);
                            PLUGIN_STATUS_LOCK.remove(toolJarPath);
                        }
                    }
                });
    }

    private void moveToUninstalled(String toolJarPath) {
        val toolJarFile = new File(toolJarPath);
        try {
            val uninstalledFile = new File(UNINSTALLED_DIR, toolJarFile.getName());
            if (uninstalledFile.exists()) {
                uninstalledFile.delete();
            }
            if (!toolJarFile.renameTo(uninstalledFile)) {
                toolJarFile.delete();
            }
        } catch (Exception e) {
            logError(e, "插件{} 卸载出错： {}", toolJarPath, e.getMessage());
        }
    }

    private Map<String, Class<?>> getSpringBeanClassesToRegister(List<String> loadedClassNames, URLClassLoader classLoader) {
        Map<String, Class<?>> springClasses = new HashMap<>();
        for (String loadedClassname : loadedClassNames) {
            Class<?> clazz = null;
            try {
                clazz = classLoader.loadClass(loadedClassname);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            val springBeanName = this.getSpringBeanName(clazz);
            if (springBeanName == null) {
                continue;
            }
            Class<?> old = springClasses.put(springBeanName, clazz);
            if (old == null) {
                try {
                    old = applicationContext.getBean(springBeanName).getClass();
                } catch (Exception ignored) {}
            }
            if (old != null) {
                throw new IllegalStateException(String.format("%s 与原有的 %s 的bean名称 %s 存在冲突",
                        clazz.getName(), old.getName(), springBeanName));
            }
        }
        return springClasses;
    }

    private List<String> getClassNamesInJar(InputStream is) throws IOException {
        List<String> classPathList = new LinkedList<>();
        // 获取jar的流,打开jar文件
        try (JarInputStream jarInputStream = new JarInputStream(is)){
            //逐个获取jar种文件
            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            //遍历
            while (jarEntry != null){
                //获取文件路径
                String name = jarEntry.getName();
                if (name.endsWith(".class")){
                    String classNamePath = name.replace(".class", "").replace("/",".");
                    classPathList.add(classNamePath);
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        }
        return classPathList;
    }


    private String getSpringBeanName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (clazz.isInterface()
                || clazz.isEnum()
                || Modifier.isAbstract(clazz.getModifiers())) {
            return null;
        }
        return Optional.ofNullable(
                computeIfNotNull(
                        () -> test(() -> clazz.getAnnotation(Component.class).value(),      e -> null),
                        () -> test(() -> clazz.getAnnotation(Controller.class).value(),     e -> null),
                        () -> test(() -> clazz.getAnnotation(RestController.class).value(), e -> null),
                        () -> test(() -> clazz.getAnnotation(Service.class).value(),        e -> null),
                        () -> test(() -> clazz.getAnnotation(Repository.class).value(),     e -> null)))
                .map(name -> name.isBlank() ? clazz.getSimpleName() : name)
                .orElse(null);
    }


    /**
     * 注册bean到spring容器中
     * @param registeredBeans 已注册bean的名称
     * @param springClasses bean 名称 和 类
     * @return 注册后的bean名称和bean对象
     */
    private void registerBeans(List<String> registeredBeans, Map<String, Class<?>> springClasses) {
        // 生成的每一个对象 如果是AgentPowerFunction的实现类，则调用clientService.addTool方法
        // noinspection SynchronizeOnNonFinalField
        synchronized (clientService) {
            springClasses.forEach((name, clazz) -> {
                Object beanInstance = applicationContext.getBeanFactory().createBean(clazz);
                applicationContext.getBeanFactory().registerSingleton(name, beanInstance);
                registeredBeans.add(name);
                if (beanInstance instanceof AgentPowerFunction) {
                    clientService.addTool(name, beanInstance);
                }
            });
        }
    }

    /**
     * 卸载spring中的bean
     * @param beans bean名称
     */
    private synchronized void unregisterBeans(List<String> beans) {
        // noinspection SynchronizeOnNonFinalField
        synchronized (clientService) {
            beans.forEach(beanName -> {
                try {
                    Object bean = applicationContext.getBean(beanName);
                    if (bean instanceof AgentPowerFunction) {
                        // 从clientService中移除工具
                        clientService.removeTool(beanName);
                    }
                    // 从Spring容器中移除Bean
                    applicationContext.getBeanFactory().destroyBean(bean);
                } catch (Exception e) {
                    // 记录异常
                    logError(e, "卸载Bean {} 时出错: {}", beanName, e.getMessage());
                }
            });
        }
    }

    @SafeVarargs
    private static <T> T computeIfNotNull(Supplier<T>... suppliers) {
        for (Supplier<T> supplier : suppliers) {
            T t = supplier.get();
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private static <T> T test(Supplier<T> supplier, Function<Throwable, T> ifError) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            return ifError.apply(e);
        }
    }
    private static void logError(Throwable throwable, String template, Object... params) {
        log.error(template, params);
        log.error(ExceptionUtil.stacktraceToString(throwable));
    }

    private enum PluginState {
        IMPORTING("导入中"),
        UNINSTALLING("卸载中"),
        RESTORING("还原中"),
        ;
        private final String message;
        PluginState(String message) {
            this.message = message;
        }
    }
}