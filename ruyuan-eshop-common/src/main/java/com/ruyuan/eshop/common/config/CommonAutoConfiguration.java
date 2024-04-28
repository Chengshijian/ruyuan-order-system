package com.ruyuan.eshop.common.config;

import com.ruyuan.eshop.common.bean.SpringApplicationContext;
import com.ruyuan.eshop.common.redis.RedisConfig;
import com.ruyuan.eshop.common.web.WebConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Configuration
@Import(value = {WebConfiguration.class, RedisConfig.class, SpringApplicationContext.class})
public class CommonAutoConfiguration {

}