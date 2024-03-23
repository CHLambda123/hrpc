package com.lambda.hrpc.registry.zookeeper;

import com.lambda.hrpc.common.entity.RegistryInfo;
import com.lambda.hrpc.registry.common.Registry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ZookeeperRegistry implements Registry {
    private ZkClient zkClient;

    // service根目录
    static private final String SERVICE_ROOT = "serviceRoot";

    // service缓存
    private final Map<String, Map<String, Map<String, RegistryInfo>>> serviceCache;

    public ZookeeperRegistry(String zkUrl) {
        this.zkClient = new ZkClient(zkUrl);
        serviceCache = new HashMap<>(64);
        // 注册监听
        zkClient.wathServicesChange(splicingPath(SERVICE_ROOT), this::handleWithServiceChanged);
    }

    /**
     * 监听到zk有变更，更新services缓存
     */
    public void handleWithServiceChanged(String typeStr, String oldPathStr, String newPathStr, String newDataStr) {
        if (ZkClient.NODE_CREATED.equals(typeStr)) {
            this.handleNodeCreated(newPathStr, newDataStr);
        } else if (ZkClient.NODE_DELETED.equals(typeStr)) {
            this.handleNodeDeleted(oldPathStr);
        } else {
            log.warn("unexpected node state, typeStr: {}, oldPathStr: {}, newPathStr: {}, newDataStr: {}",
                    typeStr, oldPathStr, newPathStr, newDataStr);
        }
    }

    /**
     * 监听到节点删除，对应服务下线
     */
    private void handleNodeDeleted(String oldPathStr) {
        if (StringUtils.isEmpty(oldPathStr) || !oldPathStr.startsWith("/")) {
            log.error("illegal path for zk node delete, oldPath: {}", oldPathStr);
            return;
        }
        String[] split = oldPathStr.split("/");
        if (split.length <= 2) {
            return;
        }
        synchronized (serviceCache) {
            if (split.length == 3) {
                serviceCache.remove(split[2]);
            } else if (split.length == 4) {
                if (serviceCache.containsKey(split[2])) {
                    serviceCache.get(split[2]).remove(split[3]);
                }
            } else if (split.length == 5) {
                serviceCache.get(split[2]).get(split[3]).remove(split[4]);
            } else {
                log.error("illegal path for zk node delete, oldPath: {}", oldPathStr);
            }
        }
    }

    /**
     * 监听到节点创建，对应服务上线
     */
    private void handleNodeCreated(String newPathStr, String newDataStr) {
        if (StringUtils.isEmpty(newPathStr) || !newPathStr.startsWith("/")) {
            log.error("illegal path for zk node create, newPath: {}", newPathStr);
            return;
        }
        String[] split = newPathStr.split("/");
        if (split.length != 5) {
            return;
        }
        if (StringUtils.isNumeric(newDataStr)) {
            RegistryInfo registryInfo = this.parseFullPath(newPathStr);
            int weight = Integer.parseInt(newDataStr);
            registryInfo.setWeight(weight);
            synchronized (serviceCache) {
                serviceCache.computeIfAbsent(split[2], s -> new HashMap<>())
                        .computeIfAbsent(split[3], s -> new HashMap<>())
                        .put(split[4], registryInfo);
            }
        } else {
            log.error("weight hasn't set, ignore it. pathStr: {}", newPathStr);
        }
    }

    /**
     * 将服务全路径映射到RegistryInfo对象
     */
    private RegistryInfo parseFullPath(String path) {
        String[] split = path.split("/");
        String[] hostAndPort = split[4].split(":");
        return new RegistryInfo(split[2], split[3], hostAndPort[0], hostAndPort[1]);
    }

    /**
     * 将各个节点合并为zk目录
     */
    private String splicingPath(String... nodes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String node : nodes) {
            if (StringUtils.isEmpty(node)) {
                continue;
            }
            // 去除头尾的/符号
            String nodeRes = node.replaceAll("^/*|/*$", "");
            if (StringUtils.isNotEmpty(nodeRes)) {
                stringBuilder.append("/").append(nodeRes);
            }
        }
        return stringBuilder.toString();
    }

    private String splicingHostAndPort(String host, String port) {
        return host + ":" + port;
    }

    @Override
    public void registService(RegistryInfo registryInfo) {
        String persisNode = splicingPath(SERVICE_ROOT, registryInfo.getServiceName(), registryInfo.getVersion());
        zkClient.createPersistent(persisNode);
        String ephemNode = splicingPath(persisNode,
                splicingHostAndPort(registryInfo.getHost(), registryInfo.getPort()));
        zkClient.createEphemeral(ephemNode, String.valueOf(registryInfo.getWeight()));
    }

    @Override
    public void unregister(RegistryInfo registryInfo) {
        String ephemNode = splicingPath(SERVICE_ROOT, registryInfo.getServiceName(), registryInfo.getVersion(),
                splicingHostAndPort(registryInfo.getHost(), registryInfo.getPort()));
        zkClient.deletePath(ephemNode);
    }

    @Override
    public List<RegistryInfo> getServices(String serviceName, String version) {
        Map<String, Map<String, RegistryInfo>> mapTemp = Optional.ofNullable(this.serviceCache.get(serviceName)).orElse(new HashMap<>());
        return new ArrayList<>(mapTemp.get(version).values());
    }
}
