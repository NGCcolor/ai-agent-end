package domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import domain.agent.adapter.repository.IAgentRepository;
import domain.agent.model.entity.ExecuteCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 执行链路节点的抽象基类
 * 作用：为所有具体的执行步骤节点（Step1, Step2, Step3等）提供基础属性注入、通用常量和底层公共方法。
 */
public abstract class AbstractExecuteSupport extends AbstractMultiThreadStrategyRouter<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> {

    private final Logger log = LoggerFactory.getLogger(AbstractExecuteSupport.class);

    // 【核心依赖 1】Spring 容器上下文。用于在运行时动态获取 Bean 实例。
    @Resource
    protected ApplicationContext applicationContext;

    // 【核心依赖 2】仓储层接口。提供给所有子类统一的数据库访问能力。
    @Resource
    protected IAgentRepository repository;

    // 【全局常量】定义上下文中用于传递“聊天记忆参数”的标准化 Key。
    // 保证各个独立节点在读写 DynamicContext 或缓存时，使用的 Key 名称绝对一致，避免硬编码引发的拼写错误。
    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_response_size";

    /**
     * 实现自 AbstractMultiThreadStrategyRouter 的多线程处理钩子方法。
     * 目前为空实现，表示当前业务流程暂不启用多线程并发路由策略，但保留了框架扩展点。
     */
    @Override
    protected void multiThread(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

    /**
     * 【核心功能方法】根据客户端 ID 获取对应的 ChatClient 实例。
     * 逻辑：通过传入的 clientId，结合枚举类定义的命名规则拼接出 Bean 的完整名称（如 "ai_client_3101"），
     * 然后从 Spring 容器中提取出装配阶段（Armory）动态注册好的 ChatClient 对象。
     */
    protected ChatClient getChatClientByClientId(String clientId) {
        return getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }

    /**
     * 泛型工具方法：封装原生的 applicationContext.getBean()，
     * 消除子类调用时频繁的显式类型转换 (Type Casting) 代码。
     */
    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

}