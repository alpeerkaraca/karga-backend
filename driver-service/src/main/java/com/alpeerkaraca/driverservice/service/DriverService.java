package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.model.Driver;
import com.alpeerkaraca.driverservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;

    public Driver findDriverById(UUID driverId) {
        return driverRepository.findDriverByDriverId(driverId).orElseThrow(() -> new UsernameNotFoundException("Sürücü bulunamadı: " + driverId));
    }
}
