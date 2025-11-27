package com.alpeerkaraca.driverservice.controller;


import com.alpeerkaraca.driverservice.dto.LocationUpdateRequest;
import com.alpeerkaraca.driverservice.service.DriverLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/drivers")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @Validated
    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody LocationUpdateRequest driverUpdateStatus) {
        String userIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID driverId = UUID.fromString(userIdString);
        driverLocationService.publishDriverLocationMessage(
                driverId,
                driverUpdateStatus.getLatitude(),
                driverUpdateStatus.getLongitude()
        );
        return ResponseEntity.accepted().build();
    }
}
