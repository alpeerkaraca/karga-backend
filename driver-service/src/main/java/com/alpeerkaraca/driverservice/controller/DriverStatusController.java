package com.alpeerkaraca.driverservice.controller;

import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.driverservice.dto.DriverUpdateStatus;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Slf4j
@PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
public class DriverStatusController {
    private final DriverStatusService driverStatusService;

    @PostMapping("/status")
    public ApiResponse<Void> updateDriverStatus(@Validated @RequestBody DriverUpdateStatus request) {
        String userIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID driverId = UUID.fromString(userIdString);

        driverStatusService.updateDriverStatus(
                driverId,
                request.status(),
                request.longitude(),
                request.latitude()
        );
        return ApiResponse.success(null, "Driver status updated successfully.");
    }
}
