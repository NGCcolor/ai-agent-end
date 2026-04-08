package domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import domain.agent.adapter.repository.IAgentRepository;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * 装配支撑类 (车间基础设施底座)
 * 作用：它继承了小傅哥的扳手路由框架。所有继承这个类的业务节点（比如 RootNode, AiClientApiNode），
 * 都不用再自己去弄数据库连接、线程池、Spring上下文了，直接喊一声“爹”，就能白嫖这里的全部资源！
 */
public abstract class AbstractArmorySupport extends AbstractMultiThreadStrategyRouter<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> {

    private final Logger log = LoggerFactory.getLogger(AbstractArmorySupport.class);

    // 🎒 奶爸的百宝箱 1：Spring 的绝对核心上下文，能拿 Bean、注 Bean，掌控雷电
    @Resource
    protected ApplicationContext applicationContext;

    // 🎒 奶爸的百宝箱 2：高并发多线程池。之前咱们看的那个同时查 6 张表的并发魔法，就是用它跑的
    @Resource
    protected ThreadPoolExecutor threadPoolExecutor;

    // 🎒 奶爸的百宝箱 3：数据库仓储接口。想查什么数据，直接调 repository
    @Resource
    protected IAgentRepository repository;

    /**
     * 缺省的多线程处理方法
     * 意思是：如果某个子节点（车间）在干正事（doApply）之前，不需要并发查数据，那就什么都不干。
     * 如果像 RootNode 那样需要并发查数据，子类直接重写 (Override) 这个方法就行。
     */
    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的
    }
    protected String beanName(String id) {
        return null;
    }

    protected String dataName() {
        return null;
    }

    /**
     * 🔥 宇宙级核心魔法：运行时动态注册 Spring Bean
     * 正常情况下，Bean 都是程序启动时靠 @Service 或 @Bean 写死扫描的。
     * 但这个方法，能在程序运行到一半时，硬生生把一个刚刚 new 出来的对象塞进 Spring 容器里！
     *
     * @param beanName  你要给这个 Bean 起的名字（比如 "ai_client_api_1001"）
     * @param beanClass 这个 Bean 的类型（比如 OpenAiApi.class）
     * @param beanInstance 你刚才 new 出来的真实对象
     */
    protected synchronized <T> void registerBean(String beanName, Class<T> beanClass, T beanInstance) {
        // 1. 获取 Spring 底层的 Bean 工厂（也就是真正管理 Bean 的仓库管理员）
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        // 2. 填写“入库申请单”（BeanDefinition）
        // 告诉 Spring：我要入库一个类型为 beanClass 的东西，你如果要造它，就直接用我传进来的 beanInstance
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass, () -> beanInstance);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();

        // 3. 设定为“单例模式”（SCOPE_SINGLETON）
        // 保证这个对象在整个 Spring 容器里只有一份，大家谁要用都来拿这一个，不浪费内存
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);

        // 4. 🔥 极其细节的防御机制：热更新！
        // 假设客户去数据库把自己的 API Key 改了，系统重新跑流水线。
        // 这时 Spring 容器里可能还存着旧的 Bean。这里先检查：如果有了，就无情删掉（卸磨杀驴）！
        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.removeBeanDefinition(beanName);
        }

        // 5. 正式把新的对象以 Bean 的身份注册进 Spring 容器！上户口成功！
        beanFactory.registerBeanDefinition(beanName, beanDefinition);

        log.info("成功注册Bean: {}", beanName);
    }

    /**
     * 便捷方法：既然能动态注册 Bean，当然也要能随时随地从 Spring 容器里拿 Bean
     */
    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

}