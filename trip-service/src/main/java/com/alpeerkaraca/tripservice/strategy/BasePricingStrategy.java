package com.alpeerkaraca.tripservice.strategy;

import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;

import java.math.BigDecimal;
import java.time.Duration;

public abstract class BasePricingStrategy implements PricingStrategy {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the base fare based on distance (Haversine formula) and time elapsed.
     *
     * @param trip       The trip for which to calculate the fare.
     * @param feePerKm   Fee charged per kilometer.
     * @param feePerMin  Fee charged per minute.
     * @param openingFee Initial fee charged at the start of the trip.
     * @param minFee     Minimum fee for the trip.
     * @return Base price or minimum fee if the calculated fare is lower.
     */

    protected BigDecimal calculateBasePrice(Trip trip, double feePerKm, double feePerMin, double openingFee, BigDecimal minFee) {
        if (trip.getTripStatus() != TripStatus.COMPLETED) {
            return BigDecimal.ZERO;
        }

        long elapsedMinutes = Duration.between(trip.getStartedAt(), trip.getEndedAt()).toMinutes();

        double distanceInKm = calculateHaversineDistance(
                trip.getStartLatitude(),
                trip.getStartLongitude(),
                trip.getEndLatitude(),
                trip.getEndLongitude()
        );

        BigDecimal fare = BigDecimal.valueOf(openingFee)
                .add(BigDecimal.valueOf(feePerKm * distanceInKm))
                .add(BigDecimal.valueOf(feePerMin * elapsedMinutes));

        if (fare.compareTo(minFee) < 0) {
            return minFee;
        }
        return fare;
    }

    /**
     * Calculates the great-circle distance between two points on a sphere using the Haversine formula.
     */
    private double calculateHaversineDistance(double startLat, double startLong, double endLat, double endLong) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLon = Math.toRadians(endLong - startLong);

        double startLatitudeRad = Math.toRadians(startLat);
        double endLatitudeRad = Math.toRadians(endLat);

        double squareOfHalfChordLength =
                Math.pow(Math.sin(dLat / 2), 2) +
                        Math.pow(Math.sin(dLon / 2), 2) *
                                Math.cos(startLatitudeRad) * Math.cos(endLatitudeRad);

        double angularDistanceInRadians = 2 * Math.asin(Math.sqrt(squareOfHalfChordLength));

        return EARTH_RADIUS_KM * angularDistanceInRadians;
    }
}