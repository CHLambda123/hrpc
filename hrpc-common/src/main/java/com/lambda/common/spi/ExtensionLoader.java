package com.lambda.common.spi;

import com.lambda.common.exception.HrpcRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ExtensionLoader<T> {
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>(64);

    private static final String DIR = "META-INF/hrpc/";

    private final Class<T> type;

    private final  ConcurrentMap<String, Object> cachedInstances = new ConcurrentHashMap<>(64);

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new HrpcRuntimeException("Extension type == null!");
        } else if (!type.isInterface()) {
            throw new HrpcRuntimeException("Extension type (" + type + ") is not an interface!");
        } else if (!type.isAnnotationPresent(Spi.class)) {
            throw new HrpcRuntimeException("Extension type (" + type + ") is not an extension, because it is NOT annotated with @" + Spi.class.getSimpleName() + "!");
        } else {
            ExtensionLoader<T> loader = (ExtensionLoader)EXTENSION_LOADERS.get(type);
            if (loader == null) {
                EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
                loader = (ExtensionLoader)EXTENSION_LOADERS.get(type);
            }

            return loader;
        }
    }

    public T getExtension(String name, Object... args) {
        Object obj = cachedInstances.computeIfAbsent(name, s -> getImpl(s, args));
        return (T) obj;
    }

    private Object getImpl(String name, Object... args) {
        HashMap<String, Class<?>> keyClass = new HashMap<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(DIR + this.type.getName());
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
                String content = bufferedReader.readLine();
                String[] split = content.split("=");
                if (split.length != 2) {
                    continue;
                }
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.isEmpty() || value.isEmpty()) {
                    continue;
                }
                keyClass.put(key, classLoader.loadClass(value));
                if (!keyClass.containsKey(name)) {
                    throw new HrpcRuntimeException("找不到对应的类，请检查是否写入配置文件");
                }
                List<Class<?>> argsTypes = new ArrayList<>();
                for (Object arg : args) {
                    argsTypes.add(arg.getClass());
                }
                Class<?>[] array = argsTypes.toArray(new Class<?>[]{});
                return keyClass.get(name).getConstructor(array).newInstance(args);
            }
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
        throw new HrpcRuntimeException("运行错误");
    }
}
