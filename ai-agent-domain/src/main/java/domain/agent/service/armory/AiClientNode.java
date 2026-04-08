package domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.model.valobj.AiClientSystemPromptVO;
import domain.agent.model.valobj.AiClientVO;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ai agent 客户端对话对象节点
 */

/**
 * AI Agent 客户端总装节点 (总装车间)
 * 核心职责：将所有零散的配置（提示词、模型、工具、记忆插件）组装成完整的 Spring AI ChatClient 实例，并注册为动态 Bean。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
@Service
public class AiClientNode extends AbstractArmorySupport {

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，正在进行客户端总装: {}", JSON.toJSONString(requestParameter));

        // 从黑板（动态上下文）中获取待装配的客户端列表（比如 3101大脑、3102执行器、3103监督员）
        List<AiClientVO> aiClientList = dynamicContext.getValue(dataName());

        // 如果没有需要装配的客户端，直接跳过，流转到下一节点
        if (null == aiClientList || aiClientList.isEmpty()) {
            return router(requestParameter, dynamicContext);
        }

        // 从黑板获取所有的系统提示词（人设约束）
        Map<String, AiClientSystemPromptVO> systemPromptMap = dynamicContext.getValue(AiAgentEnumVO.AI_CLIENT_SYSTEM_PROMPT.getDataName());

        // 遍历所有需要组装的客户端，挨个锻造
        for (AiClientVO aiClientVO : aiClientList) {

            // ==========================================
            // 1. 注入灵魂 (System Prompt)
            // ==========================================
            StringBuilder defaultSystem = new StringBuilder("Ai 智能体 \r\n");
            List<String> promptIdList = aiClientVO.getPromptIdList();
            for (String promptId : promptIdList) {
                // 根据 ID 找到具体的护栏规则（比如：“你是一个精准执行器”、“绝对不许擅自发帖”）拼接起来
                AiClientSystemPromptVO aiClientSystemPromptVO = systemPromptMap.get(promptId);
                defaultSystem.append(aiClientSystemPromptVO.getPromptContent());
            }

            // ==========================================
            // 2. 装载大脑 (LLM Model)
            // ==========================================
            // 从 Spring 容器中拉取具体的底层大模型（如 GPT-4、DeepSeek 等对应的 Bean）
            OpenAiChatModel chatModel = getBean(aiClientVO.getModelBeanName());

            // ==========================================
            // 3. 挂载双手 (MCP Tools)
            // ==========================================
            List<McpSyncClient> mcpSyncClients = new ArrayList<>();
            List<String> mcpBeanNameList = aiClientVO.getMcpBeanNameList();
            for (String mcpBeanName : mcpBeanNameList) {
                // 将小红书发帖、查询数据库等 MCP 工具对象捞出来
                mcpSyncClients.add(getBean(mcpBeanName));
            }

            // ==========================================
            // 4. 挂载外挂 (Advisor 顾问角色)
            // ==========================================
            List<Advisor> advisors = new ArrayList<>();
            List<String> advisorBeanNameList = aiClientVO.getAdvisorBeanNameList();
            for (String advisorBeanName : advisorBeanNameList) {
                // 🌟 重点！这里捞出来的就是咱们刚才费心费力写好的 RAG 插件 (4002) 和 长期记忆插件 (4003)
                advisors.add(getBean(advisorBeanName));
            }
            Advisor[] advisorArray = advisors.toArray(new Advisor[]{});

            // ==========================================
            // 5. 终极组装 (构建 ChatClient)
            // ==========================================
            // 使用 Spring AI 的建造者模式，把上面的 灵魂、大脑、双手、外挂 全部合体！
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(defaultSystem.toString()) // 设置默认系统提示词
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients.toArray(new McpSyncClient[]{}))) // 绑定工具回调
                    .defaultAdvisors(advisorArray) // 挂载拦截器（记忆/知识库）
                    .build();

            // ==========================================
            // 6. 资产入库 (注册 Bean)
            // ==========================================
            // 把造好的机甲命名（比如 "ai_client_3101"），强行注册到 Spring 的 Bean 容器中。
            // 这样一来，在业务流转（Step1、Step2）时，就能直接通过 getBean("ai_client_3101") 召唤它来干活了！
            registerBean(beanName(aiClientVO.getClientId()), ChatClient.class, chatClient);
        }

        // 装配完毕，进入收尾路由
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    @Override
    protected String beanName(String id) {
        return AiAgentEnumVO.AI_CLIENT.getBeanName(id);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT.getDataName();
    }

}
