package cn.bugstack.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 知识入库指令实体
 * 作用：汇集了数据库配置参数与待上传的物理内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIngestionEntity {

    // ================= 数据库关联参数 (用于查配置) =================

    /** 客户端ID：关联 ai_client 表，用于锁定该 Agent 所有的私有配置 */
    private String clientId;

    // ================= 策略执行参数 (从数据库配置表里捞出来的) =================

    /** 嵌入模型名称：从 ai_client_model 表读出，如 "text-embedding-ada-002" */
    private String embeddingModel;

    /** 分词策略配置：从 ai_client_config 读出，比如单段最大 Token 数 (maxTokens) */
    private Integer tokenSplitSize;

    /** 重叠长度：分词时的 overlap，防止语义在截断处丢失 */
    private Integer tokenOverlapSize;

    // ================= 物理数据参数 (实际要存的内容) =================

    /** 知识库分类标签：对应你 Controller 里的 ragTag，写入 Metadata 用于隔离 */
    private String ragTag;

    /** 文档正文：解析后的纯文本 */
    private String content;

    /** 扩展元数据：除了 ragTag 外，可能还有上传人、时间、文件名等 */
    private Map<String, Object> metadata;

}