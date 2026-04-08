package domain.agent.service.armory.factory.element;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 对应简历亮点 1：前置 Query 检索引擎优化 (指代消解与意图拆解)
 */
@Component
public class QueryRewriteAdvisor implements BaseAdvisor {

    private final ChatClient fastChatClient;

    public QueryRewriteAdvisor(ChatClient.Builder builder) {
        // 构建一个轻量级的 Client 用于专门做 Query 改写
        this.fastChatClient = builder.build();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        // 1. 获取用户原始提问
        String originalText = request.prompt().getUserMessage().getText();

        // 2. 拷贝并获取上下文 (解决黄线 null 警告)
        Map<String, Object> context = new HashMap<>(request.context());
        String shortTermHistory = (String) context.get("short_term_history");

        // 3. 构建改写 Prompt
        String rewritePrompt = String.format(
                "【历史对话】: %s\n【最新提问】: %s\n请结合历史对话，将最新提问改写为一句独立完整的检索词，不要包含任何多余的解释。",
                shortTermHistory != null ? shortTermHistory : "无",
                originalText
        );

        // 4. 调用小模型进行指代消解和脱水
        String rewrittenQuery = fastChatClient.prompt()
                .user(rewritePrompt)
                .call()
                .content();

        // 5. 防空指针处理 (解决第 49 行 "可能为 null" 的黄线警告)
        if (rewrittenQuery == null || rewrittenQuery.trim().isEmpty()) {
            rewrittenQuery = originalText;
        }

        // 6. 重新构建请求，向下传递 (解决找不到 from() 方法的红线报错)
        return ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage(rewrittenQuery)))
                .context(context)
                .build();
    }

    // ========================================================================
    // 下面是解决 "类必须声明为抽象或实现 after 方法" 报错的核心补全
    // ========================================================================

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        // Query 改写是一个纯前置拦截器，后置不需要做任何处理，直接放行
        return response;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 标准的 Advisor 链式调用模板
        return this.after(chain.nextCall(this.before(request, chain)), chain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return BaseAdvisor.super.adviseStream(request, chain);
    }

    @Override
    public int getOrder() {
        // 设置最高优先级 (-100)，确保改写逻辑在 Memory 和 RAG 之前执行
        return -100;
    }

    @Override
    public String getName() {
        return "QueryRewriteAdvisor";
    }
}