package org.agentpower.plugins;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.agentpower.client.AgentPowerClientServiceImpl;
import org.agentpower.common.Tuples;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

@Log4j2
public class AgentPowerPluginInstallationService {
    private static final Map<String, Tuples._2<
            PluginState, Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long>
            >> PLUGIN_INFO_MAP = new HashMap<>();
    private final GenericApplicationContext applicationContext;
    private final AgentPowerClientServiceImpl clientService;
    private final ThreadPoolExecutor executorService;
    private final File pluginPath;
    private final File pluginDetectPath;
    private final File pluginRecyclePath;
    private final File pluginFailedPath;
    private final File pluginImportPath;

    public AgentPowerPluginInstallationService(GenericApplicationContext applicationContext,
                                               AgentPowerClientServiceImpl clientService,
                                               File pluginPath, File pluginDetectPath,
                                               File pluginRecyclePath, File pluginFailedPath,
                                               File pluginImportPath) {
        this.executorService = new ThreadPoolExecutor(
                5, 5, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), new BlockPolicy());
        this.applicationContext = applicationContext;
        this.clientService = clientService;
        this.pluginPath = pluginPath;
        this.pluginDetectPath = pluginDetectPath;
        this.pluginRecyclePath = pluginRecyclePath;
        this.pluginFailedPath = pluginFailedPath;
        this.pluginImportPath = pluginImportPath;
        initDirs();
    }
    private void initDirs() {
        if (!pluginPath.exists()) {
            pluginPath.mkdirs();
        }
        if (!pluginDetectPath.exists()) {
            pluginDetectPath.mkdirs();
        }
        if (!pluginRecyclePath.exists()) {
            pluginRecyclePath.mkdirs();
        }
        if (!pluginFailedPath.exists()) {
            pluginFailedPath.mkdirs();
        }
        if (!pluginImportPath.exists()) {
            pluginImportPath.mkdirs();
        }
    }

    /**
     * 执行检测和安装
     */
    public synchronized void detectAndUpdateIfNeeded() throws InterruptedException {
        val jars = Optional.ofNullable(pluginDetectPath.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".jar")))
                .stream()
                .flatMap(Arrays::stream)
                .filter(File::isFile)
                .toList();
        val countDownLatch = new CountDownLatch(jars.size());
        for (File jarFile : jars) {
            executorService.submit(() -> {
                try {
                    compareAndUpdate(jarFile);
                } catch (Throwable throwable) {
                    LogError(throwable, "插件 {} 自动导入出错： {}",
                            jarFile.getName(), throwable.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        while (countDownLatch.getCount() != 0) {
            countDownLatch.await();
        }
    }

    public List<AgentPowerPluginVo> listPlugins() {
        return PLUGIN_INFO_MAP.entrySet().stream()
                .map(entry -> {
                    val pluginJarFileName = entry.getKey();
                    val pluginStatus = entry.getValue().t1().message;
                    val pluginInfo = entry.getValue().t2();
                    val pluginFileSize = pluginInfo.t3();
                    Set<String> functionBeans = pluginInfo.t2().entrySet().stream()
                            .filter(beanInfo -> clientService.isFunctionObject(beanInfo.getValue()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                    return new AgentPowerPluginVo(pluginJarFileName, pluginStatus,
                            pluginInfo.t2().size(), pluginFileSize,
                            functionBeans);
                }).toList();
    }

    public List<AgentPowerPluginVo> listUninstalledPlugins() {
        return Optional.ofNullable(pluginRecyclePath.list((file, name) -> name.toLowerCase().endsWith(".jar")))
                .stream()
                .flatMap(Arrays::stream)
                .map(file -> new AgentPowerPluginVo(file, PluginState.UNINSTALLED.message,
                        -1, file.length(), Collections.emptySet())).toList();
    }

    /**
     * 导入插件
     * @param jarFileName 插件包名
     * @param inputStream 插件包资源
     * @return 错误信息
     */
    public String importPlugin(String jarFileName, InputStream inputStream) {
        Tuples._2<PluginState, Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long>>
                pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
        if (pluginInfo != null) {
            return pluginInfo.t1().toString();
        }
        synchronized (PLUGIN_INFO_MAP) {
            pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
            if (pluginInfo != null) {
                return pluginInfo.t1().toString();
            }
            PLUGIN_INFO_MAP.put(jarFileName, new Tuples._2<>(PluginState.IMPORTING, null));
        }
        String failedInfo = null;
        try {
            File importingFile = new File(pluginImportPath, jarFileName);
            if (importingFile.exists() && !importingFile.delete()) {
                failedInfo = "导入失败，导入路径已存在同名插件包且系统无法删除，请手动删除再重试";
            } else {
                FileUtil.writeFromStream(inputStream, importingFile);
                if (! importingFile.renameTo(new File(pluginImportPath, jarFileName))) {
                    importingFile.delete();
                    failedInfo = "导入失败，转移文件出错";
                } else {
                    addToolJar(jarFileName);
                }
            }
        } catch (Exception e) {
            LogError(e, "插件 {} 导入出错 {}", jarFileName, e.getMessage());
            failedInfo = "导入失败，" + e.getMessage();
        }
        if (failedInfo != null) {
            synchronized (PLUGIN_INFO_MAP) {
                pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
                if (pluginInfo != null && pluginInfo.t1().equals(PluginState.IMPORTING)
                        && pluginInfo.t2() == null) {
                    PLUGIN_INFO_MAP.remove(jarFileName);
                }
            }
        }
        return failedInfo;
    }

    /**
     * 卸载插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String uninstallPlugin(String jarFileName) {
        try {
            removeToolJar(jarFileName, pluginRecyclePath);
        } catch (Exception e) {
            LogError(e, "插件 {} 卸载出错 {}", jarFileName, e.getMessage());
            return "卸载失败，卸载出错：" + e.getMessage();
        }
        return null;
    }

    /**
     * 还原插件 只支持已卸载且未被安装的插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String restorePlugin(String jarFileName) {
        Tuples._2<PluginState, Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long>>
                pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
        if (pluginInfo != null) {
            return pluginInfo.t1().toString();
        }
        synchronized (PLUGIN_INFO_MAP) {
            pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
            if (pluginInfo != null) {
                return pluginInfo.t1().toString();
            }
            PLUGIN_INFO_MAP.put(jarFileName, new Tuples._2<>(PluginState.RESTORING, null));
        }
        String failedInfo = null;
        try {
            // 复制插件包到tmp目录
            File jarFile = new File(pluginRecyclePath, jarFileName);
            if (! jarFile.exists()) {
                failedInfo = "还原失败，插件包不存在";
            } else {
                if (! jarFile.renameTo(new File(pluginPath, jarFileName))) {
                    failedInfo = "还原失败，转移文件出错";
                } else {
                    addToolJar(jarFileName);
                }
            }
        } catch (Exception e) {
            LogError(e, "插件 {} 还原出错 {}", jarFileName, e.getMessage());
            failedInfo = "还原失败，还原出错：" + e.getMessage();
        }
        if (failedInfo != null) {
            synchronized (PLUGIN_INFO_MAP) {
                pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
                if (pluginInfo != null && pluginInfo.t1().equals(PluginState.RESTORING)
                        && pluginInfo.t2() == null) {
                    PLUGIN_INFO_MAP.remove(jarFileName);
                }
            }
        }
        return failedInfo;
    }

    public int clearFailedCache() {
        return Optional.ofNullable(pluginFailedPath.listFiles())
                .stream().flatMap(Arrays::stream)
                .map(file -> {
                    synchronized (PLUGIN_INFO_MAP) {
                        if (PLUGIN_INFO_MAP.get(file.getName()) == null) {
                            if (file.delete()) {
                                return 1;
                            }
                        }
                    }
                    return 0;
                }).reduce(0, Integer::sum);
    }

    public int clearUninstalledCache() {
        return Optional.ofNullable(pluginRecyclePath.listFiles())
                .stream().flatMap(Arrays::stream)
                .map(file -> {
                    synchronized (PLUGIN_INFO_MAP) {
                        if (PLUGIN_INFO_MAP.get(file.getName()) == null) {
                            if (file.delete()) {
                                return 1;
                            }
                        }
                    }
                    return 0;
                }).reduce(0, Integer::sum);
    }


    /**
     * 比较并更新插件
     * @param jarFile 插件jar包文件
     */
    private void compareAndUpdate(File jarFile) {
        val jarFileName = jarFile.getName();
        val pluginInfo = PLUGIN_INFO_MAP.get(jarFileName);
        if (pluginInfo != null) {
            PluginState state = pluginInfo.t1();
            if (!state.equals(PluginState.USING)) {
                // 只对使用中的插件进行检测更新
                return;
            }
            if (jarFile.length() == pluginInfo.t2().t3()) {
                // 文件大小相同 不进行更新
                return;
            }
            log.info("检测到插件 {} 需要更新", jarFileName);
            // 卸载重装
            removeToolJar(jarFileName, pluginRecyclePath);
        }
        synchronized (PLUGIN_INFO_MAP) {
            val tmp = PLUGIN_INFO_MAP.get(jarFileName);
            if (tmp != null) {
                LogWarning("插件 {} 登记信息已存在，请检查系统是否正常: {}", jarFileName, tmp.t1().message);
                return;
            }
            if (! jarFile.renameTo(new File(pluginPath, jarFileName))) {
                LogError(new IllegalStateException("插件无法移动至插件目录"), "插件 {} 无法从检测目录转移到指定目录", jarFileName);
                return;
            } else {
                PLUGIN_INFO_MAP.put(jarFileName, new Tuples._2<>(PluginState.IMPORTING, null));
            }
        }
        // 安装插件
        addToolJar(jarFileName);
    }

    /**
     * 还原插件
     * @param jarFile 插件包名
     */
    private void restorePlugin(File jarFile) {

    }

    /**
     * 安装jar插件
     *      缓存jar包的类加载器，并遍历其中的类，验证是否需要注册到spring容器中
     *      当bean实现了AgentPowerFunction时，需要调用 clientService 的 addTool 方法
     * @param toolJarFileName jar包名称
     */
    private void addToolJar(String toolJarFileName) {
        try {
            val file = new File(pluginPath, toolJarFileName);
            List<String> loadedClassNames = GetClassNamesInJar(file);
            if (CollectionUtils.isEmpty(loadedClassNames)) {
                throw new IllegalStateException(toolJarFileName + " jar包中未找到任何类");
            }
            URL[] urls = {file.toURI().toURL()};
            AgentPowerPluginClassLoader classLoader = new AgentPowerPluginClassLoader(urls, this.getClass().getClassLoader());
            Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long> jarInfo = new Tuples._3<>(
                    classLoader, new HashMap<>(), file.length());
            // 更新信息 出错时可以卸载
            PLUGIN_INFO_MAP.put(toolJarFileName, new Tuples._2<>(PluginState.IMPORTING, jarInfo));
            Map<String, Class<?>> springClasses = getSpringBeanClassesToRegister(toolJarFileName, loadedClassNames, classLoader);
            RegisterBeans(applicationContext, clientService, jarInfo.t2(), springClasses);
            // 更新状态 标记为使用中
            PLUGIN_INFO_MAP.put(toolJarFileName, new Tuples._2<>(PluginState.USING, jarInfo));
        } catch (Exception e) {
            LogError(e, "插件jar包 {} 安装出错：{}", toolJarFileName, e.getMessage());
            boolean doRemove = false;
            synchronized (PLUGIN_INFO_MAP) {
                Tuples._2<PluginState, Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long>>
                        pluginInfo = PLUGIN_INFO_MAP.get(toolJarFileName);
                if (pluginInfo != null && pluginInfo.t1().equals(PluginState.IMPORTING)) {
                    if (pluginInfo.t2() != null) {
                        PLUGIN_INFO_MAP.put(toolJarFileName,
                                new Tuples._2<>(PluginState.IMPORT_FAILED, pluginInfo.t2()));
                        doRemove = true;
                    } else {
                        PLUGIN_INFO_MAP.remove(toolJarFileName);
                    }
                }
            }
            if (doRemove) {
                removeToolJar(toolJarFileName, pluginFailedPath);
            }
        }
    }

    /**
     * 卸载jar插件
     *      卸载时通过jar文件名卸载
     *      卸载时先获取到相关的bean 如果是函数则调用 clientService 的 removeTool 方法
     *      然后卸载所有的bean对象，关闭jar包的类加载器，并将jar包迁移到 uninstalled 目录下
     * @param toolJarFileName jar包名称
     */
    private void removeToolJar(String toolJarFileName, File backupPath) {
        Tuples._2<PluginState, Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long>> pluginInfo;
        synchronized (PLUGIN_INFO_MAP) {
            pluginInfo = PLUGIN_INFO_MAP.get(toolJarFileName);
            if (pluginInfo == null
                    // 仅使用中或导入失败可执行该方法
                    || !pluginInfo.t1().equals(PluginState.USING)
                    || !pluginInfo.t1().equals(PluginState.IMPORT_FAILED)) {
                throw new IllegalStateException(pluginInfo == null ?
                        "卸载失败，插件未安装" : pluginInfo.t1().toString());
            }
            PLUGIN_INFO_MAP.put(toolJarFileName, pluginInfo = new Tuples._2<>(PluginState.UNINSTALLING, pluginInfo.t2()));
        }
        Tuples._3<AgentPowerPluginClassLoader, Map<String, Object>, Long> jarInfo = pluginInfo.t2();
        AgentPowerPluginClassLoader classLoader = jarInfo.t1();
        try {
            UnregisterBeans(applicationContext, clientService, jarInfo.t2());
        } catch (Exception e) {
            LogError(e, "卸载插件{}时出错： {}", toolJarFileName, e.getMessage());
        } finally {
            try {
                classLoader.close();
            } catch (IOException e) {
                LogError(e, "卸载插件{}时出错： {}", toolJarFileName, e.getMessage());
            }
            synchronized (PLUGIN_INFO_MAP) {
                val toolJarInfo = PLUGIN_INFO_MAP.get(toolJarFileName);
                if (toolJarInfo != null && toolJarInfo.t1().equals(PluginState.UNINSTALLING)) {
                    // 将文件迁移到uninstalled目录下
                    moveToUninstalled(toolJarFileName, backupPath);
                    PLUGIN_INFO_MAP.remove(toolJarFileName);
                }
            }
        }
    }

    private static void moveToUninstalled(String toolJarPath, File backupPath) {
        val toolJarFile = new File(toolJarPath);
        try {
            val backupFile = new File(backupPath, toolJarFile.getName());
            if (backupFile.exists()) {
                backupFile.delete();
            }
            if (!toolJarFile.renameTo(backupFile)) {
                toolJarFile.delete();
            }
        } catch (Exception e) {
            LogError(e, "卸载插件{}时出错： {}", toolJarPath, e.getMessage());
        }
    }

    /**
     * 获取要注册的bean的字节码对象
     * @param jarFileName       jar包名称 作为bean名称前缀
     * @param loadedClassNames  jar包中加载的类名
     * @param classLoader       jar包的类加载器
     * @return bean的字节码对象集
     */
    private Map<String, Class<?>> getSpringBeanClassesToRegister(String jarFileName, List<String> loadedClassNames, AgentPowerPluginClassLoader classLoader) {
        Map<String, Class<?>> springClasses = new HashMap<>();
        for (String loadedClassname : loadedClassNames) {
            Class<?> clazz = null;
            try {
                clazz = classLoader.loadClass(loadedClassname);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            var springBeanName = GetSpringBeanName(clazz);
            if (springBeanName == null) {
                continue;
            }
            springBeanName = jarFileName + "#" + springBeanName;
            Class<?> old = springClasses.put(springBeanName, clazz);
            if (old == null) {
                try {
                    old = applicationContext.getBean(springBeanName).getClass();
                } catch (Exception ignored) {}
            }
            if (old != null) {
                // 只有同个插件包会冲突
                throw new IllegalStateException(String.format("%s 与原有的 %s 的bean名称 %s 存在冲突",
                        clazz.getName(), old.getName(), springBeanName));
            }
        }
        return springClasses;
    }


    private enum PluginState {
        IMPORTING("导入中", "请稍后重试"),
        UNINSTALLING("卸载中", "请稍后重试"),
        UNINSTALLED("已卸载", ""),
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
    @SafeVarargs
    private static <T> T ComputeIfNotNull(Supplier<T>... suppliers) {
        for (Supplier<T> supplier : suppliers) {
            T t = supplier.get();
            if (t != null) {
                return t;
            }
        }
        return null;
    }
    private static <T> T TESTER(Supplier<T> supplier, Function<Throwable, T> ifError) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            return ifError.apply(e);
        }
    }
    private static void LogError(Throwable throwable, String template, Object... params) {
        log.error(
                String.format("%s\n%s", ExceptionUtil.stacktraceToString(throwable), template),
                params);
    }
    private static void LogWarning(String template, Object... params) {
        log.warn(template, params);
    }

    private static List<String> GetClassNamesInJar(File file) throws IOException {
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


    private static String GetSpringBeanName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (clazz.isInterface()
                || clazz.isEnum()
                || Modifier.isAbstract(clazz.getModifiers())) {
            return null;
        }
        return Optional.ofNullable(
                        ComputeIfNotNull(
                                () -> TESTER(() -> clazz.getAnnotation(Component.class).value(), e -> null),
                                () -> TESTER(() -> clazz.getAnnotation(Controller.class).value(), e -> null),
                                () -> TESTER(() -> clazz.getAnnotation(RestController.class).value(), e -> null),
                                () -> TESTER(() -> clazz.getAnnotation(Service.class).value(), e -> null),
                                () -> TESTER(() -> clazz.getAnnotation(Repository.class).value(), e -> null)))
                .map(name -> name.isBlank() ? clazz.getSimpleName() : name)
                .orElse(null);
    }

    /**
     * 注册bean到spring容器中
     * @param registeredBeans 已注册bean的名称
     * @param springClasses bean 名称 和 类
     * @return 注册后的bean名称和bean对象
     */
    private static void RegisterBeans(GenericApplicationContext applicationContext,
                                      AgentPowerClientServiceImpl clientService,
                                      Map<String, Object> registeredBeans, Map<String, Class<?>> springClasses) {
        // 生成的每一个对象 如果是AgentPowerFunction的实现类，则调用clientService.addTool方法
        synchronized (clientService) {
            long registeredFunctions = springClasses.entrySet().stream()
                    .filter(entry -> {
                        String name = entry.getKey();
                        Class<?> clazz = entry.getValue();
                        DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
                        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
                        beanFactory.registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
                        return clientService.addTool(name, beanFactory.getBean(name, clazz));
                    })
                    .count();
            if (registeredFunctions == 0L) {
                // 没有函数 卸载插件
                throw new IllegalArgumentException("插件包未找到任何函数");
            }
        }
    }

    /**
     * 卸载spring中的bean
     * @param beans bean名称
     */
    private static void UnregisterBeans(GenericApplicationContext applicationContext,
                                        AgentPowerClientServiceImpl clientService,
                                        Map<String, Object> beans) {
        if (CollectionUtils.isEmpty(beans)) {
            return;
        }
        synchronized (clientService) {
            beans.forEach((beanName, bean) -> {
                try {
                    if (clientService.isFunctionObject(bean)) {
                        // 从clientService中移除工具
                        clientService.removeTool(beanName);
                    }
                    // 从Spring容器中移除Bean
                    applicationContext.getDefaultListableBeanFactory().destroyBean(bean);
                } catch (Exception e) {
                    // 记录异常
                    LogError(e, "卸载Bean {} 时出错: {}", beanName, e.getMessage());
                }
            });
            clientService.refreshResolver();
        }
    }
}
