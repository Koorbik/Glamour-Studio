INSERT INTO app_users
(name, surname, email, phone_num, password_hash, role,
 verification_code, verification_code_expires_at, enabled)
VALUES
    (
        'Alice',
        'Administrator',
        'alice@acme.com',
        '600123456',
        '{bcrypt}$2a$12$CujBhdicFbBBOLbJr7yPSeqw76PGW6AI2.ohWqdT0VE4Olcbutd5K',  -- BCrypt hash of "admin123"
        'ROLE_ADMIN',
        NULL,
        NULL,
        TRUE
    );

INSERT INTO app_users
(name, surname, email, phone_num, password_hash, role,
 verification_code, verification_code_expires_at, enabled)
VALUES
    (
        'Bob',
        'Customer',
        'bob@acme.com',
        '600654321',
        '{bcrypt}$2a$12$NWg8doOoSXA8HpSUDv/s2ujwBANtbdkp5qQP0RtsZfro3jER6Whgm',  -- BCrypt hash of "user123"
        'ROLE_USER',
        NULL,
        NULL,
        TRUE
    );

-- Add appointment statuses
INSERT INTO appointment_statuses (name) VALUES 
('PENDING'),
('CONFIRMED'), 
('COMPLETED'),
('CANCELLED');

-- Add sample services
INSERT INTO services (name, description, duration_min, price) VALUES
('Basic Makeup', 'Natural everyday makeup application', 60, 50.00),
('Evening Makeup', 'Glamorous makeup for special occasions', 90, 80.00),
('Bridal Makeup', 'Complete bridal makeup package', 120, 150.00),
('Makeup Lesson', 'Learn to do your own makeup', 90, 70.00);

-- Basic Makeup slots (service_id = 1)
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 1, '2026-01-30 09:00:00', '2026-01-30 10:00:00', false),
(1, 1, '2026-01-30 10:30:00', '2026-01-30 11:30:00', false),
(1, 1, '2026-02-02 14:00:00', '2026-02-02 15:00:00', false),
(1, 1, '2026-02-01 09:00:00', '2026-02-01 10:00:00', false);

-- Evening Makeup slots (service_id = 2)
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 2, '2026-01-10 16:00:00', '2026-01-10 17:30:00', false),
(1, 2, '2026-01-11 17:00:00', '2026-01-11 18:30:00', false),
(1, 2, '2026-01-13 16:00:00', '2026-01-13 17:30:00', false);

-- Bridal Makeup slots (service_id = 3)
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 3, '2026-01-15 08:00:00', '2026-01-15 10:00:00', false),
(1, 3, '2026-01-16 09:00:00', '2026-01-16 11:00:00', false),
(1, 3, '2026-01-20 08:00:00', '2026-01-20 10:00:00', false);

-- Makeup Lesson slots (service_id = 4)
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 4, '2026-01-10 13:00:00', '2026-01-10 14:30:00', false),
(1, 4, '2026-01-12 15:00:00', '2026-01-12 16:30:00', false);

-- Add one already booked slot for testing
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 1, '2026-01-10 12:00:00', '2026-01-10 13:00:00', true);

-- Add one slot in the past for testing (should not be bookable)
INSERT INTO availability_slots (app_user_id, service_id, start_time, end_time, is_booked) VALUES
(1, 1, '2025-05-01 09:00:00', '2025-05-01 10:00:00', false);
