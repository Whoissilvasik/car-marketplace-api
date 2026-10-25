package com.example.carmarketplace.controller;

import com.example.carmarketplace.dto.CarRequest;
import com.example.carmarketplace.entity.Car;
import com.example.carmarketplace.exception.CarNotFoundException;
import com.example.carmarketplace.exception.GlobalExceptionHandler;
import com.example.carmarketplace.service.CarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarController.class)
@Import(GlobalExceptionHandler.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CarService carService;

    private static Car car(Long id, String brand, String model, String price, int year) {
        Car car = new Car(brand, model, new BigDecimal(price), year);
        car.setId(id);
        return car;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void getAll_returnsPagedResponse() throws Exception {
        PageImpl<Car> page = new PageImpl<>(
                List.of(car(1L, "Toyota", "Camry", "18500.00", 2020)), PageRequest.of(0, 20), 1);
        when(carService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        when(carService.getById(1L)).thenReturn(car(1L, "Toyota", "Camry", "18500.00", 2020));

        mockMvc.perform(get("/cars/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Camry"))
                .andExpect(jsonPath("$.year").value(2020));
    }

    @Test
    void getById_missing_returns404WithJsonError() throws Exception {
        when(carService.getById(99L)).thenThrow(new CarNotFoundException(99L));

        mockMvc.perform(get("/cars/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/cars/99"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/cars/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_valid_returns201WithLocation() throws Exception {
        CarRequest request = new CarRequest("Toyota", "Camry", new BigDecimal("18500.00"), 2020);
        when(carService.create(any(CarRequest.class))).thenReturn(car(5L, "Toyota", "Camry", "18500.00", 2020));

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/cars/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.brand").value("Toyota"));
    }

    @Test
    void create_invalidBody_returns400WithFieldDetails() throws Exception {
        String body = "{\"brand\":\"\",\"model\":\"Camry\",\"price\":-10,\"year\":2020}";

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(2));

        verify(carService, never()).create(any());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brand\": \"Toyota\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void create_wrongFieldType_returns400() throws Exception {
        String body = "{\"brand\":\"Toyota\",\"model\":\"Camry\",\"price\":\"cheap\",\"year\":2020}";

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_valid_returns200() throws Exception {
        CarRequest request = new CarRequest("Honda", "Civic", new BigDecimal("15000.00"), 2018);
        when(carService.update(eq(1L), any(CarRequest.class)))
                .thenReturn(car(1L, "Honda", "Civic", "15000.00", 2018));

        mockMvc.perform(put("/cars/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.year").value(2018));
    }

    @Test
    void update_missing_returns404() throws Exception {
        CarRequest request = new CarRequest("Honda", "Civic", new BigDecimal("15000.00"), 2018);
        when(carService.update(eq(42L), any(CarRequest.class))).thenThrow(new CarNotFoundException(42L));

        mockMvc.perform(put("/cars/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePrice_valid_returns200() throws Exception {
        when(carService.updatePrice(eq(1L), any(BigDecimal.class)))
                .thenReturn(car(1L, "Toyota", "Camry", "17000.00", 2020));

        mockMvc.perform(patch("/cars/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":17000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(17000.00));
    }

    @Test
    void updatePrice_missingPrice_returns400() throws Exception {
        mockMvc.perform(patch("/cars/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/cars/1"))
                .andExpect(status().isNoContent());

        verify(carService).delete(1L);
    }

    @Test
    void delete_missing_returns404() throws Exception {
        doThrow(new CarNotFoundException(7L)).when(carService).delete(7L);

        mockMvc.perform(delete("/cars/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void search_withFilters_returns200() throws Exception {
        PageImpl<Car> page = new PageImpl<>(
                List.of(car(1L, "Toyota", "Camry", "18500.00", 2020)), PageRequest.of(0, 20), 1);
        when(carService.search(eq("Toyota"), any(), any(), any(), eq(2015), any())).thenReturn(page);

        mockMvc.perform(get("/cars/search")
                        .param("brand", "Toyota")
                        .param("maxPrice", "20000")
                        .param("minYear", "2015"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_invalidNumberParam_returns400() throws Exception {
        mockMvc.perform(get("/cars/search").param("minPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
