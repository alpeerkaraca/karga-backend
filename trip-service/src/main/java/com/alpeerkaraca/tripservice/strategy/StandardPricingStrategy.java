package com.alpeerkaraca.tripservice.strategy;

import com.alpeerkaraca.tripservice.model.PricingType;
import com.alpeerkaraca.tripservice.model.Trip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("standardPricingStrategy")
public class StandardPricingStrategy extends BasePricingStrategy {

    private static final double DISTANCE_FEE_PER_KM = 36.30;
    private static final double TIME_FEE_PER_MIN = 7.56;
    private static final double OPENING_FEE = 54.50;
    private static final BigDecimal MINIMUM_FEE = BigDecimal.valueOf(175);

    /**
     * Calculates the total fare based on distance (Haversine formula) and time elapsed.
     * Returns the minimum fee if the calculated fare is lower.
     */
    @Override
    public BigDecimal calculate(Trip trip) {
        return calculateBasePrice(trip, DISTANCE_FEE_PER_KM, TIME_FEE_PER_MIN, OPENING_FEE, MINIMUM_FEE);
    }

    @Override
    public PricingType getType() {
        return PricingType.STANDARD;
    }
}