package com.alpeerkaraca.tripservice.controller;


import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.tripservice.dto.NearbyDriversResponse;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.service.TripManagementService;
import com.alpeerkaraca.tripservice.service.TripRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips")
public class TripsController {

    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final TripRequestService tripRequestService;
    private final TripManagementService tripManagementService;

    @GetMapping("/nearby-drivers")
    public ResponseEntity<ApiResponse<List<NearbyDriversResponse>>> getNearbyDrivers(
            @NotNull @Valid @RequestParam("latitude") Double latitude,
            @NotNull @Valid @RequestParam("longitude") Double longitude
    ) {
        List<NearbyDriversResponse> nearbyDrivers = tripRequestService.findNearbyDrivers(
                latitude,
                longitude,
                DEFAULT_RADIUS_KM
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        nearbyDrivers,
                        "Yakındaki sürücüler listelendi."
                ));

    }

    @PostMapping("/request")
    public ApiResponse<Trip> requestTrip(@Valid @RequestBody TripRequest request) {
        String passengerIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID userId = UUID.fromString(passengerIdString);
        Trip trip = tripRequestService.requestTrip(request, userId);

        return ApiResponse.success(trip, "Yolculuk talebiniz alındı.");
    }

    @GetMapping("/available")
    public ApiResponse<List<Trip>> getAvailableTrips() {
        List<Trip> trips = tripManagementService.getAvailableTrips();
        return ApiResponse.success(trips, "Mevcut yolculuklar listelendi.");
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{tripId}/accept")
    public ResponseEntity<ApiResponse<Trip>> acceptTrip(@PathVariable("tripId") UUID tripId) {
        String driverIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID driverID = UUID.fromString(driverIdStr);
        Trip trip = tripManagementService.acceptTrip(tripId, driverID);
        return ResponseEntity.ok(ApiResponse.success(trip, "Trip accepted."));
    }

    @PostMapping("/{tripId}/start")
    public ApiResponse<Void> startTrip(@PathVariable("tripId") UUID tripId) {
        tripManagementService.startTrip(tripId);
        return ApiResponse.success(null, "Yolculuk başlatıldı.");
    }

    @PostMapping("/{tripId}/complete")
    public ApiResponse<Void> completeTrip(@PathVariable("tripId") UUID tripId) {
        tripManagementService.completeTrip(tripId);
        return ApiResponse.success(null, "Yolculuk tamamlandı.");
    }

    @PostMapping("/{tripId}/cancel")
    public ApiResponse<Void> cancelTrip(@PathVariable("tripId") UUID tripId) {
        tripManagementService.cancelTrip(tripId);
        return ApiResponse.success(null, "Yolculuk iptal edildi.");
    }
}
