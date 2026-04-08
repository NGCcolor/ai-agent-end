package domain.agent.model.valobj.Enums;

// 🚀 注释掉高级检索器引用，防止编译报错
// import cn.bugstack.ai.domain.agent.service.armory.factory.element.AdvancedKnowledgeRetriever;

import domain.agent.model.valobj.AiClientAdvisorVO;
import domain.agent.service.armory.factory.element.RagAnswerAdvisor;
// 🌟 引入咱们手搓的长期记忆插件类（注意检查包路径是否与你本地一致）
import domain.agent.service.armory.factory.element.VectorStoreMemoryAdvisor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 顾问类型枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AiClientAdvisorTypeEnumVO {

    CHAT_MEMORY("ChatMemory", "上下文记忆（内存模式）") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            AiClientAdvisorVO.ChatMemory chatMemory = aiClientAdvisorVO.getChatMemory();
            return PromptChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                            .maxMessages(chatMemory.getMaxMessages())
                            .build()
            ).build();
        }
    },

    RAG_ANSWER("RagAnswer", "知识库") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            AiClientAdvisorVO.RagAnswer ragAnswer = aiClientAdvisorVO.getRagAnswer();

            // 🚀 【原生模式】：直接使用 VectorStore 构造 RagAnswerAdvisor
            return new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                    .topK(ragAnswer.getTopK())
                    .filterExpression(ragAnswer.getFilterExpression())
                    .build());
        }
    },

    // 🌟 新增：长期记忆专属枚举实例
    VECTOR_STORE_MEMORY("VectorStoreChatMemoryAdvisor", "向量长期记忆") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            // 获取解析到的长期记忆参数，提取 topK，如果没有配置默认给 5
            int topK = 5;
            if (aiClientAdvisorVO.getVectorStoreMemory() != null) {
                topK = aiClientAdvisorVO.getVectorStoreMemory().getTopK();
            }
            // 🌟 直接实例化咱们之前手写的拦截器核心逻辑
            return new VectorStoreMemoryAdvisor.CustomVectorMemoryAdvisor(vectorStore, topK);
        }
    }

    ;

    private String code;
    private String info;

    private static final Map<String, AiClientAdvisorTypeEnumVO> CODE_MAP = new HashMap<>();

    static {
        for (AiClientAdvisorTypeEnumVO enumVO : values()) {
            CODE_MAP.put(enumVO.getCode(), enumVO);
        }
    }

    /**
     * 策略方法：创建顾问对象
     * @param aiClientAdvisorVO 顾问配置对象
     * @param vectorStore 向量存储（降级模式使用）
     * @return 顾问对象
     */
    public abstract Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore);

    public static AiClientAdvisorTypeEnumVO getByCode(String code) {
        AiClientAdvisorTypeEnumVO enumVO = CODE_MAP.get(code);
        if (enumVO == null) {
            throw new RuntimeException("err! advisorType " + code + " not exist!");
        }
        return enumVO;
    }

}