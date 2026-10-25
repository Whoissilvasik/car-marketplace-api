package com.example.carmarketplace.service;

import com.example.carmarketplace.dto.CarRequest;
import com.example.carmarketplace.entity.Car;
import com.example.carmarketplace.exception.CarNotFoundException;
import com.example.carmarketplace.exception.InvalidCarDataException;
import com.example.carmarketplace.repository.CarRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;

@Service
@Transactional(readOnly = true)
public class CarService {

    private static final int FIRST_CAR_YEAR = 1886;

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Transactional
    public Car create(CarRequest request) {
        validate(request.brand(), request.model(), request.price(), request.year());
        Car car = new Car(request.brand().trim(), request.model().trim(), request.price(), request.year());
        return carRepository.save(car);
    }

    public Car getById(Long id) {
        return carRepository.findById(id).orElseThrow(() -> new CarNotFoundException(id));
    }

    public Page<Car> getAll(Pageable pageable) {
        return carRepository.findAll(pageable);
    }

    @Transactional
    public Car update(Long id, CarRequest request) {
        validate(request.brand(), request.model(), request.price(), request.year());
        Car car = getById(id);
        car.setBrand(request.brand().trim());
        car.setModel(request.model().trim());
        car.setPrice(request.price());
        car.setYear(request.year());
        return carRepository.save(car);
    }

    @Transactional
    public Car updatePrice(Long id, BigDecimal newPrice) {
        validatePrice(newPrice);
        Car car = getById(id);
        car.setPrice(newPrice);
        return carRepository.save(car);
    }

    @Transactional
    public void delete(Long id) {
        if (!carRepository.existsById(id)) {
            throw new CarNotFoundException(id);
        }
        carRepository.deleteById(id);
    }

    public Page<Car> search(String brand, BigDecimal minPrice, BigDecimal maxPrice,
                            Integer year, Integer minYear, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidCarDataException("minPrice must not be greater than maxPrice");
        }
        if (year != null && minYear != null && year < minYear) {
            throw new InvalidCarDataException("year must not be less than minYear");
        }

        Specification<Car> specification = Specification.where(null);

        if (brand != null && !brand.isBlank()) {
            String normalizedBrand = brand.trim().toLowerCase();
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("brand")), normalizedBrand));
        }
        if (minPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (year != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("year"), year));
        }
        if (minYear != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("year"), minYear));
        }

        return carRepository.findAll(specification, pageable);
    }

    private void validate(String brand, String model, BigDecimal price, Integer year) {
        if (brand == null || brand.isBlank()) {
            throw new InvalidCarDataException("Brand must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new InvalidCarDataException("Model must not be blank");
        }
        validatePrice(price);
        int maxYear = Year.now().getValue() + 1;
        if (year == null || year < FIRST_CAR_YEAR || year > maxYear) {
            throw new InvalidCarDataException(
                    "Production year must be between " + FIRST_CAR_YEAR + " and " + maxYear);
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new InvalidCarDataException("Price must be greater than zero");
        }
    }
}
