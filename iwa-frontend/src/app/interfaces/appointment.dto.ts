import {ReviewResponseDto} from './review.dto';

export interface AppointmentResponseDto {
  appointmentId: number;
  userId?: number;
  userName: string;
  serviceId?: number;
  serviceName: string;
  serviceDescription?: string;
  serviceDurationMin?: number;
  servicePrice?: number;
  status: string;
  location: string;
  scheduledAt: string;
  description: string;
  calendarEventId?: string;
  calendarSyncStatus?: 'SYNCED' | 'PENDING' | 'FAILED' | 'NOT_SYNCED';
  lastCalendarSync?: string;
  paymentStatus?: string;
  paymentMethod?: string;
  review?: ReviewResponseDto;
}

export interface BookAppointmentDto {
  slotId: number;
  serviceId: number;
  location: string;
  description?: string;
  acceptsTerms: boolean;
}

export interface RescheduleAppointmentDto {
  newSlotId: number;
  serviceId: number;
}

export interface UpdateAppointmentStatusDto {
  status: string;
}
