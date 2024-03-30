package com.lambda.hrpc.common.spi;

import com.lambda.hrpc.common.annotation.FieldName;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            return (ExtensionLoader<T>) EXTENSION_LOADERS.computeIfAbsent(type, s -> new ExtensionLoader<T>(type));
        }
    }

    public T getExtension(String name, Map<String, Object> argsMap) {
        Object obj = cachedInstances.computeIfAbsent(name, s -> getImpl(s, argsMap));
        return (T) obj;
    }

    private Object getImpl(String name, Map<String, Object> argsMap) {
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
                if (argsMap == null || argsMap.isEmpty()) {
                    return keyClass.get(name).getConstructor().newInstance();
                }
                Constructor<?>[] constructors = keyClass.get(name).getConstructors();
                for (Constructor<?> constructor : constructors) {
                    try {
                        if (constructor.getParameterCount() != argsMap.size()) {
                            continue;
                        }
                        List<Object> args = new ArrayList<>();
                        for (Parameter parameter : constructor.getParameters()) {
                            args.add(argsMap.get(parameter.getAnnotation(FieldName.class).value()));
                        }
                        return constructor.newInstance(args.toArray());
                    } catch (Exception ige) {
                        continue;
                    }
                }
                throw new HrpcRuntimeException("no constructor exist");
            }
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
        throw new HrpcRuntimeException("运行错误");
    }
}
