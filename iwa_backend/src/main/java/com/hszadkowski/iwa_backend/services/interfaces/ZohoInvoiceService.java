package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.models.Payment;

public interface ZohoInvoiceService {
    byte[] createAndDownloadInvoice(Payment payment);
}
