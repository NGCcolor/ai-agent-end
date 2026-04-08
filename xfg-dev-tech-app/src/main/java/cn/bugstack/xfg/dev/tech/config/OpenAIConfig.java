//package cn.bugstack.xfg.dev.tech.config;
//
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//// 👇 记忆功能需要的新依赖包 👇
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.InMemoryChatMemory;
//import org.springframework.ai.tool.ToolCallbackProvider;
//import org.springframework.ai.document.MetadataMode;
//import org.springframework.ai.openai.OpenAiEmbeddingModel;
//import org.springframework.ai.openai.OpenAiEmbeddingOptions;
//import org.springframework.ai.openai.api.OpenAiApi;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.SimpleVectorStore;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.util.List;
//
//@Configuration
//public class OpenAIConfig {
//
//    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}")
//    private String embeddingModelName;
//
//    @Bean
//    public TokenTextSplitter tokenTextSplitter() {
//        return new TokenTextSplitter();
//    }
//
//    @Bean
//    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl, @Value("${spring.ai.openai.api-key}") String apikey) {
//        return OpenAiApi.builder()
//                .baseUrl(baseUrl)
//                .apiKey(apikey)
//                .build();
//    }
//
//    @Bean
//    public Jackson2ObjectMapperBuilderCustomizer customizer() {
//        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//    }
//
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        return objectMapper;
//    }
//
//    @Bean("openAiSimpleVectorStore")
//    public SimpleVectorStore vectorStore(OpenAiEmbeddingModel openAiEmbeddingModel) {
//        return SimpleVectorStore.builder(openAiEmbeddingModel).build();
//    }
//
//    @Bean("openAiPgVectorStore")
//    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, OpenAiEmbeddingModel openAiEmbeddingModel) {
//        return PgVectorStore.builder(jdbcTemplate, openAiEmbeddingModel)
//                .vectorTableName("vector_store_openai")
//                .dimensions(1536)
//                .build();
//    }
//
//    // =========================================================
//    // 👇👇👇 下面是为你新增的记忆功能与工具挂载配置 👇👇👇
//    // =========================================================
//
//    /**
//     * 1. 记忆芯片：使用内存级别的聊天记忆
//     * 如果重启程序，记忆会清空。如果想永久保存，以后可以换成 Redis 等。
//     */
//    @Bean
//    public ChatMemory chatMemory() {
//        return new InMemoryChatMemory();
//    }
//
//    /**
//     * 2. 终极对话客户端（老大哥的本体）
//     * 注入 ChatClient.Builder (Spring Boot 自动提供)
//     * 注入 chatMemory (上面的记忆芯片)
//     * 注入 List<ToolCallbackProvider> (注意：因为你现在有 CSDN 和微信两个小弟，用 List 可以把它们全部一把抓过来！)
//     */
//    @Bean
//    public ChatClient chatClient(ChatClient.Builder builder,
//                                 ChatMemory chatMemory,
//                                 List<ToolCallbackProvider> toolProviders) {
//
//        return builder
//                // 把所有小弟（CSDN、微信等）的工具全部装备上
//                .defaultTools(toolProviders.toArray(new ToolCallbackProvider[0]))
//                // 装上记忆顾问，让模型能记住上下文，实现多轮对话
//                .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
//                .build();
//    }
//}