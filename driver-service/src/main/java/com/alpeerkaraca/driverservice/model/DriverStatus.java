package com.alpeerkaraca.driverservice.model;

import com.alpeerkaraca.common.exception.InvalidEnumException;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum DriverStatus {
    ONLINE,
    OFFLINE,
    BUSY;

    @JsonCreator
    public static DriverStatus fromString(String value) {
        try {
            return DriverStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidEnumException("Invalid status: " + value + ". Accepted Values: ONLINE, OFFLINE, BUSY");
        }
    }
}