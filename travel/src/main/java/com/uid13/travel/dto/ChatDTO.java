package com.uid13.travel.dto;

import com.alibaba.cola.dto.DTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天响应数据传输对象
 * 封装 Agent 回复的消息内容、Agent 标识和会话 ID
 *
 * @author uid13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatDTO extends DTO {

    /** Agent 回复的文本内容 */
    private String message;

    /** Agent 标识 */
    private String agent;

    /** 会话 ID，用于多轮对话上下文关联 */
    private String threadId;
}
