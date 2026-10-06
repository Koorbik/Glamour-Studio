package com.hszadkowski.iwa_backend.services;

import com.hszadkowski.iwa_backend.dto.appointment.BookAppointmentDto;
import com.hszadkowski.iwa_backend.dto.appointment.RescheduleAppointmentDto;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.models.Service;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.repos.AvailabilitySlotRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Each attempt must run in its own transaction on its own connection, so the
// test-managed transaction that @DataJpaTest normally wraps around a test is switched off.
@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import(AppointmentServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentBookingConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentStatusRepository appointmentStatusRepository;
    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;
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
        AppointmentStatus confirmed = new AppointmentStatus();
        confirmed.setName("CONFIRMED");
        appointmentStatusRepository.save(confirmed);

        service = serviceRepository.save(Service.builder()
                .name("Basic Makeup")
                .durationMin(60)
                .price(new BigDecimal("50.00"))
                .build());

        admin = createUser("alice@test.local", "ROLE_ADMIN");
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        userRepository.deleteAll();
        serviceRepository.deleteAll();
        appointmentStatusRepository.deleteAll();
    }

    @Test
    void concurrentBookingsOfSameSlotCreateExactlyOneAppointment() throws Exception {
        Integer slotId = createFreeSlot(1);

        List<Runnable> attempts = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String email = createUser("customer" + i + "@test.local", "ROLE_USER").getEmail();
            attempts.add(() -> appointmentService.bookAppointment(bookingRequest(slotId), email));
        }

        assertThat(countSuccessfulAttempts(attempts)).isEqualTo(1);
        assertThat(appointmentsForSlot(slotId)).hasSize(1);
        assertThat(isBooked(slotId)).isTrue();
    }

    @Test
    void rescheduleRacingWithBookingsForSameSlotLeavesExactlyOneWinner() throws Exception {
        Integer originalSlotId = createFreeSlot(1);
        Integer contestedSlotId = createFreeSlot(2);

        String reschedulerEmail = createUser("rescheduler@test.local", "ROLE_USER").getEmail();
        Integer appointmentId = appointmentService
                .bookAppointment(bookingRequest(originalSlotId), reschedulerEmail)
                .getAppointmentId();

        List<Runnable> attempts = new ArrayList<>();
        attempts.add(() -> appointmentService.rescheduleAppointment(appointmentId,
                new RescheduleAppointmentDto(contestedSlotId, service.getServiceId()), reschedulerEmail));
        for (int i = 1; i < CONCURRENT_REQUESTS; i++) {
            String email = createUser("customer" + i + "@test.local", "ROLE_USER").getEmail();
            attempts.add(() -> appointmentService.bookAppointment(bookingRequest(contestedSlotId), email));
        }

        assertThat(countSuccessfulAttempts(attempts)).isEqualTo(1);
        assertThat(appointmentsForSlot(contestedSlotId)).hasSize(1);
        assertThat(isBooked(contestedSlotId)).isTrue();
        // The original slot is released only if the reschedule was the attempt that won
        assertThat(isBooked(originalSlotId)).isEqualTo(appointmentsForSlot(originalSlotId).size() == 1);
    }

    private int countSuccessfulAttempts(List<Runnable> attempts) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch allReady = new CountDownLatch(attempts.size());
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (Runnable attempt : attempts) {
            results.add(executor.submit(() -> {
                allReady.countDown();
                startSignal.await();
                try {
                    attempt.run();
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }

        assertThat(allReady.await(10, TimeUnit.SECONDS)).isTrue();
        startSignal.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        executor.shutdown();
        return successes;
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

    private BookAppointmentDto bookingRequest(Integer slotId) {
        return new BookAppointmentDto(slotId, service.getServiceId(), "Studio", null, true);
    }

    private List<Appointment> appointmentsForSlot(Integer slotId) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getSlot().getSlotId().equals(slotId))
                .toList();
    }

    private boolean isBooked(Integer slotId) {
        return availabilitySlotRepository.findById(slotId).orElseThrow().getIsBooked();
    }
}
