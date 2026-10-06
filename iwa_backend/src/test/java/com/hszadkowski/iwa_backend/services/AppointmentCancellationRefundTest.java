package com.hszadkowski.iwa_backend.services;

import com.hszadkowski.iwa_backend.dto.appointment.BookAppointmentDto;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.models.Payment;
import com.hszadkowski.iwa_backend.models.Service;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.repos.AvailabilitySlotRepository;
import com.hszadkowski.iwa_backend.repos.PaymentRepository;
import com.hszadkowski.iwa_backend.repos.ServiceRepository;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.implementations.AppointmentServiceImpl;
import com.hszadkowski.iwa_backend.services.interfaces.AppointmentService;
import com.hszadkowski.iwa_backend.services.interfaces.ContractService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailTemplateService;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import com.hszadkowski.iwa_backend.services.interfaces.PayUService;
import com.hszadkowski.iwa_backend.services.interfaces.SmsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

// The service manages its own transactions, so the test-managed one is switched off
// to observe what really runs inside and outside of them.
@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import(AppointmentServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentCancellationRefundTest {

    private static final String PAYU_ORDER_ID = "ORDER-1";

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentStatusRepository appointmentStatusRepository;
    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private SmsService smsService;
    @MockitoBean
    private EmailTemplateService emailTemplateService;
    @MockitoBean
    private GoogleCalendarService googleCalendarService;
    @MockitoBean
    private PayUService payUService;
    @MockitoBean
    private ContractService contractService;

    @AfterEach
    void tearDown() {
        // Bulk deletes: loading Payment and Appointment as entities to remove them
        // trips over their bidirectional one-to-one reference
        paymentRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        availabilitySlotRepository.deleteAll();
        userRepository.deleteAll();
        serviceRepository.deleteAll();
        appointmentStatusRepository.deleteAll();
    }

    @Test
    void cancellingPaidAppointmentRefundsAfterCancellationIsCommitted() {
        saveStatus("CONFIRMED");
        saveStatus("CANCELLED");

        Service service = serviceRepository.save(Service.builder()
                .name("Basic Makeup")
                .durationMin(60)
                .price(new BigDecimal("50.00"))
                .build());
        AppUser admin = userRepository.save(AppUser.builder()
                .name("Alice").surname("Administrator").email("alice@test.local").role("ROLE_ADMIN").build());
        AppUser customer = userRepository.save(AppUser.builder()
                .name("Bob").surname("Customer").email("bob@test.local").role("ROLE_USER").build());

        // More than 24h ahead, so the cancellation qualifies for a full refund
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setAppUser(admin);
        slot.setService(service);
        slot.setStartTime(LocalDateTime.now().plusDays(3));
        slot.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        slot.setIsBooked(false);
        Integer slotId = availabilitySlotRepository.save(slot).getSlotId();

        Integer appointmentId = appointmentService.bookAppointment(
                new BookAppointmentDto(slotId, service.getServiceId(), "Studio", null, true),
                customer.getEmail()).getAppointmentId();

        Payment payment = new Payment();
        payment.setAppointment(appointmentRepository.findById(appointmentId).orElseThrow());
        payment.setAppUser(customer);
        payment.setTransactionId(PAYU_ORDER_ID);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setStatus("COMPLETED");
        paymentRepository.save(payment);

        AtomicBoolean transactionActiveDuringRefund = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActiveDuringRefund.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(payUService).refundTransaction(anyString(), any(), anyString());

        appointmentService.cancelAppointment(appointmentId, customer.getEmail());

        verify(payUService).refundTransaction(
                eq(PAYU_ORDER_ID),
                argThat(amount -> amount.compareTo(new BigDecimal("50.00")) == 0),
                anyString());
        assertThat(transactionActiveDuringRefund).isFalse();

        Appointment cancelled = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(cancelled.getStatus().getName()).isEqualTo("CANCELLED");
        assertThat(paymentRepository.findByTransactionId(PAYU_ORDER_ID).orElseThrow().getStatus())
                .isEqualTo("REFUNDED");
        assertThat(availabilitySlotRepository.findById(slotId).orElseThrow().getIsBooked()).isFalse();
    }

    private void saveStatus(String name) {
        AppointmentStatus status = new AppointmentStatus();
        status.setName(name);
        appointmentStatusRepository.save(status);
    }
}
