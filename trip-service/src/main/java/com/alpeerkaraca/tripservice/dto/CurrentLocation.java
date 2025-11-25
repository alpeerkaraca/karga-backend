package com.alpeerkaraca.tripservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentLocation {
    @NotNull
    @Min(-90)
    @Max(90)
    double latitude;
    @NotNull
    @Min(-180)
    @Max(180)
    double longitude;
}
