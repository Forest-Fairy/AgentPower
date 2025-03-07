package org.agentpower.plugins;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import lombok.val;
import org.agentpower.client.service.AgentPowerClientServiceImpl;
import org.agentpower.common.Tuples;
import org.agentpower.infrastructure.AgentPowerFunction;
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
    private static final Map<String, Tuples._2<PluginState, Tuples._3<URLClassLoader, List<String>, Long>>>
            PLUGIN_INFO_MAP = new HashMap<>();
    private static final File PLUGIN_DIR = new File("./plugins");
    private static final File TMP_DIR = new File(PLUGIN_DIR, "tmp");
    private static final File INVALID_DIR = new File(PLUGIN_DIR, "invalid");
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
        return PLUGIN_INFO_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().t0() == PluginState.USING)
                .map(Map.Entry::getKey).toList();
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
        if (jarFileName.replace("\\", "/").indexOf("/") > 0) {
            return "操作失败，插件包名不能包含路径";
        }
        synchronized (PLUGIN_INFO_MAP) {
            Tuples._2<PluginState, Tuples._3<URLClassLoader, List<String>, Long>>
                    pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
            if (pluginInfo != null) {
                return pluginInfo.t0().toString();
            }
            PLUGIN_INFO_MAP.put(jarFileName, new Tuples._2<>(PluginState.WAITING, null));
        }
        try {
            File targetFile = new File(PLUGIN_DIR, jarFileName);
            if (targetFile.exists()) {
                if (! targetFile.delete()) {
                    return "导入失败，路径已存在同名插件包，系统无法删除，请手动删除";
                }
            }
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
        if (jarFileName.replace("\\", "/").indexOf("/") > 0) {
            return "操作失败，插件包名不能包含路径";
        }
        try {
            removeToolJar(jarFileName);
        } catch (Exception e) {
            logError(e, "插件 {} 卸载出错 {}", jarFileName, e.getMessage());
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
        synchronized (PLUGIN_INFO_MAP) {
            Tuples._2<PluginState, Tuples._3<URLClassLoader, List<String>, Long>>
                    pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
            if (pluginInfo != null) {
                return pluginInfo.t0().toString();
            }
            PLUGIN_INFO_MAP.put(jarFileName, new Tuples._2<>(PluginState.WAITING, null));
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
            logError(e, "插件 {} 还原出错 {}", jarFileName, e.getMessage());
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
        val pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
        if (pluginInfo != null) {
            PluginState state = pluginInfo.t0();
            if (! state.equals(PluginState.USING) &&
                    ! state.equals(PluginState.WAITING)) {
                // 存在操作中的插件，跳过
                return;
            }
            if (state.equals(PluginState.USING)) {
                if (jarFile.length() == pluginInfo.t1().t2()) {
                    // 文件大小相同，跳过
                    return;
                }
                // 卸载插件
                removeToolJar(jarFileName);
            }
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
            List<String> loadedClassNames = this.getClassNamesInJar(file);
            if (CollectionUtils.isEmpty(loadedClassNames)) {
                throw new IllegalStateException(toolJarPath + " jar包中未找到任何类");
            }
            URL[] urls = {file.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
            Tuples._3<URLClassLoader, List<String>, Long> jarInfo = new Tuples._3<>(
                    classLoader, new LinkedList<>(), file.length());
            // 更新信息 出错时可以卸载
            PLUGIN_INFO_MAP.put(toolJarPath, new Tuples._2<>(PluginState.IMPORTING, jarInfo));
            Map<String, Class<?>> springClasses = getSpringBeanClassesToRegister(loadedClassNames, classLoader);
            registerBeans(jarInfo.t1(), springClasses);
            // 更新状态 标记为使用中
            PLUGIN_INFO_MAP.put(toolJarPath, new Tuples._2<>(PluginState.USING, jarInfo));
        } catch (Exception e) {
            logError(e, "插件jar包 {} 安装出错：{}", toolJarPath, e.getMessage());
            boolean doRemove = false;
            synchronized (PLUGIN_INFO_MAP) {
                Tuples._2<PluginState, Tuples._3<URLClassLoader, List<String>, Long>>
                        pluginInfo = PLUGIN_INFO_MAP.get(toolJarPath);
                if (pluginInfo != null && pluginInfo.t1() != null
                        && pluginInfo.t0().equals(PluginState.IMPORTING)) {
                    doRemove = true;
                }
            }
            if (doRemove) {
                removeToolJar(toolJarPath);
            }
        }
    }

    /**
     * 卸载jar插件
     *      卸载时通过jar文件名卸载
     *      卸载时先获取到相关的bean 如果是函数则调用 clientService 的 removeTool 方法
     *      然后卸载所有的bean对象，关闭jar包的类加载器，并将jar包迁移到 uninstalled 目录下
     * @param toolJarPath jar包路径
     */
    private void removeToolJar(String toolJarPath) {
        Tuples._2<PluginState, Tuples._3<URLClassLoader, List<String>, Long>> pluginInfo;
        synchronized (PLUGIN_INFO_MAP) {
            pluginInfo = PLUGIN_INFO_MAP.get(toolJarPath);
            if (pluginInfo == null
                    // 仅使用中或导入失败可执行该方法
                    || !pluginInfo.t0().equals(PluginState.USING)
                    || !pluginInfo.t0().equals(PluginState.IMPORT_FAILED)) {
                throw new IllegalStateException(pluginInfo == null ? "卸载失败，插件未安装" : pluginInfo.t0().toString());
            }
            PLUGIN_INFO_MAP.put(toolJarPath, pluginInfo = new Tuples._2<>(PluginState.UNINSTALLING, pluginInfo.t1()));
        }
        Tuples._3<URLClassLoader, List<String>, Long> jarInfo = pluginInfo.t1();
        URLClassLoader classLoader = jarInfo.t0();
        try {
            unregisterBeans(jarInfo.t1());
        } catch (Exception e) {
            logError(e, "卸载插件出错： {}", e.getMessage());
        } finally {
            try {
                classLoader.close();
            } catch (IOException e) {
                logError(e, "卸载插件出错： {}", e.getMessage());
            }
            // 将文件迁移到uninstalled目录下
            moveToUninstalled(toolJarPath);
            PLUGIN_INFO_MAP.remove(toolJarPath);
        }
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

    private List<String> getClassNamesInJar(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
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
        if (CollectionUtils.isEmpty(beans)) {
            return;
        }
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
            clientService.refreshResolver();
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
        WAITING("等待导入中", "请稍后重试"),
        IMPORTING("导入中", "请稍后重试"),
        UNINSTALLING("卸载中", "请稍后重试"),
        RESTORING("还原中", "请稍后重试"),
        USING("使用中", "请卸载重试"),
        IMPORT_FAILED("导入失败", "请稍后重试"),
        ;
        private final String message;
        private final String tips;
        PluginState(String message, String tips) {
            this.message = message;
            this.tips = tips;
        }
        @Override
        public String toString() {
            return String.format("插件正在%s，%s", message, tips);
        }
    }
}