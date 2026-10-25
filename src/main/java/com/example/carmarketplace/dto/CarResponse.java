package com.example.carmarketplace.dto;

import com.example.carmarketplace.entity.Car;

import java.math.BigDecimal;

public record CarResponse(Long id, String brand, String model, BigDecimal price, int year) {

    public static CarResponse from(Car car) {
        return new CarResponse(car.getId(), car.getBrand(), car.getModel(), car.getPrice(), car.getYear());
    }
}
