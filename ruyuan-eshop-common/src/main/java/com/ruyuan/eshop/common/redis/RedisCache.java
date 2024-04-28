package com.ruyuan.eshop.common.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * </p>
 *
 * @author zhonghuashishan
 */
public class RedisCache {

    private RedisTemplate redisTemplate;

    public RedisCache(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存存储
     * @param key
     * @param value
     * @param seconds 自动失效时间
     */
    public void set(String key, String value, int seconds){

        ValueOperations<String,String> vo = redisTemplate.opsForValue();
        if(seconds > 0){
            vo.set(key, value, seconds, TimeUnit.SECONDS);
        }else{
            vo.set(key, value);
        }
    }

    /**
     * 缓存获取
     * @param key
     * @return
     */
    public String get(String key){
        ValueOperations<String,String> vo = redisTemplate.opsForValue();
        return vo.get(key);
    }

    /**
     * 缓存手动失效
     * @param key
     * @return
     */
    public boolean delete(String key){
        return redisTemplate.delete(key);
    }

}
