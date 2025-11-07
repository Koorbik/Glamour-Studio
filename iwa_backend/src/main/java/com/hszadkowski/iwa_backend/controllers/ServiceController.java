package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.service.CreateOrUpdateServiceDto;
import com.hszadkowski.iwa_backend.dto.service.ServiceResponseDto;
import com.hszadkowski.iwa_backend.services.interfaces.MakeUpServicesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final MakeUpServicesService makeUpServicesService;

    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> getAllServices() {
        return ResponseEntity.ok(makeUpServicesService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponseDto> getServiceById(@PathVariable Integer id) {
        return ResponseEntity.ok(makeUpServicesService.getServiceById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponseDto> createService(@RequestBody @Valid CreateOrUpdateServiceDto serviceDto) {

        ServiceResponseDto response = makeUpServicesService.createService(serviceDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponseDto> updateService(@PathVariable Integer id,
                                                            @RequestBody @Valid CreateOrUpdateServiceDto serviceDto) {
        return ResponseEntity.ok(makeUpServicesService.updateService(id, serviceDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Integer id) {
        makeUpServicesService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}

