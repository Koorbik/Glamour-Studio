package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.service.CreateOrUpdateServiceDto;
import com.hszadkowski.iwa_backend.dto.service.ServiceResponseDto;
import com.hszadkowski.iwa_backend.exceptions.ServiceDoesNotExistException;
import com.hszadkowski.iwa_backend.models.Service;
import com.hszadkowski.iwa_backend.repos.ServiceRepository;
import com.hszadkowski.iwa_backend.services.interfaces.MakeUpServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class MakeUpServicesServiceImpl implements MakeUpServicesService {
    private final ServiceRepository serviceRepository;

    @Override
    public ServiceResponseDto createService(CreateOrUpdateServiceDto request) {

        Service appService = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .durationMin(request.getMinDuration())
                .price(request.getPrice())
                .build();

        Service savedService = serviceRepository.save(appService);

        return mapToResponseDto(savedService);
    }

    @Override
    public ServiceResponseDto updateService(Integer serviceId, CreateOrUpdateServiceDto request) {

        Service toUpdate = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceDoesNotExistException(
                        "Service with ID " + serviceId + " does not exist in the Database"));

        toUpdate.setName(request.getName());
        toUpdate.setDescription(request.getDescription());
        toUpdate.setDurationMin(request.getMinDuration());
        toUpdate.setPrice(request.getPrice());

        Service updatedService = serviceRepository.save(toUpdate);
        return mapToResponseDto(updatedService);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponseDto getServiceById(Integer serviceId) {
        return serviceRepository.findById(serviceId)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new ServiceDoesNotExistException(
                        "Service with ID " + serviceId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponseDto> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteService(Integer serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ServiceDoesNotExistException(
                    "Cannot delete service with ID " + serviceId + " - service not found");
        }
        serviceRepository.deleteById(serviceId);
    }

    private ServiceResponseDto mapToResponseDto(Service service) {
        return new ServiceResponseDto(
                service.getServiceId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMin(),
                service.getPrice()
        );
    }
}
