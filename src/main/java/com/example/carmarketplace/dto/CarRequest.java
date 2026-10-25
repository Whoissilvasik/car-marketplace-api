package com.example.carmarketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Car data for creating or updating a listing")
public record CarRequest(

        @Schema(example = "Toyota")
        @NotBlank(message = "Brand must not be blank")
        @Size(max = 100, message = "Brand must not be longer than 100 characters")
        String brand,

        @Schema(example = "Camry")
        @NotBlank(message = "Model must not be blank")
        @Size(max = 100, message = "Model must not be longer than 100 characters")
        String model,

        @Schema(example = "18500.00")
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Price: at most 10 integer digits and 2 fraction digits")
        BigDecimal price,

        @Schema(example = "2020")
        @NotNull(message = "Production year is required")
        @Min(value = 1886, message = "Production year must not be earlier than 1886")
        Integer year
) {
}
