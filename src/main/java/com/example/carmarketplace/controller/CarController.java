package com.example.carmarketplace.controller;

import com.example.carmarketplace.dto.CarRequest;
import com.example.carmarketplace.dto.CarResponse;
import com.example.carmarketplace.dto.PageResponse;
import com.example.carmarketplace.dto.PriceUpdateRequest;
import com.example.carmarketplace.entity.Car;
import com.example.carmarketplace.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/cars")
@Tag(name = "Cars", description = "Car sale listings")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    @Operation(summary = "List cars with pagination and sorting",
            description = "Parameters: page, size, sort (for example, sort=price,desc)")
    public PageResponse<CarResponse> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.from(carService.getAll(pageable).map(CarResponse::from));
    }

    @GetMapping("/search")
    @Operation(summary = "Search cars by optional filters")
    public PageResponse<CarResponse> search(
            @Parameter(description = "Brand (case-insensitive)") @RequestParam(required = false) String brand,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Exact production year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Minimum production year") @RequestParam(required = false) Integer minYear,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.from(
                carService.search(brand, minPrice, maxPrice, year, minYear, pageable).map(CarResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car by id")
    public CarResponse getById(@PathVariable Long id) {
        return CarResponse.from(carService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a car")
    public ResponseEntity<CarResponse> create(@Valid @RequestBody CarRequest request) {
        Car created = carService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(CarResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a car")
    public CarResponse update(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        return CarResponse.from(carService.update(id, request));
    }

    @PatchMapping("/{id}/price")
    @Operation(summary = "Update only the price of a car")
    public CarResponse updatePrice(@PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        return CarResponse.from(carService.updatePrice(id, request.price()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a car")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
