package com.ruyuan.eshop.tms.api.impl;

import com.ruyuan.eshop.tms.api.TmsApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = TmsApi.class)
public class TmsApiImpl implements TmsApi {

}
