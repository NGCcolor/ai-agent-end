package domain.agent.model.valobj;

import domain.agent.service.armory.factory.element.VectorStoreMemoryAdvisor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 顾问配置，值对象
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientAdvisorVO {

    /**
     * 顾问ID
     */
    private String advisorId;

    /**
     * 顾问名称
     */
    private String advisorName;

    /**
     * 顾问类型(PromptChatMemory/RagAnswer/SimpleLoggerAdvisor等)
     */
    private String advisorType;

    /**
     * 顺序号
     */
    private Integer orderNum;

    /**
     * 扩展；记忆
     */
    private ChatMemory chatMemory;

    /**
     * 扩展；rag 问答
     */
    private RagAnswer ragAnswer;
    // 🌟 新增：长期向量记忆的扩展属性坑位
    private VectorStoreMemory vectorStoreMemory;

    // 🌟 新增：长期记忆专属参数类
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VectorStoreMemory {
        private int topK = 5;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatMemory {
        private int maxMessages;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RagAnswer {
        private int topK = 4;
        private String filterExpression;
    }

}
