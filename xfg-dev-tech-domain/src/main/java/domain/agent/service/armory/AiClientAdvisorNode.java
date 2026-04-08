package domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.model.valobj.Enums.AiClientAdvisorTypeEnumVO;
import domain.agent.model.valobj.AiClientAdvisorVO;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
// 🚀 核心引入：引入你新写的高级检索器
//import domain.agent.service.armory.factory.element.AdvancedKnowledgeRetriever;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 顾问角色节点 (Advisor 装配车间)
 * 核心功能：负责将数据库里配置的记忆、RAG 等增强能力，实例化为 Spring AI 的 Advisor 对象，并注册到 Spring 容器中。
 */
@Slf4j
@Service
public class  AiClientAdvisorNode extends AbstractArmorySupport {

    // 🔥 核心改动 1：把原来的 VectorStore 删掉，换成咱们自己的超级检索器！
    // 此时 Spring Boot 会自动把 AdvancedKnowledgeRetriever 单例注入进来
//    @Resource
//    private AdvancedKnowledgeRetriever advancedKnowledgeRetriever;
    @Resource
    private VectorStore vectorStore;

    // 链路的下一个节点：去给大模型本体装配这些东西
    @Resource
    private AiClientNode aiClientNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，Advisor 顾问角色{}", JSON.toJSONString(requestParameter));

        // 1. 从“万能托盘”里拿出当前需要装配的外挂配置清单（由第一步查数据库得来）
        List<AiClientAdvisorVO> aiClientAdvisorList = dynamicContext.getValue(dataName());

        // 防御性编程：如果没有给这个 Agent 配置记忆或知识库，直接放行去下一关
        if (aiClientAdvisorList == null || aiClientAdvisorList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client advisor");
            return router(requestParameter, dynamicContext);
        }

        // 2. 挨个锻造外挂装备
        for (AiClientAdvisorVO aiClientAdvisorVO : aiClientAdvisorList) {
            // 🔥 核心动作 A：根据配置的类型（记忆还是RAG），通过工厂方法创建出真实的 Advisor 实例
            Advisor advisor = createAdvisor(aiClientAdvisorVO);

            // 🔥 核心动作 B：把造好的 Advisor 挂载到 Spring 容器里（变成类似 ai_client_advisor_4001 的 Bean）
            registerBean(beanName(aiClientAdvisorVO.getAdvisorId()), Advisor.class, advisor);
        }

        // 本工序完成，去下一关
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientNode;
    }

    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_ADVISOR.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_ADVISOR.getDataName();
    }

    /**
     * 【精妙的委派设计模式】
     * 这里并没有写一堆 if-else 来判断是造记忆还是造 RAG，
     * 而是直接获取到枚举类 (AiClientAdvisorTypeEnumVO)，让枚举类自己去实例化！
     */
    private Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO) {
        String advisorType = aiClientAdvisorVO.getAdvisorType();
        AiClientAdvisorTypeEnumVO advisorTypeEnum = AiClientAdvisorTypeEnumVO.getByCode(advisorType);

        // 🔥 核心改动 2：注意看这里！把大管家手里的新武器（超级检索器），原封不动地传给了枚举的创建方法！
//        return advisorTypeEnum.createAdvisor(aiClientAdvisorVO, advancedKnowledgeRetriever);
        return advisorTypeEnum.createAdvisor(aiClientAdvisorVO, vectorStore);
    }

}