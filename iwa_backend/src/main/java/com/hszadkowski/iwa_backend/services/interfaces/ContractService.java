package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.models.Appointment;

public interface ContractService {
    byte[] generateContract(Appointment appointment);
}