package domain.agent.service.armory.factory.element;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义 RAG（检索增强生成）顾问===================================详细描述了rag检索是如何实现的
 * 当前状态：原生单路向量检索（已保留高级重排逻辑注释）
 */
public class RagAnswerAdvisor implements BaseAdvisor {

    private final Logger log = LoggerFactory.getLogger(RagAnswerAdvisor.class);

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final String userTextAdvise;

    /* 🔥 高级功能预留：双路检索器
    private final AdvancedKnowledgeRetriever advancedRetriever;
    private final double THRESHOLD = 0.80;
    */

    public RagAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        /* 如果以后要启用高级检索器，构造函数需要增加参数
        this.advancedRetriever = advancedRetriever;
        */
        this.userTextAdvise = "\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n";
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        HashMap<String, Object> context = new HashMap<>(chatClientRequest.context());
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        String query = (new PromptTemplate(userText)).render();
        Filter.Expression filter = doGetFilterExpression(context);

        // ============================================================
        // 🚀 检索逻辑开始
        // ============================================================

        // 【模式 1：原生向量检索】- 目前生效
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest)
                .query(query)
                .filterExpression(filter)
                .build();
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);

        /* 【模式 2：高级双路召回 + 重排 + 阈值拦截】- 已注释保留
        List<Document> documents = this.advancedRetriever.searchAndRerank(query, searchRequest.getTopK(), filter);

        if (documents == null || documents.isEmpty()) {
            return chatClientRequest;
        }

        Object topScoreObj = documents.get(0).getMetadata().get("final_rerank_score");
        double topScore = topScoreObj != null ? ((Number) topScoreObj).doubleValue() : 0.0;

        if (topScore < THRESHOLD) {
            log.warn("RAG 最高得分 ({}) 低于及格线，取消增强。", topScore);
            return chatClientRequest;
        }
        */

        // ============================================================

        // 如果没有找任何东西，直接放行
        if (documents == null || documents.isEmpty()) {
            return chatClientRequest;
        }

        context.put("qa_retrieved_documents", documents);
        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        String advisedUserText = userText + System.lineSeparator() + this.userTextAdvise;
        Map<String, Object> advisedUserParams = new HashMap<>(chatClientRequest.context());
        advisedUserParams.put("question_answer_context", documentContext);

        // 3. 🔥 真正的“喂药”动作在这里：
        return ChatClientRequest.builder()
                .prompt(Prompt.builder()
                        .messages(new UserMessage(advisedUserText),// 你的问题 + 提示模板
                                new AssistantMessage(JSON.toJSONString(advisedUserParams)))
                        .build())
                .context(advisedUserParams)
                .build();
    }

    //这个纯是打扫生成的信息，然后加入原空间相当于将回答标准化格式
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 安全判断，防止熔断时 context 里没东西
        Object retrievedDocs = chatClientResponse.context().get("qa_retrieved_documents");
        if (retrievedDocs == null) {
            return chatClientResponse;
        }

        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        chatResponseBuilder.metadata("qa_retrieved_documents", retrievedDocs);
        ChatResponse chatResponse = chatResponseBuilder.build();

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return this.after(callAdvisorChain.nextCall(this.before(chatClientRequest, callAdvisorChain)), callAdvisorChain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return BaseAdvisor.super.adviseStream(chatClientRequest, streamAdvisorChain);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString())
                ? (new FilterExpressionTextParser()).parse(context.get("qa_filter_expression").toString())
                : this.searchRequest.getFilterExpression();
    }
}