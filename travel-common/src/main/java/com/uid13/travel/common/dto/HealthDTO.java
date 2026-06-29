package com.uid13.travel.common.dto;

import com.alibaba.cola.dto.DTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 健康检查响应数据传输对象
 * 封装服务健康状态、服务名称和版本信息
 *
 * @author uid13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HealthDTO extends DTO {

    /** 服务健康状态 */
    private String status;

    /** 服务名称 */
    private String service;

    /** 服务版本 */
    private String version;
}
