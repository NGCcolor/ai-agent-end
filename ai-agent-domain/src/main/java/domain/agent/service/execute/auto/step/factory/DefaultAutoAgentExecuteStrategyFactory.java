package domain.agent.service.execute.auto.step.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import domain.agent.model.entity.ExecuteCommandEntity;
import domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import domain.agent.service.execute.auto.step.RootNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动执行策略工厂类
 * 角色：司令部的“前台总机”
 */
@Service
public class DefaultAutoAgentExecuteStrategyFactory {

    // 整个执行流水线的入口节点（发令枪）
    private final RootNode executeRootNode;

    public DefaultAutoAgentExecuteStrategyFactory(RootNode executeRootNode) {
        this.executeRootNode = executeRootNode;
    }

    // 外部系统（比如 Controller）调用这个方法，就能拿到整个执行责任链的入口
    // （注：方法名叫 armoryStrategyHandler 可能是小傅哥复制之前的代码没改名，但逻辑是返回 executeRootNode）
    public StrategyHandler<ExecuteCommandEntity, DynamicContext, String> armoryStrategyHandler(){
        return executeRootNode;
    }

    /**
     * 🌟 核心灵魂所在：动态上下文 (DynamicContext)
     * 角色：多个 Agent 之间共享的“会议纪要”和“万能托盘”
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        // 当前走到第几步了？（防止流程乱套）
        private int step = 1;

        // 🔥 防爆保险丝：最大允许循环几次？
        // 多 Agent 互相反思很容易陷入死循环（比如质检员一直不满意，执行者一直重做），
        // 加上这个最大步数，到了次数强行终止，防止大模型把你 API 余额刷破产！
        private int maxStep = 1;

        // 🔥 极其重要：执行历史记录（会议纪要）
        // 因为大模型没有记忆，从节点1走到节点3时，必须把之前的思考过程记录在这里，
        // 一并塞给下一个 Agent，下一个 Agent 才知道前面发生了什么。
        private StringBuilder executionHistory;

        // 当前正在执行的任务描述（比如：“查北京天气”、“写一篇关于天气的文章”）
        private String currentTask;

        // 最终任务是否完美通关的标志？（如果是 true，流程就结束跳出了）
        boolean isCompleted = false;

        // 存放那张数据库流转表（ai_agent_flow_config）里的配置。
        // 比如：1号是分析师，2号是执行者，3号是质检员。这本名册存在这里供大家查阅。
        private Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap;

        // 📦 万能口袋（白板空间）
        // 有些中间数据（比如刚才查到的温度 25℃）不知道存哪，就以 Key-Value 的形式扔在这个 Map 里。
        private Map<String, Object> dataObjects = new HashMap<>();

        // 往万能口袋里存东西
        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        // 从万能口袋里拿东西
        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }
}