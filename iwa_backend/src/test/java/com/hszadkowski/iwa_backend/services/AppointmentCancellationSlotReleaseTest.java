package com.hszadkowski.iwa_backend.services;

import com.hszadkowski.iwa_backend.dto.appointment.BookAppointmentDto;
import com.hszadkowski.iwa_backend.dto.appointment.UpdateAppointmentStatusDto;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.models.Payment;
import com.hszadkowski.iwa_backend.models.PaymentMethod;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Every service call must commit on its own, as separate requests do in production, so the
// test-managed transaction that @DataJpaTest normally wraps around a test is switched off.
@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import(AppointmentServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentCancellationSlotReleaseTest {

    private static final String ALICE = "alice@test.local";
    private static final String BOB = "bob@test.local";
    private static final String CAROL = "carol@test.local";

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

    private Service service;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        createStatus("CONFIRMED");
        createStatus("CANCELLED");

        service = serviceRepository.save(Service.builder()
                .name("Basic Makeup")
                .durationMin(60)
                .price(new BigDecimal("50.00"))
                .build());

        admin = createUser("admin@test.local", "ROLE_ADMIN");
        createUser(ALICE, "ROLE_USER");
        createUser(BOB, "ROLE_USER");
        createUser(CAROL, "ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        // Bulk delete: removing Payment entities one by one fails on flush because the
        // appointments loaded alongside them still reference the removed instances
        paymentRepository.deleteAllInBatch();
        appointmentRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        userRepository.deleteAll();
        serviceRepository.deleteAll();
        appointmentStatusRepository.deleteAll();
    }

    @Test
    void cancellingAnAlreadyCancelledAppointmentIsRejectedAndKeepsRebookedSlotTaken() {
        Integer slotId = createFreeSlot(1);
        Integer aliceAppointmentId = book(slotId, ALICE);
        appointmentService.cancelAppointment(aliceAppointmentId, ALICE);
        book(slotId, BOB);

        assertThatThrownBy(() -> appointmentService.cancelAppointment(aliceAppointmentId, ALICE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Appointment is already cancelled");
        assertThatThrownBy(() -> appointmentService.cancelAppointment(aliceAppointmentId, admin.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Appointment is already cancelled");

        assertSlotStillBelongsToBob(slotId);
    }

    @Test
    void settingStatusCancelledOnAlreadyCancelledAppointmentKeepsRebookedSlotTaken() {
        Integer slotId = createFreeSlot(1);
        Integer aliceAppointmentId = book(slotId, ALICE);
        appointmentService.cancelAppointment(aliceAppointmentId, ALICE);
        book(slotId, BOB);

        appointmentService.updateAppointmentStatus(aliceAppointmentId, new UpdateAppointmentStatusDto("CANCELLED"));

        assertSlotStillBelongsToBob(slotId);
    }

    @Test
    void cancelAfterStatusWasAlreadySetToCancelledKeepsRebookedSlotTaken() {
        Integer slotId = createFreeSlot(1);
        Integer aliceAppointmentId = book(slotId, ALICE);
        appointmentService.updateAppointmentStatus(aliceAppointmentId, new UpdateAppointmentStatusDto("CANCELLED"));
        assertThat(isBooked(slotId)).isFalse();
        book(slotId, BOB);

        assertThatThrownBy(() -> appointmentService.cancelAppointment(aliceAppointmentId, ALICE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Appointment is already cancelled");

        assertSlotStillBelongsToBob(slotId);
    }

    @Test
    void repeatedCancelOfPaidAppointmentDoesNotRequestAnotherRefund() {
        Integer slotId = createFreeSlot(3);
        Integer appointmentId = book(slotId, ALICE);
        Integer paymentId = createCompletedPayment(appointmentId);
        // A refund that fails leaves the payment COMPLETED, so the payment status alone
        // would let a second cancel ask PayU for the money again
        doThrow(new RuntimeException("PayU unavailable"))
                .when(payUService).refundTransaction(any(), any(), any());

        appointmentService.cancelAppointment(appointmentId, ALICE);
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus()).isEqualTo("COMPLETED");

        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, ALICE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Appointment is already cancelled");

        verify(payUService, times(1)).refundTransaction(any(), any(), any());
    }

    private void assertSlotStillBelongsToBob(Integer slotId) {
        assertThat(isBooked(slotId)).isTrue();
        assertThatThrownBy(() -> book(slotId, CAROL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("This time slot is no longer available or has already passed");
        assertThat(activeBookersOf(slotId)).containsExactly(BOB);
    }

    private Integer book(Integer slotId, String email) {
        return appointmentService
                .bookAppointment(new BookAppointmentDto(slotId, service.getServiceId(), "Studio", null, true), email)
                .getAppointmentId();
    }

    private void createStatus(String name) {
        AppointmentStatus status = new AppointmentStatus();
        status.setName(name);
        appointmentStatusRepository.save(status);
    }

    private AppUser createUser(String email, String role) {
        return userRepository.save(AppUser.builder()
                .name("Test").surname("User").email(email).role(role).build());
    }

    private Integer createFreeSlot(int daysFromNow) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setAppUser(admin);
        slot.setService(service);
        slot.setStartTime(LocalDateTime.now().plusDays(daysFromNow));
        slot.setEndTime(LocalDateTime.now().plusDays(daysFromNow).plusHours(1));
        slot.setIsBooked(false);
        return availabilitySlotRepository.save(slot).getSlotId();
    }

    private Integer createCompletedPayment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();

        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAppUser(appointment.getAppUser());
        payment.setTransactionId("ORDER-1");
        payment.setAmount(service.getPrice());
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod(PaymentMethod.PAYU);
        return paymentRepository.save(payment).getPaymentId();
    }

    private List<String> activeBookersOf(Integer slotId) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getSlot().getSlotId().equals(slotId))
                .filter(appointment -> !"CANCELLED".equals(appointment.getStatus().getName()))
                .map(appointment -> appointment.getAppUser().getEmail())
                .toList();
    }

    private boolean isBooked(Integer slotId) {
        return availabilitySlotRepository.findById(slotId).orElseThrow().getIsBooked();
    }
}
