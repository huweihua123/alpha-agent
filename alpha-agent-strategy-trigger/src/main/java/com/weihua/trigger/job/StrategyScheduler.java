package com.weihua.trigger.job;

import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;
import com.weihua.strategy.application.service.TradingCycleAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 策略调度器
 * 定期扫描并执行所有运行中的策略
 */
@Component
public class StrategyScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyScheduler.class);
    
    @Resource
    private TradingCycleAppService tradingCycleAppService;
    
    @Resource
    private IStrategyInstanceRepository strategyInstanceRepository;
    
    /**
     * 每60秒执行一次，扫描所有 RUNNING 状态的策略
     * 可通过配置文件调整间隔: strategy.scheduler.fixed-rate
     */
    @Scheduled(fixedRateString = "${strategy.scheduler.fixed-rate:60000}")
    public void executeAllRunningStrategies() {
        logger.info("Strategy scheduler triggered");
        
        try {
            // 查询所有运行中的策略
            List<StrategyInstanceAggregate> runningStrategies = 
                strategyInstanceRepository.findByStatus(StrategyStatus.RUNNING);
            
            if (runningStrategies == null || runningStrategies.isEmpty()) {
                logger.debug("No running strategies found");
                return;
            }
            
            logger.info("Found {} running strategies", runningStrategies.size());
            
            // 逐个执行
            for (StrategyInstanceAggregate strategy : runningStrategies) {
                try {
                    logger.info("Executing strategy: userId={}, strategyId={}, name={}", 
                        strategy.getUserId(),
                        strategy.getStrategyId(), 
                        strategy.getConfig().getStrategyName());
                    
                    tradingCycleAppService.executeCycle(strategy.getStrategyId());
                    
                    logger.info("Strategy executed successfully: userId={}, strategyId={}", 
                        strategy.getUserId(), strategy.getStrategyId());
                    
                } catch (Exception e) {
                    logger.error("Strategy execution failed: userId={}, strategyId={}, error={}", 
                        strategy.getUserId(), strategy.getStrategyId(), e.getMessage(), e);
                    
                    // 可以在这里记录失败次数,超过阈值后自动停止策略
                    // 或者发送告警通知
                }
            }
            
        } catch (Exception e) {
            logger.error("Strategy scheduler execution failed", e);
        }
    }
}
