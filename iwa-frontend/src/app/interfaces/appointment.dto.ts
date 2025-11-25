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
}

export interface BookAppointmentDto {
  slotId: number;
  serviceId: number;
  location: string;
  description?: string;
}

export interface RescheduleAppointmentDto {
  newSlotId: number;
  serviceId: number;
}

export interface UpdateAppointmentStatusDto {
  status: string;
}
