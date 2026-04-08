package domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import domain.agent.model.entity.ExecuteCommandEntity;
import domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 执行根节点 (执行链路的物理入口起点)
 * 核心功能：负责在多Agent协作正式开始前，初始化贯穿全链路的动态上下文（DynamicContext）对象。
 */
@Slf4j
@Service("executeRootNode")
public class RootNode extends AbstractExecuteSupport {

    // 注入链路的第一个实质性处理节点（分析节点）
    @Resource
    private Step1AnalyzerNode step1AnalyzerNode;

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("=== 动态多轮执行测试开始 ====");
        log.info("用户输入: {}", requestParameter.getMessage());
        log.info("最大执行步数: {}", requestParameter.getMaxStep());
        log.info("会话ID: {}", requestParameter.getSessionId());

        // 从数据库查出当前智能体(AgentId)下，绑定的所有具体客户端(比如3101分析师、3102执行者)配置信息
        Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap = repository.queryAiAgentClientFlowConfig(requestParameter.getAiAgentId());

        // 2. 初始化核心黑板数据 (将初始状态写入 DynamicContext)
        // 赋予各个节点调用其它角色的配置信息
        dynamicContext.setAiAgentClientFlowConfigVOMap(aiAgentClientFlowConfigVOMap);

        // 极其关键：初始化一条空白的 StringBuilder，用来持续追加记录后续各个 Step 的执行日志和思考过程
        dynamicContext.setExecutionHistory(new StringBuilder());

        // 确定全局总任务目标
        dynamicContext.setCurrentTask(requestParameter.getMessage());

        // 设定系统最大流转层数，防止后续 Step2 和 Step3 陷入无限循环的死锁重试中
        dynamicContext.setMaxStep(requestParameter.getMaxStep());

        // 3. 基础上下文装配完毕，调用父类的 router() 方法触发路由流转，跳转到 get() 方法指定的下一个节点
        return router(requestParameter, dynamicContext);
    }

    /**
     * 路由指针：指定当前节点执行完 doApply 后，数据流向哪里。
     * 因为这是起点节点，所以它的硬编码流向必定是第一个业务处理节点：Step1AnalyzerNode（分析师节点）。
     */
    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return step1AnalyzerNode;
    }
}