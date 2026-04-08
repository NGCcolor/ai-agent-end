package domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.service.armory.RootNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认的“军械库（Armory）”策略工厂类
 * 作用：在 AI Agent 的设计里，Armory 通常指代“装备/工具/技能”。
 * 这个工厂未来会用来根据不同的条件，组装和分发不同的 AI 技能策略。
 *
 *
 * 说白了就是定义了一堆东西，不管哪个客户端都要填写这些，相当于一个用来装填参数的框架
 *
 *
 */
@Service // 告诉 Spring：这是一个服务类，请帮我把它放进 Spring 容器里管理（单例）
public class DefaultArmoryStrategyFactory {
//    这是干嘛的：这个方法就像是工厂外墙上安装的一个**“红色启动大按钮”**。外部系统只要调这个方法，工厂就把 RootNode 交出去。

    private final RootNode rootNode;

    public DefaultArmoryStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, String> armoryStrategyHandler(){
        return rootNode;
    }

    /**
     * 动态上下文环境类 (极其精妙的设计！)
     * 在复杂的策略模式中，不同策略需要的参数千奇百怪（有的需要 userId，有的需要 prompt，有的需要文件流）。
     * 如果写死固定属性的实体类，改起来会非常痛苦。用这个动态上下文，想塞什么塞什么！
     */
    @Data                 // Lombok：自动生成 getter、setter、toString、equals 等方法
    @Builder              // Lombok：自动生成建造者模式 (可以使用 DynamicContext.builder().build() 来优雅地创建对象)
    @AllArgsConstructor   // Lombok：自动生成包含所有参数的全参构造函数
    @NoArgsConstructor    // Lombok：自动生成无参构造函数（配合框架反射必须要用到）
    public static class DynamicContext {

        // 核心底座：用一个 HashMap 来装载所有的动态参数，Key 是参数名，Value 是具体的参数对象
        private Map<String, Object> dataObjects = new HashMap<>();

        /**
         * 万能存储方法
         * 泛型 <T> 让你存参数时随心所欲，不管存 String、Integer 还是自定义的对象都可以
         *
         * @param key   参数名 (比如: "userId")
         * @param value 参数值 (比如: 10086)
         */
        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        /**
         * 万能提取方法
         * 这里的泛型 <T> 是魔法：它会自动根据你接收参数的类型进行强转！
         * 你拿数据的时候再也不用写难看的强制类型转换了，比如： String name = context.getValue("name");
         *
         * @param key 参数名
         * @return 自动强转后的参数值
         */
        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }

}