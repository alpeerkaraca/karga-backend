package com.alpeerkaraca.tripservice.strategy;

import com.alpeerkaraca.tripservice.model.PricingType;
import com.alpeerkaraca.tripservice.model.Trip;

import java.math.BigDecimal;

public interface PricingStrategy {
    /**
     * Calculates the fare for a given trip.
     *
     * @param trip Completed trip details.
     * @return Calculated fare as BigDecimal.
     */
    BigDecimal calculate(Trip trip);

    PricingType getType();
}
