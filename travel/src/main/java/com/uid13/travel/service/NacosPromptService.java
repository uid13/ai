package com.uid13.travel.service;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.model.prompt.Prompt;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Nacos Prompt 服务
 * 通过 Nacos AI SDK 从 AI 注册中心 Prompt 管理获取 Agent 提示词
 * 在多 Agent 编排中，为 Supervisor 和 TravelAgent 提供各自的系统提示词
 *
 * @author uid13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NacosPromptService {

    private final NacosMcpProperties nacosMcpProperties;
    private AiService aiService;

    @PostConstruct
    public void init() throws NacosException {
        this.aiService = AiFactory.createAiService(nacosMcpProperties.getNacosProperties());
    }

    /**
     * 根据 Prompt Key 获取提示词内容（最新版本）
     *
     * @param promptKey Prompt 键
     * @return 提示词内容（Markdown 格式）
     */
    public String getPrompt(String promptKey) {
        try {
            Prompt prompt = aiService.getPrompt(promptKey);
            if (prompt == null || prompt.getTemplate() == null) {
                throw new RuntimeException("Prompt 未找到：" + promptKey);
            }
            return prompt.getTemplate();
        } catch (NacosException e) {
            throw new RuntimeException("获取 Nacos Prompt 失败：" + promptKey, e);
        }
    }

    /**
     * 根据 Prompt Key 和版本获取提示词内容
     *
     * @param promptKey Prompt 键
     * @param version   版本号
     * @return 提示词内容（Markdown 格式）
     */
    public String getPrompt(String promptKey, String version) {
        try {
            Prompt prompt = aiService.getPromptByVersion(promptKey, version);
            if (prompt == null || prompt.getTemplate() == null) {
                throw new RuntimeException("Prompt 未找到：" + promptKey + " v" + version);
            }
            return prompt.getTemplate();
        } catch (NacosException e) {
            throw new RuntimeException("获取 Nacos Prompt 失败：" + promptKey + " v" + version, e);
        }
    }
}
