//package domain.agent.service.armory.factory.element.advisorTool;
//
//import org.springframework.ai.document.Document;
//import org.springframework.stereotype.Component;
//import java.util.List;
//
///**
// * 文档重排实现类 (对接 LambdaMART / BGE-Reranker)
// */
//@Component
//public class DocumentRerankerImpl implements IAdvisor.IDocumentReranker {
//
//    private final String baseUrl = "http://192.168.1.108:8103"; // 你的重排模型地址
//    private final String model = "bge-reranker-v2-m3";
//
//    @Override
//    public List<Document> rerank(String query, List<Document> documents) {
//        // 1. 如果文档太少，不需要重排，直接返回
//        if (documents == null || documents.size() <= 1) return documents;
//
//        // 2. TODO: 使用 RestTemplate 或 WebClient 发送 POST 请求给模型服务器
//        // 请求体通常包含：{ "query": query, "documents": [doc1.text, doc2.text...] }
//
//        // 3. 拿到模型返回的分数后，更新 metadata
//        for (Document doc : documents) {
//            // 这里目前还是模拟逻辑：
//            double score = calculateMockScore(query, doc.getText());
//            doc.getMetadata().put("final_rerank_score", score);
//        }
//
//        // 4. 根据新分数排序
//        documents.sort((d1, d2) -> Double.compare(
//                (Double) d2.getMetadata().getOrDefault("final_rerank_score", 0.0),
//                (Double) d1.getMetadata().getOrDefault("final_rerank_score", 0.0)
//        ));
//
//        return documents;
//    }
//
//    private double calculateMockScore(String query, String text) {
//        // 演示逻辑：暂时给个 0.85 的及格分
//        return 0.85;
//    }
//}