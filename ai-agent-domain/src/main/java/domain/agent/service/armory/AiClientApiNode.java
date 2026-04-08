package domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.model.valobj.AiClientApiVO;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * OpenAI API配置节点
 * 角色：流水线上的“武器装配与登记员”
 */
@Slf4j
@Service // 自己本身也是个 Spring Bean，随时等候流水线调用
public class AiClientApiNode extends AbstractArmorySupport {

    @Resource
    private AiClientToolMcpNode aiClientToolMcpNode;

    /**
     * 核心加工逻辑：拿到盘子（Context），取料、组装、登记、放行
     */
    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建，API 构建节点 {}", JSON.toJSONString(requestParameter));

        // 步骤 1：从“万能托盘（DynamicContext）”里拿原材料
        // 前面的 RootNode 和 DataLoadStrategy 已经去数据库里查好了配置，放在了盘子里。
        // 这里直接根据 Key（AI_CLIENT_API.getDataName()）把底层的 API 配置列表抓出来。
        List<AiClientApiVO> aiClientApiList = dynamicContext.getValue(AiAgentEnumVO.AI_CLIENT_API.getDataName());

        // 防御性编程：如果盘子里没东西，说明这个客户端压根没配大模型，直接下班。
        if (aiClientApiList == null || aiClientApiList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client api");
            return null;
        }

        // 步骤 2：遍历组装（因为可能同时需要装配多个大模型 API）
        for (AiClientApiVO aiClientApiVO : aiClientApiList) {

            // 🔥 核心动作 A：现场拼装武器（OpenAiApi）
            // 拿着查出来的 baseUrl 和 apiKey，利用 Spring AI 提供的建造者模式，
            // 真正 new 出一个负责 HTTP 通讯的底座对象。
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(aiClientApiVO.getBaseUrl())
                    .apiKey(aiClientApiVO.getApiKey())
                    .completionsPath(aiClientApiVO.getCompletionsPath())
                    .embeddingsPath(aiClientApiVO.getEmbeddingsPath())
                    .build();

            // 🔥 核心动作 B：武器上户口（动态注册 Spring Bean）
            // 调用父类 AbstractArmorySupport 提供的强行注册方法，把刚才拼好的 openAiApi 塞进 Spring 容器！
            // Bean 的名字是动态生成的，比如： ai_client_api_1001 (前缀 + ApiId)
            // 这样下一个节点（造 ChatModel 的车间）就能直接根据这个名字把它提走用了。
            registerBean(AiAgentEnumVO.AI_CLIENT_API.getBeanName(aiClientApiVO.getApiId()), OpenAiApi.class, openAiApi);
        }

        // 步骤 3：本工段任务完成，放行！
        // 调用 router 将请求和装着数据的托盘，传给下一个车间节点（比如去组装 Model）
        return router(requestParameter, dynamicContext);
    }

    /**
     * 扳手框架（Wrench）的路由规则获取方法
     * 意思是：我干完活之后，接下来的流程交给默认的策略处理器去决定往哪走。
     */
    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientToolMcpNode;
    }
    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_API.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_API.getDataName();
    }

}