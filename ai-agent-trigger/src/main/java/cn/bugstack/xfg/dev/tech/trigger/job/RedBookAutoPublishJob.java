package cn.bugstack.xfg.dev.tech.trigger.job;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.model.entity.ExecuteCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 小红书自动化运营定时任务
 * 终极隔离版：将 User Prompt (业务需求) 与 System Prompt (系统护栏) 物理隔离
 */
@Slf4j
@Service
public class RedBookAutoPublishJob {

    @Resource
    private DefaultArmoryStrategyFactory defaultArmoryStrategyFactory;

    @Resource
    private DefaultAutoAgentExecuteStrategyFactory defaultAutoAgentExecuteStrategyFactory;

    /**
     * 每天早上 9:30 执行
     */
//    @Scheduled(cron = "0 30 9 * * ?")//每天九点半
    @Scheduled(cron = "0 * * * * ?")
    public void execute() {
        log.info("🚀 启动小红书多 Agent 协同发帖任务 (动态挂载隔离版)...");

        try {
            // ==========================================
            // 1. 【初始化军械库】挂载 3101、3102、3103 实例
            // ==========================================
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                    defaultArmoryStrategyFactory.armoryStrategyHandler();

            armoryStrategyHandler.apply(
                    ArmoryCommandEntity.builder()
                            .commandType(AiAgentEnumVO.AI_CLIENT.getCode())
                            .commandIdList(Arrays.asList("3101", "3102", "3103"))
                            .build(),
                    new DefaultArmoryStrategyFactory.DynamicContext());

            // ==========================================
            // 2. 【构建业务指令】(纯净的 User Message)
            // ==========================================
            ExecuteCommandEntity executeCommandEntity = new ExecuteCommandEntity();
            executeCommandEntity.setAiAgentId("3");
            executeCommandEntity.setSessionId("XHS-JOB-" + System.currentTimeMillis());
            executeCommandEntity.setMaxStep(5); // 允许的最大内卷轮数

            // 🌟 获取今天的真实日期和星期（用中文，大模型理解得更好）
            String today = java.time.LocalDate.now().toString();
            String dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.CHINA);

            // 🌟 使用 String.format 动态注入日期
            // 🌟 使用 String.format 动态注入日期
            String dynamicMessage = String.format("""
                    【业务需求 - 赛博玄学运势生成】
                     任务目标：生成一篇小红书爆款“今日综合运势”笔记。
                     当前日期：%s (%s)
                     
                    内容生成指南（请利用你的大模型随机性，每次挑选不同的组合，确保每天绝对不重样）：
                      1. 【今日主角】：请随机挑选 2-3 个“星座”（如天蝎、金牛等），并可以随机搭配“MBTI人格”（如 INFP、ESTJ），作为今天的重点点名对象。
                      2. 【运势基调】：请随机“抽取”一张塔罗牌（如愚者、命运之轮），或者结合今日星象（如水星逆行、满月），作为整篇运势的神秘学背景。
                      3. 【运势解析】：给出这几个主角在“搞钱（财运）”、“搬砖（事业）”、“桃花（感情）”上的神准预测。语气要懂年轻人的痛点（比如：拒绝画大饼、今天适合摸鱼、下班赶紧跑）。
                      4. 【开运玄学】：必须给出非常具体的“今日幸运色”、“今日幸运数字”以及一个搞笑的“开运小动作”（例如：喝一杯冰美式、摸一下老板的绿植、穿一双红袜子）。
                      5. 【爆款排版】：标题必须极具吸引力，甚至带点玄学警告（例：“⚠️刷到这条请留步，X月X日这三大星座马上要转运！”）。正文 Emoji 必须拉满。
                      6. 【互动钩子】：结尾必须引导用户互动，例如：“评论区留下【接好运/接暴富】，一秒破除水逆！”
                      7. 附带标签：#每日运势 #星座 #接好运 #塔罗占卜 #MBTI
                      
                要求：
                1. 标题必须具有吸引力，控制在 20 个字以内。
                2. 正文必须包含丰富的 Emoji，且总字数严格控制在 800字以内。
                3. 最后附带 3-5 个垂直话题标签。
                
                【全局执行护栏 - 极其重要】
                无论你们团队如何分工，作为大脑，你必须遵守以下铁律：
                在收到【监督员】明确的“是否通过: PASS” 历史记录前，严禁在你的下一步策略中授权【执行器】去调用 `xiaohongshu-server` 工具！
                在此之前，你的唯一任务是分配文案打磨计划给执行器！
                当且仅当监督员 PASS 后，你必须明确下达终极指令：“授权执行器调用 xiaohongshu-server 工具进行发帖”。
                """, today, dayOfWeek); // <-- 这里把时间传进去

            executeCommandEntity.setMessage(dynamicMessage);

            // ==========================================
            // 3. 【初始化黑板并注入系统护栏】(System Prompt)
            // ==========================================
            DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext =
                    new DefaultAutoAgentExecuteStrategyFactory.DynamicContext();



            // ==========================================
            // 4. 【触发多 Agent 协同流转】
            // ==========================================
            StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> executeHandler
                    = defaultAutoAgentExecuteStrategyFactory.armoryStrategyHandler();

            // 使用带有 SYSTEM_RULE_3101 的 dynamicContext 启动整个架构
            String result = executeHandler.apply(executeCommandEntity, dynamicContext);

            log.info("✅ 任务执行结果: {}", result);

        } catch (Exception e) {
            log.error("❌ 小红书定时任务执行异常", e);
        }
    }
}