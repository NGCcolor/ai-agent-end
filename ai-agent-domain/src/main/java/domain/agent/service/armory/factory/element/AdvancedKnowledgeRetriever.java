//package domain.agent.service.armory.factory.element;
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.filter.Filter;
//import org.springframework.stereotype.Service;
//
//import jakarta.annotation.Resource;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 高级知识检索器 (支持混合检索与重排序)
// * 作用：取代单一的 VectorStore，提供两路召回、合并去重、外部精排打分的能力。
// */
//@Service
//public class AdvancedKnowledgeRetriever {
//
//    @Resource
//    private VectorStore vectorStore;
//
//    /**
//     * 核心检索方法：召回并打分
//     *
//     * @param query            用户问题
//     * @param topK             最终需要的文档数量（比如 4）
//     * @param filterExpression 动态过滤条件
//     * @return 带有最终精排得分的文档列表
//     */
//    public List<Document> searchAndRerank(String query, int topK, Filter.Expression filterExpression) {
//
//        // ==========================================
//        // 1. 召回阶段 (Recall) - 粗排，多捞一点数据
//        // ==========================================
//        int recallSize = topK * 3; // 比如最终要4条，这里先捞12条供排序
//
//        // 1.1 向量召回 (Dense Retrieval)
//        SearchRequest searchRequest = SearchRequest.builder()
//                .query(query)
//                .topK(recallSize)
//                .filterExpression(filterExpression)
//                .build();
//        List<Document> vectorDocs = vectorStore.similaritySearch(searchRequest);
//
//        // 1.2 关键词召回 (Sparse/BM25) - TODO: 这里预留接口，实际可接入 Elasticsearch 或 Milvus
//        List<Document> keywordDocs = mockKeywordSearch(query, recallSize);
//
//        // ==========================================
//        // 2. 合并去重 (Merge & Deduplicate)
//        // ==========================================
//        List<Document> mergedDocs = mergeAndDeduplicate(vectorDocs, keywordDocs);
//        if (mergedDocs.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        // ==========================================
//        // 3. 重排阶段 (Rerank) - LambdaMART / BGE 模型
//        // ==========================================
//        // TODO: 将 mergedDocs 发送给你的外部重排模型 API 进行精准打分
//        List<Document> rerankedDocs = mockLambdaMartRerank(query, mergedDocs);
//
//        // ==========================================
//        // 4. 截断输出 (TopK)
//        // ==========================================
//        return rerankedDocs.stream().limit(topK).collect(Collectors.toList());
//    }
//
//    /**
//     * 合并去重逻辑 (基于文档的 ID 或 Content)
//     */
//    private List<Document> mergeAndDeduplicate(List<Document> list1, List<Document> list2) {
//        Map<String, Document> uniqueDocs = list1.stream()
//                .collect(Collectors.toMap(Document::getId, doc -> doc, (existing, replacement) -> existing));
//
//        for (Document doc : list2) {
//            uniqueDocs.putIfAbsent(doc.getId(), doc);
//        }
//        return new ArrayList<>(uniqueDocs.values());
//    }
//
//    // ----------------------------------------------------------------------
//    // ⚠️ 下面是占位方法（Dummy），保证代码完美编译和直接运行。
//    // 等你对接了真实的 ES 和 重排 API，直接替换下面两个方法的实现即可。
//    // ----------------------------------------------------------------------
//
//    private List<Document> mockKeywordSearch(String query, int size) {
//        // 占位：实际中请调用 Elasticsearch
//        return new ArrayList<>();
//    }
//
//    private List<Document> mockLambdaMartRerank(String query, List<Document> docs) {
//        // 占位：实际中请调用真实的 Rerank API (如 BGE-Reranker)
//        // 目前演示：直接复用向量数据库自带的相似度得分（如果有的话），并注入到 Metadata 中
//        for (Document doc : docs) {
//            // 如果底层向量库没返回距离，这里给个默认及格分 0.85 保证流程跑通
//            double mockScore = doc.getMetadata().containsKey("distance") ?
//                    (1.0 - (Double) doc.getMetadata().get("distance")) : 0.85;
//
//            // 🔥 极其关键：把综合得分写入 Metadata，供 Advisor 拦截使用
//            doc.getMetadata().put("final_rerank_score", mockScore);
//        }
//
//        // 按照得分降序排列
//        docs.sort((d1, d2) -> Double.compare(
//                (Double) d2.getMetadata().getOrDefault("final_rerank_score", 0.0),
//                (Double) d1.getMetadata().getOrDefault("final_rerank_score", 0.0)
//        ));
//
//        return docs;
//    }
//}