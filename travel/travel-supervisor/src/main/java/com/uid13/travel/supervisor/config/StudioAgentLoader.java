package com.uid13.travel.supervisor.config;

import com.alibaba.cloud.ai.agent.studio.loader.AbstractAgentLoader;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Studio AgentLoader —— 仅暴露 {@link LlmRoutingAgent}（主管 Agent），
 * 并用内联 {@link StreamingFixedAgent} 修复 A2A 远程流式事件兼容性问题。
 *
 * <h3>背景</h3>
 * {@code spring-ai-alibaba-agent-framework} 中 {@code A2aNodeActionWithConfig}
 * 创建 {@link StreamingOutput} 时使用了未设置 {@link OutputType} 的四参数构造器，
 * 导致所有经 A2A 协议传输的流式事件 {@code getOutputType()} 为 null、{@code message()} 为 null，
 * 文本仅存在于 {@code chunk()}。
 * <p>
 * Studio 的 {@code ExecutionController} 在处理 SSE 事件时只读取 {@code message()}，
 * 不看 {@code chunk()}，导致前端收到空 {@code data:{} }。
 *
 * <h3>修复方式</h3>
 * {@link StreamingFixedAgent} 拦截 {@link Agent#stream(UserMessage, RunnableConfig)}，
 * 遍历流事件，将 {@code chunk} 中文本提升为 {@link AssistantMessage}，
 * 确保 {@code OutputType#AGENT_MODEL_STREAMING} 事件携带有效 {@code message}。
 *
 * <h3>参考</h3>
 * <ul>
 *   <li>上游 Issue：<a href="https://github.com/alibaba/spring-ai-alibaba/issues/4760">#4760</a></li>
 * </ul>
 *
 * @author uid13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudioAgentLoader extends AbstractAgentLoader {

    private final LlmRoutingAgent supervisorAgent;

    @Override
    protected Map<String, Agent> loadAgentMap() {
        log.info("StudioAgentLoader: wrapping LlmRoutingAgent '{}'", supervisorAgent.name());
        return Map.of(supervisorAgent.name(), new StreamingFixedAgent(supervisorAgent));
    }

    /**
     * 流式兼容包装器 —— 将 A2A 远程事件中 {@code chunk} 文本提升为 {@link AssistantMessage}。
     * <p>
     * 同时处理以下两种情况：
     * <ul>
     *   <li>{@code ot == null && chunk 有值} —— A2A 远程事件（最常见的场景）</li>
     *   <li>{@code ot == AGENT_MODEL_STREAMING && chunk 有值 && message == null} —— 框架内部事件</li>
     * </ul>
     * 并将 {@code AGENT_MODEL_FINISHED} 转为 {@code AGENT_MODEL_STREAMING}，
     * 防止被 studio 的 {@code ExecutionController} 过滤器跳过。
     */
    @Slf4j
    private static class StreamingFixedAgent extends Agent {
        private final Agent delegate;

        StreamingFixedAgent(Agent delegate) {
            this.delegate = delegate;
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public String description() {
            return delegate.description();
        }

        @Override
        public StateGraph getGraph() {
            return delegate.getGraph();
        }

        @Override
        public synchronized com.alibaba.cloud.ai.graph.CompiledGraph getAndCompileGraph() {
            return delegate.getAndCompileGraph();
        }

        @Override
        protected StateGraph initGraph() throws GraphStateException {
            return delegate.getGraph();
        }

        @Override
        public Flux<NodeOutput> stream(UserMessage userMessage, RunnableConfig config)
                throws GraphRunnerException {
            return fixStream(delegate.stream(userMessage, config));
        }

        @Override
        public Flux<NodeOutput> stream(String message, RunnableConfig config)
                throws GraphRunnerException {
            return fixStream(delegate.stream(message, config));
        }

        private Flux<NodeOutput> fixStream(Flux<NodeOutput> upstream) {
            return upstream.map(nodeOutput -> {
                if (!(nodeOutput instanceof StreamingOutput<?> so)) {
                    return nodeOutput;
                }
                OutputType ot = so.getOutputType();
                Message msg = so.message();
                String chunk = so.chunk();

                if (chunk != null && !chunk.isEmpty() && msg == null) {
                    return new StreamingOutput<>(new AssistantMessage(chunk),
                            so.node(), so.agent(), so.state(), so.tokenUsage(),
                            OutputType.AGENT_MODEL_STREAMING);
                }
                if (ot == OutputType.AGENT_MODEL_FINISHED && msg != null) {
                    return new StreamingOutput<>(msg,
                            so.node(), so.agent(), so.state(), so.tokenUsage(),
                            OutputType.AGENT_MODEL_STREAMING);
                }
                return nodeOutput;
            });
        }
    }
}
