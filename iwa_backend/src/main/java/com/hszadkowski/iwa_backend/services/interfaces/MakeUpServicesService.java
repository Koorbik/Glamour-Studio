package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.service.CreateOrUpdateServiceDto;
import com.hszadkowski.iwa_backend.dto.service.ServiceResponseDto;

import java.util.List;

public interface MakeUpServicesService {

    ServiceResponseDto createService(CreateOrUpdateServiceDto request);

    ServiceResponseDto updateService(Integer serviceId, CreateOrUpdateServiceDto request);

    ServiceResponseDto getServiceById(Integer serviceId);

    List<ServiceResponseDto> getAllServices();

    void deleteService(Integer serviceId);
}
