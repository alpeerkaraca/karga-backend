package com.alpeerkaraca.tripservice.factory;

import com.alpeerkaraca.tripservice.model.PricingType;
import com.alpeerkaraca.tripservice.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PricingStrategyFactory {
    private final Map<PricingType, PricingStrategy> strategies;

    public PricingStrategyFactory(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PricingStrategy::getType, Function.identity()));
    }

    public PricingStrategy getStrategy(PricingType type) {
        return strategies.getOrDefault(type, strategies.get(PricingType.STANDARD));
    }
}
