//package domain.agent.service.armory.factory.element.advisorTool;
//
//import org.springframework.ai.document.Document;
//import java.util.List;
//
///**
// * 顾问工具箱总接口
// */
//public interface IAdvisor {
//
//    // 1. 关键词检索接口 (Sparse Retrieval)
//    interface ISparseSearcher {
//        List<Document> search(String query, int size);
//    }
//
//    // 2. 文档重排接口 (Reranking)
//    interface IDocumentReranker {
//        List<Document> rerank(String query, List<Document> documents);
//    }
//}

package domain.agent.service.armory.factory.element.advisorTool;

import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * AI 顾问（插件）动态装配器接口
 * 作用：配合数据库配置，动态构建 Spring AI 所需的各类 Advisor 实例
 */
public interface IAdvisor {

    /**
     * 根据数据库中的扩展参数，构建具体的 Advisor 实例
     *
     * @param extParam 数据库中 ai_client_advisor 表的 ext_param 字段（JSON 格式字符串）
     * @return 返回 Spring AI 原生支持的 Advisor 实例
     */
    Advisor buildAdvisor(String extParam);

}