package com.ruyuan.eshop.common.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * </p>
 *
 * @author zhonghuashishan
 */
public class RedisLock {

    RedissonClient redissonClient;

    public RedisLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 互斥锁，seconds秒后自动失效
     * @param key
     * @param seconds
     */
    public boolean lock(String key, int seconds) {
        RLock rLock = redissonClient.getLock(key);
        if (rLock.isLocked()) {
            return false;
        }
        rLock.lock(seconds, TimeUnit.SECONDS);
        return true;
    }

    /**
     * 互斥锁，自动续期
     *
     * @param key
     */
    public boolean lock(String key) {
        RLock rLock = redissonClient.getLock(key);
        if (rLock.isLocked()) {
            return false;
        }
        rLock.lock();
        return true;
    }

    /**
     * 手动释放锁
     *
     * @param key
     */
    public void unlock(String key) {
        RLock rLock = redissonClient.getLock(key);
        if (rLock.isLocked()) {
            rLock.unlock();
        }
    }

}
