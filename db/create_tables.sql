CREATE SCHEMA IF NOT EXISTS "public";

CREATE TYPE notification_type AS ENUM ('reminder', 'confirmation', 'cancellation');

CREATE TABLE "public"."app_users" (
                                  "app_user_id" SERIAL PRIMARY KEY,
                                  "name" varchar NOT NULL,
                                  "surname" varchar NOT NULL,
                                  "email" varchar UNIQUE,
                                  "phone_num" varchar,
                                  "password_hash" varchar NOT NULL,
                                  "role" varchar NOT NULL DEFAULT 'user'
);

CREATE TABLE "public"."appointment_statuses" (
                                                 "status_id" SERIAL PRIMARY KEY,
                                                 "name" varchar NOT NULL
);

CREATE TABLE "public"."services" (
                                     "service_id" SERIAL PRIMARY KEY,
                                     "name" varchar,
                                     "description" text,
                                     "duration_min" integer,
                                     "price" decimal
);

CREATE TABLE "public"."appointments" (
                                         "appointment_id" SERIAL PRIMARY KEY,
                                         "app_user_id" integer NOT NULL,
                                         "service_id" integer,
                                         "status_id" integer NOT NULL,
                                         "location" varchar NOT NULL,
                                         "scheduled_at" date NOT NULL,
                                         "description" text,
                                         CONSTRAINT fk_appointments_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id"),
                                         CONSTRAINT fk_appointments_status FOREIGN KEY ("status_id") REFERENCES "public"."appointment_statuses"("status_id"),
                                         CONSTRAINT fk_appointments_service FOREIGN KEY ("service_id") REFERENCES "public"."services"("service_id")
);

CREATE TABLE "public"."payments" (
                                     "payment_id" SERIAL PRIMARY KEY,
                                     "appointment_id" integer NOT NULL,
                                     "app_user_id" integer NOT NULL,
                                     "amount" decimal NOT NULL,
                                     "status" varchar NOT NULL,
                                     "transaction_id" varchar UNIQUE,
                                     "paid_at" timestamp,
                                     "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_payments_appointment FOREIGN KEY ("appointment_id") REFERENCES "public"."appointments"("appointment_id"),
                                     CONSTRAINT fk_payments_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id")
);

CREATE TABLE "public"."availability_slots" (
                                               "slot_id" SERIAL PRIMARY KEY,
                                               "app_user_id" integer NOT NULL,
                                               "service_id" integer NOT NULL,
                                               "start_time" timestamp NOT NULL,
                                               "end_time" timestamp NOT NULL,
                                               "is_booked" boolean NOT NULL,
                                               CONSTRAINT fk_slots_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id"),
                                               CONSTRAINT fk_slots_service FOREIGN KEY ("service_id") REFERENCES "public"."services"("service_id")
);

CREATE TABLE "public"."notifications" (
                                          "notif_id" SERIAL PRIMARY KEY,
                                          "app_user_id" integer NOT NULL,
                                          "appointment_id" integer NOT NULL,
                                          "type" notification_type NOT NULL,
                                          "message" text NOT NULL,
                                          "sent_at" timestamp NOT NULL,
                                          CONSTRAINT fk_notifications_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id"),
                                          CONSTRAINT fk_notifications_appointment FOREIGN KEY ("appointment_id") REFERENCES "public"."appointments"("appointment_id")
);

CREATE TABLE "public"."reviews" (
                                    "review_id" SERIAL PRIMARY KEY,
                                    "appointment_id" integer NOT NULL,
                                    "app_user_id" integer NOT NULL,
                                    "rating" integer NOT NULL,
                                    "comment" text NOT NULL,
                                    "created_at" timestamp NOT NULL,
                                    CONSTRAINT fk_reviews_appointment FOREIGN KEY ("appointment_id") REFERENCES "public"."appointments"("appointment_id"),
                                    CONSTRAINT fk_reviews_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id")
);

CREATE TABLE "public"."calendar_tokens" (
                                            "token_id" SERIAL PRIMARY KEY,
                                            "app_user_id" integer NOT NULL,
                                            "provider" varchar NOT NULL,
                                            "access_token" text NOT NULL,
                                            "refresh_token" text,
                                            "expires_at" timestamp,
                                            "email" varchar,
                                            CONSTRAINT fk_calendar_tokens_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id")
);

CREATE TABLE "public"."calendar_events" (
                                            "calendar_event_id" SERIAL PRIMARY KEY,
                                            "appointment_id" integer NOT NULL,
                                            "app_user_id" integer NOT NULL,
                                            "provider" varchar NOT NULL,
                                            "external_event_id" varchar NOT NULL,
                                            "calendar_id" varchar NOT NULL,
                                            "synced" boolean NOT NULL DEFAULT true,
                                            CONSTRAINT fk_calendar_events_appointment FOREIGN KEY ("appointment_id") REFERENCES "public"."appointments"("appointment_id"),
                                            CONSTRAINT fk_calendar_events_user FOREIGN KEY ("app_user_id") REFERENCES "public"."app_users"("app_user_id")
);

ALTER TABLE appointments ADD COLUMN slot_id INTEGER;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_slot
    FOREIGN KEY (slot_id) REFERENCES availability_slots(slot_id);
