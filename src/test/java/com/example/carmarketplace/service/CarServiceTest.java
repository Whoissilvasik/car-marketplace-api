package com.example.carmarketplace.service;

import com.example.carmarketplace.dto.CarRequest;
import com.example.carmarketplace.entity.Car;
import com.example.carmarketplace.exception.CarNotFoundException;
import com.example.carmarketplace.exception.InvalidCarDataException;
import com.example.carmarketplace.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    private static CarRequest validRequest() {
        return new CarRequest("Toyota", "Camry", new BigDecimal("18500.00"), 2020);
    }

    private static Car existingCar() {
        Car car = new Car("Toyota", "Camry", new BigDecimal("18500.00"), 2020);
        car.setId(1L);
        return car;
    }
    @Test
    void create_savesTrimmedCarAndReturnsIt() {
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> {
            Car car = invocation.getArgument(0);
            car.setId(10L);
            return car;
        });

        Car created = carService.create(new CarRequest("  Toyota ", " Camry  ", new BigDecimal("18500.00"), 2020));

        assertEquals(10L, created.getId());
        assertEquals("Toyota", created.getBrand());
        assertEquals("Camry", created.getModel());
        assertEquals(new BigDecimal("18500.00"), created.getPrice());
        assertEquals(2020, created.getYear());
    }

    @Test
    void create_blankBrand_throwsAndDoesNotSave() {
        CarRequest request = new CarRequest("  ", "Camry", new BigDecimal("100"), 2020);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void create_blankModel_throwsAndDoesNotSave() {
        CarRequest request = new CarRequest("Toyota", "", new BigDecimal("100"), 2020);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void create_negativePrice_throwsAndDoesNotSave() {
        CarRequest request = new CarRequest("Toyota", "Camry", new BigDecimal("-1"), 2020);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void create_zeroPrice_throws() {
        CarRequest request = new CarRequest("Toyota", "Camry", BigDecimal.ZERO, 2020);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
    }

    @Test
    void create_yearTooOld_throws() {
        CarRequest request = new CarRequest("Toyota", "Camry", new BigDecimal("100"), 1800);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
    }

    @Test
    void create_yearTooFarInFuture_throws() {
        int tooFar = Year.now().getValue() + 2;
        CarRequest request = new CarRequest("Toyota", "Camry", new BigDecimal("100"), tooFar);

        assertThrows(InvalidCarDataException.class, () -> carService.create(request));
    }
    @Test
    void getById_existingCar_returnsCar() {
        Car car = existingCar();
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertSame(car, carService.getById(1L));
    }

    @Test
    void getById_missingCar_throwsNotFound() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class, () -> carService.getById(99L));
    }

    @Test
    void getAll_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Car> page = new PageImpl<>(List.of(existingCar()), pageable, 1);
        when(carRepository.findAll(pageable)).thenReturn(page);

        Page<Car> result = carService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Toyota", result.getContent().get(0).getBrand());
    }
    @Test
    void update_existingCar_changesAllFields() {
        Car car = existingCar();
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);

        Car updated = carService.update(1L, new CarRequest("Honda", "Civic", new BigDecimal("15000"), 2018));

        assertEquals("Honda", updated.getBrand());
        assertEquals("Civic", updated.getModel());
        assertEquals(new BigDecimal("15000"), updated.getPrice());
        assertEquals(2018, updated.getYear());
    }

    @Test
    void update_missingCar_throwsNotFound() {
        when(carRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class, () -> carService.update(5L, validRequest()));
        verify(carRepository, never()).save(any());
    }

    @Test
    void update_invalidData_throwsBeforeTouchingRepository() {
        CarRequest request = new CarRequest("", "Camry", new BigDecimal("100"), 2020);

        assertThrows(InvalidCarDataException.class, () -> carService.update(1L, request));
        verify(carRepository, never()).findById(any());
    }

    @Test
    void updatePrice_existingCar_changesOnlyPrice() {
        Car car = existingCar();
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);

        Car updated = carService.updatePrice(1L, new BigDecimal("17000.00"));

        assertEquals(new BigDecimal("17000.00"), updated.getPrice());
        assertEquals("Toyota", updated.getBrand());
    }

    @Test
    void updatePrice_negativePrice_throws() {
        assertThrows(InvalidCarDataException.class, () -> carService.updatePrice(1L, new BigDecimal("-5")));
        verify(carRepository, never()).save(any());
    }
    @Test
    void delete_existingCar_deletesById() {
        when(carRepository.existsById(1L)).thenReturn(true);

        carService.delete(1L);

        verify(carRepository).deleteById(1L);
    }

    @Test
    void delete_missingCar_throwsNotFound() {
        when(carRepository.existsById(7L)).thenReturn(false);

        assertThrows(CarNotFoundException.class, () -> carService.delete(7L));
        verify(carRepository, never()).deleteById(any());
    }
    @Test
    @SuppressWarnings("unchecked")
    void search_withFilters_delegatesToRepositoryWithSpecification() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Car> page = new PageImpl<>(List.of(existingCar()), pageable, 1);
        when(carRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Car> result = carService.search("toyota", new BigDecimal("1000"), new BigDecimal("30000"),
                null, 2015, pageable);

        assertEquals(1, result.getTotalElements());
        verify(carRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_withoutFilters_stillQueriesRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(carRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<Car> result = carService.search(null, null, null, null, null, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void search_minPriceGreaterThanMaxPrice_throws() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(InvalidCarDataException.class, () -> carService.search(
                null, new BigDecimal("500"), new BigDecimal("100"), null, null, pageable));
    }

    @Test
    void search_yearLessThanMinYear_throws() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(InvalidCarDataException.class, () -> carService.search(
                null, null, null, 2010, 2015, pageable));
    }
}
