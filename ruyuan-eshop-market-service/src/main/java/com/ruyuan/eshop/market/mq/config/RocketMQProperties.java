package com.ruyuan.eshop.market.mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rocketmq的配置信息
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@ConfigurationProperties(prefix = "rocketmq")
public class RocketMQProperties {

    private String nameServer;

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }
}