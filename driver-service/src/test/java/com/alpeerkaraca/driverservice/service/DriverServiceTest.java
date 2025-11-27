package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.model.Driver;
import com.alpeerkaraca.driverservice.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    private Driver testDriver;
    private UUID testDriverId;

    @BeforeEach
    void setUp() {
        testDriverId = UUID.randomUUID();
        testDriver = Driver.builder()
                .driverId(testDriverId)
                .isApproved(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should find driver by ID successfully")
    void findDriverById_ExistingDriver_ReturnsDriver() {
        // Arrange
        when(driverRepository.findDriverByDriverId(testDriverId)).thenReturn(Optional.of(testDriver));

        // Act
        Driver result = driverService.findDriverById(testDriverId);

        assertThat(result.isApproved()).isTrue();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDriverId()).isEqualTo(testDriverId);

        verify(driverRepository).findDriverByDriverId(testDriverId);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when driver not found")
    void findDriverById_NonExistentDriver_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(driverRepository.findDriverByDriverId(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> driverService.findDriverById(nonExistentId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Sürücü bulunamadı");

        verify(driverRepository).findDriverByDriverId(nonExistentId);
    }
}

