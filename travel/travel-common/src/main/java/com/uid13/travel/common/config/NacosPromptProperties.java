package com.uid13.travel.common.config;

import com.alibaba.nacos.api.PropertyKeyConst;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Nacos Prompt 管理配置属性
 * 用于连接 Nacos AI 注册中心获取 Agent 提示词
 * 
 * @author uid13
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nacos.prompt")
public class NacosPromptProperties {

    /** Nacos Server 地址 */
    private String serverAddr = "127.0.0.1:8848";

    /** Nacos 命名空间 */
    private String namespace = "public";

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /**
     * 转换为 Nacos SDK 所需的 Properties 对象
     *
     * @return Nacos 连接属性
     */
    public Properties toNacosProperties() {
        Properties props = new Properties();
        props.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
        if (namespace != null) {
            props.setProperty(PropertyKeyConst.NAMESPACE, namespace);
        }
        if (username != null) {
            props.setProperty(PropertyKeyConst.USERNAME, username);
        }
        if (password != null) {
            props.setProperty(PropertyKeyConst.PASSWORD, password);
        }
        return props;
    }
}
