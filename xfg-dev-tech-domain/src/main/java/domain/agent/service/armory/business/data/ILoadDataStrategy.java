package domain.agent.service.armory.business.data;


import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;

/**
 * 数据加载策略======谁数据加载就重写这个
 */
public interface ILoadDataStrategy {

    void loadData(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext);

}
