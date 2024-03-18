package com.lambda.registry.zookeeper;

import com.lambda.common.exception.HrpcRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheBridge;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZkClient {
    private final CuratorFramework client;
    public static final String NODE_CREATED  = "NODE_CREATED";
    public static final String NODE_CHANGED  = "NODE_CHANGED";
    public static final String NODE_DELETED  = "NODE_DELETED";

    public ZkClient(String zkUrl) {
        this(zkUrl, null);
    }

    public ZkClient(String zkUrl, String authStr) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(zkUrl)
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(30000)
                .sessionTimeoutMs(60000);
        if (StringUtils.isNotEmpty(authStr)) {
            builder.authorization("digest", authStr.getBytes());
            builder.aclProvider(new ACLProvider() {
                @Override
                public List<ACL> getDefaultAcl() {
                    return ZooDefs.Ids.CREATOR_ALL_ACL;
                }

                @Override
                public List<ACL> getAclForPath(String s) {
                    return ZooDefs.Ids.CREATOR_ALL_ACL;
                }
            });
        }
        this.client = builder.build();
        this.client.start();
        try {
            boolean connected = client.blockUntilConnected(10000, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new HrpcRuntimeException("fail to connect to zookeeper");
            }
        } catch (InterruptedException e) {
            throw new HrpcRuntimeException(e);
        }

    }

    public void createPersistent(String path) {
        try {
            client.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException ignore) {
            log.warn("{}节点已经存在", path);
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }

    public void createEphemeral(String path, String data) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, data.getBytes(StandardCharsets.UTF_8));
        } catch (KeeperException.NodeExistsException e) {
            deletePath(path);
            createEphemeral(path, data);
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }

    public void deletePath(String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
            log.warn("{}节点不存在", path);
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }

    public List<String> getChildrenNodes(String path, boolean tolerateFault) {
        try {
            return client.getChildren().forPath(path);
        } catch (Exception e) {
            if (tolerateFault) {
                return new ArrayList<>();
            } else {
                throw new HrpcRuntimeException(e);
            }
        }
    }

    public String getNodeData(String path, boolean tolerateFault) {
        try {
            return new String(client.getData().forPath(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (tolerateFault) {
                return null;
            } else {
                throw new HrpcRuntimeException(e);
            }
        }
    }

    public void wathServicesChange(String path, ServicesChangeHandle servicesChangeHandle) {
        CuratorCacheBridge curatorCacheBridge = CuratorCache.bridgeBuilder(client, path).build();
        curatorCacheBridge.listenable().addListener((type, oldData, newData) -> {
            String typeStr = null;
            if (type != null) {
                typeStr = type.toString();
            }
            String oldPathStr = null;
            if (oldData != null) {
                oldPathStr = oldData.getPath();
            }
            String newPathStr = null;
            if (newData != null) {
                newPathStr = newData.getPath();
            }
            String newDataStr = null;
            if (newData != null && newData.getData() != null) {
                newDataStr = new String(newData.getData(), StandardCharsets.UTF_8);
            }
            servicesChangeHandle.apply(typeStr, oldPathStr, newPathStr, newDataStr);
        });
        curatorCacheBridge.start();
    }

    public interface ServicesChangeHandle {
        void apply(String typeStr, String oldPathStr, String newPathStr, String newDataStr);
    }

}
