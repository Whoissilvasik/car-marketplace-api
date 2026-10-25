package com.example.carmarketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "New price of the car")
public record PriceUpdateRequest(

        @Schema(example = "17900.00")
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Price: at most 10 integer digits and 2 fraction digits")
        BigDecimal price
) {
}
