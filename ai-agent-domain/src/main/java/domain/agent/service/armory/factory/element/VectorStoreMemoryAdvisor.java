package domain.agent.service.armory.factory.element; // 🌟 修复 1：修正包名路径

import com.alibaba.fastjson.JSON;
import domain.agent.service.armory.factory.element.advisorTool.IAdvisor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手搓版：向量长期记忆装配器 (带高级语义切割 & 全版本兼容)
 */
@Component("VectorStoreChatMemoryAdvisor")
public class VectorStoreMemoryAdvisor implements IAdvisor {

    @Resource
    private VectorStore vectorStore;

    @Override
    public Advisor buildAdvisor(String extParam) {
        int defaultTopK = 5;
        try {
            if (extParam != null && !extParam.isEmpty()) {
                defaultTopK = JSON.parseObject(extParam).getInteger("topK");
            }
        } catch (Exception e) {
            // 解析失败则使用默认值
        }
        return new CustomVectorMemoryAdvisor(vectorStore, defaultTopK);
    }

    public static class CustomVectorMemoryAdvisor implements BaseAdvisor {

        private final VectorStore vectorStore;
        private final int topK;
        private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

        // 实例化语义切割器
        private final TokenTextSplitter textSplitter = new TokenTextSplitter();

        public CustomVectorMemoryAdvisor(VectorStore vectorStore, int topK) {
            this.vectorStore = vectorStore;
            this.topK = topK;
        }

        /***、
         * 根据id去向量数据库中查询五条相关的，然后一块喂给大模型
         * */
        @Override
        public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
            Map<String, Object> context = new HashMap<>(request.context());
            String conversationId = (String) context.get(CONVERSATION_ID_KEY);
            String userText = request.prompt().getUserMessage().getText();

            context.put("current_user_text", userText);

            if (conversationId == null || conversationId.isEmpty()) {
                // 🌟 修复 3：解决 from() 报错，手动传递 prompt
                return ChatClientRequest.builder()
                        .prompt(request.prompt())
                        .context(context)
                        .build();
            }

            String filterStr = "conversation_id == '" + conversationId + "'";

            // 🌟 修复 2：解决 query() 报错，使用经典 defaults() 构造方式
            // ✅ 兼容你版本的 Builder 模式
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userText)
                    .topK(topK)
                    .filterExpression(new FilterExpressionTextParser().parse(filterStr))
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            if (documents == null || documents.isEmpty()) {
                // 🌟 修复 3：同上
                return ChatClientRequest.builder()
                        .prompt(request.prompt())
                        .context(context)
                        .build();
            }

            String history = documents.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
            String advisedText = "【长期历史记忆参考】:\n" + history + "\n\n【当前最新任务】:\n" + userText;

            // 🌟 修复 3：同上，使用重构后的 prompt
            return ChatClientRequest.builder()
                    .prompt(new Prompt(new UserMessage(advisedText)))
                    .context(context)
                    .build();
        }

        /**、
         * 把刚才用户的提问（User）和大模型的回答（Assistant）拼成一整块文本。然后语义切割放到向量数据库中
         * */

        @Override
        public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
            Map<String, Object> context = response.context();
            String conversationId = (String) context.get(CONVERSATION_ID_KEY);
            String userText = (String) context.get("current_user_text");

            // 🌟 修复 4：解决 getContent() 报错，改为旧版 API 的 getText()
            String assistantText = response.chatResponse().getResult().getOutput().getText();

            if (conversationId != null && userText != null && assistantText != null) {
                String memoryContent = "之前探讨的内容 -> 任务: " + userText + "\n你的输出: " + assistantText;

                Document doc = new Document(memoryContent, Map.of("conversation_id", conversationId));

                // 执行语义切割并存入向量库
                List<Document> splitDocs = textSplitter.apply(List.of(doc));
                vectorStore.add(splitDocs);
            }

            return response;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            return this.after(chain.nextCall(this.before(request, chain)), chain);
        }

        @Override
        public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
            return BaseAdvisor.super.adviseStream(request, chain);
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String getName() {
            return "CustomVectorMemoryAdvisor";
        }
    }
}