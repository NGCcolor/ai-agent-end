package domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import domain.agent.model.entity.ExecuteCommandEntity;
import domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import domain.agent.model.valobj.Enums.AiClientTypeEnumVO;
import domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 任务分析节点
 */
@Slf4j
@Service
public class Step1AnalyzerNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("\n🎯 === 执行第 {} 步 ===", dynamicContext.getStep());

        // ==========================================
        // 1. 组装“会议简报”（给大模型看的 Prompt）
        // ==========================================
        log.info("\n📊 阶段1: 任务状态分析");
        // 这里把 DynamicContext（黑板）上的信息汇总，告诉分析师现在的战况
        String analysisPrompt = String.format("""
                        **原始用户需求:** %s  // 老板最初布置的任务
                        
                        **当前执行步骤:** 第 %d 步 (最大 %d 步) // 提醒AI别无限循环
                        
                        **历史执行记录:**
                        %s  // 之前大家都干了啥？如果是第一轮，这里就是 [首次执行]
                        
                        **当前任务:** %s // 这一轮具体要干啥？
                        
                        请分析当前任务状态，评估执行进度，并制定下一步策略。
                        """,
                requestParameter.getMessage(),//最终任务不可篡改
                dynamicContext.getStep(),//确定现在是多少步
                dynamicContext.getMaxStep(),//最多多少步
                !dynamicContext.getExecutionHistory().isEmpty() ? dynamicContext.getExecutionHistory().toString() : "[首次执行]",//记录每一步发生了什么
                dynamicContext.getCurrentTask()//目前任务
        );

        // ==========================================
        // 2. 召唤“分析师”本体
        // ==========================================
        // 从流转名册里，精准找到代表“任务分析师(TASK_ANALYZER_CLIENT)”的配置
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.TASK_ANALYZER_CLIENT.getCode());

        //调用大模型
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        // ==========================================
        // 3. 灵魂对话与记忆唤醒
        // ==========================================
        String analysisResult = chatClient
                .prompt(analysisPrompt) // 把会议简报拍在它桌上
                .advisors(a -> a        // 唤醒它的官方底层记忆芯片（ChatMemory）
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();      // 听取它的分析报告！

        assert analysisResult != null;

        // （辅助方法）纯粹是为了把大模型返回的文本，在控制台打印得好看一点，方便你调试查Bug
        parseAnalysisResult(dynamicContext.getStep(), analysisResult);

        // ==========================================
        // 4. 将分析结果写在“黑板”上（交棒准备）
        // ==========================================
        // 极其关键！分析师把自己的计划存进 DynamicContext，
        // 这样等会儿 Step2(执行者) 登场时，就能直接从黑板上看到分析师的指示！
        dynamicContext.setValue("analysisResult", analysisResult);

        // ==========================================
        // 5. 判断任务是否通关？
        // ==========================================
        // 看看大模型的回答里有没有包含结束的暗号
        if (analysisResult.contains("任务状态: COMPLETED") ||
                analysisResult.contains("完成度评估: 100%")) {
            // 如果大模型认为任务搞定了，直接修改黑板上的状态为“已完成”
            dynamicContext.setCompleted(true);
            log.info("✅ 任务分析显示已完成！");

            // 直接触发路由，准备走向下个节点（因为 completed = true，会走向 Step4 总结）
            return router(requestParameter, dynamicContext);
        }

        // 如果没干完，触发路由，准备走向下个节点（Step2 执行者）
        return router(requestParameter, dynamicContext);
    }

    /**
     * 路由分发器：决定下一步去哪个节点？
     * （被 doApply 最后的 router() 触发）
     */
    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {

        // 护栏机制 1：如果黑板上写着已完成（分析师说干完了），或者触发了最大防爆步数
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            // 下班！直接走向 Step4（总结与汇报节点）
            return getBean("step4LogExecutionSummaryNode");
        }

        // 护栏机制 2：如果没干完
        // 把带着最新计划书的黑板，交给 Step 2（精准执行节点，即打工人 3102）去干活！
        return getBean("step2PrecisionExecutorNode");
    }

    // （这里省略 parseAnalysisResult 方法的注解，因为它全是 String.split 和 log.info 打印日志的代码，不涉及核心架构逻辑）
    // 它的作用就是把大模型输出的Markdown文本，拆分成好看的带 Emoji 的控制台日志。
    private void parseAnalysisResult(int step, String analysisResult) {
        log.info("\n📊 === 第 {} 步分析结果 ===", step);

        String[] lines = analysisResult.split("\n");
        String currentSection = "";

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("任务状态分析:")) {
                currentSection = "status";
                log.info("\n🎯 任务状态分析:");
                continue;
            } else if (line.contains("执行历史评估:")) {
                currentSection = "history";
                log.info("\n📈 执行历史评估:");
                continue;
            } else if (line.contains("下一步策略:")) {
                currentSection = "strategy";
                log.info("\n🚀 下一步策略:");
                continue;
            } else if (line.contains("完成度评估:")) {
                currentSection = "progress";
                String progress = line.substring(line.indexOf(":") + 1).trim();
                log.info("\n📊 完成度评估: {}", progress);
                continue;
            } else if (line.contains("任务状态:")) {
                currentSection = "task_status";
                String status = line.substring(line.indexOf(":") + 1).trim();
                if (status.equals("COMPLETED")) {
                    log.info("\n✅ 任务状态: 已完成");
                } else {
                    log.info("\n🔄 任务状态: 继续执行");
                }
                continue;
            }

            switch (currentSection) {
                case "status":
                    log.info("   📋 {}", line);
                    break;
                case "history":
                    log.info("   📊 {}", line);
                    break;
                case "strategy":
                    log.info("   🎯 {}", line);
                    break;
                default:
                    log.info("   📝 {}", line);
                    break;
            }
        }
    }

}
