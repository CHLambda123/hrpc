package com.lambda.registry.redis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lambda.common.entity.RegistryInfo;
import com.lambda.registry.common.Registry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisRegistry implements Registry {
    private final RedisClient redisClient;

    private final Cache<String, RegistryInfo> serviceCache;

    public final Map<String, String> providerCache;

    private static final int MAX_SERVICE_COUNT = 100;

    private static final String SERVICE_ROOT = "serviceRoot";
    private static final String REGISTER = "register";
    private static final String UN_REGISTER = "unRegister";

    private static final int KEY_EXPIRED_PERIOD = 2;

    public RedisRegistry(String host, String port, String password) {
        redisClient = new RedisClient(host, Integer.parseInt(port), password);
        serviceCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SERVICE_COUNT)
                .expireAfterWrite(KEY_EXPIRED_PERIOD, TimeUnit.SECONDS)
                .build();
        providerCache = new ConcurrentHashMap<>(64);
        redisClient.setDamonThreadForKeySub(this::handleServiceEvent);
        redisClient.setDamonThreadForKeyPub(REGISTER, providerCache);
    }

    private void handleServiceEvent(String channel, String message) {
        log.info("channel: {}, message: {}", channel, message);
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (channel.endsWith(REGISTER)) {
            handleServiceRegisterEvent(message);
        } else if (channel.endsWith(UN_REGISTER)) {
            handleServiceUnregisterEvent(message);
        }
    }

    private void handleServiceUnregisterEvent(String key) {
        serviceCache.invalidate(key);
    }

    private RegistryInfo loadRegistryInfo(String key, String value) {
        String[] split = key.split(":");
        if (split.length != 5) {
            return null;
        }
        int weight = StringUtils.isEmpty(value)?1:Integer.parseInt(value);
        return new RegistryInfo(split[1], split[2], split[3], split[4], weight);
    }

    private void handleServiceRegisterEvent(String key) {
        String value = this.redisClient.getValueByKey(key);
        if (StringUtils.isNumeric(value)) {
            RegistryInfo registryInfo = loadRegistryInfo(key, value);
            if (registryInfo == null) {
                return;
            }
            serviceCache.put(key, registryInfo);
        }
    }

    private String splicingKey(RegistryInfo registryInfo) {
        return SERVICE_ROOT + ":" +
                registryInfo.getServiceName() + ":" +
                registryInfo.getVersion() + ":" +
                registryInfo.getHost() + ":" +
                registryInfo.getPort();
    }

    @Override
    public void registService(RegistryInfo registryInfo) {
        String key = splicingKey(registryInfo);
        String weightStr = String.valueOf(registryInfo.getWeight());
        redisClient.setValue(key, weightStr);
        providerCache.put(key, weightStr);
    }

    @Override
    public void unregister(RegistryInfo registryInfo) {
        String key = splicingKey(registryInfo);
        providerCache.remove(key);
        redisClient.publishEvent(UN_REGISTER, splicingKey(registryInfo));
    }

    @Override
    public List<RegistryInfo> getServices(String serviceName, String version) {
        ConcurrentMap<String, RegistryInfo> map = serviceCache.asMap();
        Set<String> keySet = map.keySet();
        ArrayList<RegistryInfo> registryInfos = new ArrayList<>();
        for (String key : keySet) {
            if (!key.startsWith(SERVICE_ROOT + ":" + serviceName + ":" + version)) {
                continue;
            }
            RegistryInfo registryInfo = serviceCache.getIfPresent(key);
            if (registryInfo != null) {
                registryInfos.add(registryInfo);
            }
        }
        return registryInfos;
    }

}
