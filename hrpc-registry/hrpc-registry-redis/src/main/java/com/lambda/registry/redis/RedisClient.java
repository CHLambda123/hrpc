package com.lambda.registry.redis;

import com.lambda.common.exception.HrpcRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RedisClient {
    private final JedisPool jedisPool;
    private final String eventPrefix = "__keyevent@*__:serviceEvent";
    public RedisClient(String host, int port, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(8);
        config.setMaxTotal(18);
        jedisPool = new JedisPool(config, host, port, 2000, password);
    }

    public Map<String, String> getAllKeysAndValues(String prefix) {
        Jedis jedis = jedisPool.getResource();
        Set<String> keys = jedis.keys(prefix + "*");
        Map<String, String> res = new HashMap<>(64);
        for (String key : keys) {
            String value = jedis.get(key);
            if (StringUtils.isNotEmpty(value)) {
                res.put(key, value);
            }
        }
        jedis.close();
        return res;
    }

    public void deleteKey(String key) {
        Jedis jedis = jedisPool.getResource();
        jedis.del(key);
        jedis.close();
    }

    public String getValueByKey(String key) {
        Jedis jedis = jedisPool.getResource();
        String res = jedis.get(key);
        jedis.close();
        return res;
    }

    public void setValue(String key, String value) {
        if (key == null || value == null) {
            return;
        }
        Jedis jedis = jedisPool.getResource();
        jedis.set(key, value);
        jedis.close();
    }

    public void publishEvent(String channel, String message) {
        Jedis jedis = jedisPool.getResource();
        channel = eventPrefix + channel;
        jedis.publish(channel, message);
        jedis.close();
    }

    public boolean doKeyPub(String eventName, String message) {
        try {
            publishEvent(eventName, message);
        } catch (Exception e) {
            log.error("error happen: {}", e);
            return false;
        }
        return true;
    }
    public void setDamonThreadForKeyPub(String eventName, Map<String, String> map) {
        String threadName = "damonThreadForKeyPub";
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (threadName.equals(thread.getName())) {
                return;
            }
        }
        Thread thread = new Thread(()->{
            while (true) {
                for (String key : map.keySet()) {
                    while (!doKeyPub(eventPrefix + eventName, key)) {
                        // ignore
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new HrpcRuntimeException(e);
                }
            }
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }
    public void setDamonThreadForKeySub(OnPMessageHandler onPMessageHandler) {
        String threadName = "damonThreadForKeySub";
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (threadName.equals(thread.getName())) {
                return;
            }
        }
        Thread thread = new Thread(()->{
            while (!this.doSubscribe(onPMessageHandler)) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new HrpcRuntimeException(e);
                }
            }
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private Boolean doSubscribe(OnPMessageHandler onPMessageHandler) {
        try {
            Jedis jedis = jedisPool.getResource();
            String keyExpiredEventPattern = eventPrefix + "*";
            jedis.psubscribe(new JedisPubSub() {
                @Override
                public void onPMessage(String pattern, String channel, String message) {
                    onPMessageHandler.apply(channel, message);
                }
            }, keyExpiredEventPattern);
        } catch (Exception e) {
            log.error("error happen: {}", e);
            return false;
        }
        return true;
    }

    public interface OnPMessageHandler {
        void apply(String channel, String message);
    }
}
