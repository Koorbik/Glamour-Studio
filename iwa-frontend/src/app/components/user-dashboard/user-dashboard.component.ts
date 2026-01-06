import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { GoogleCalendarService, GoogleCalendarSyncStatus } from '../../services/google-calendar.service';
import { PaymentService } from '../../services/payment.service';
import { AppointmentResponseDto } from '../../interfaces/appointment.dto';
import { RescheduleDialogComponent } from './reschedule-dialog/reschedule-dialog.component';
import { CancelConfirmDialogComponent } from './cancel-confirm-dialog/cancel-confirm-dialog.component';

interface AppointmentWithSyncStatus extends AppointmentResponseDto {
  isSyncing?: boolean;
  calendarEventId?: string;
}

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatBadgeModule,
    MatTooltipModule,
    MatDividerModule,
    MatSnackBarModule
  ],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss']
})
export class UserDashboardComponent implements OnInit {
  appointments = signal<AppointmentWithSyncStatus[]>([]);
  isLoading = signal(true);
  selectedTab = signal(0);
  isProcessingPayment = signal<number | null>(null);

  // Google Calendar sync status
  calendarSyncStatus = signal<GoogleCalendarSyncStatus>({
    isSynced: false,
    syncEnabled: false
  });

  upcomingAppointments = computed(() =>
    this.appointments()
      .filter(apt => apt.status !== 'CANCELLED' && apt.status !== 'COMPLETED')
      .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime())
  );

  pastAppointments = computed(() =>
    this.appointments()
      .filter(apt => apt.status === 'COMPLETED' || apt.status === 'CANCELLED')
      .sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime())
  );

  constructor(
    private apiService: ApiService,
    private googleCalendarService: GoogleCalendarService,
    private paymentService: PaymentService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAppointments();
    this.loadCalendarSyncStatus();
  }

  loadAppointments(): void {
    this.isLoading.set(true);
    this.apiService.get<AppointmentWithSyncStatus[]>('appointments/my').subscribe({
      next: (data) => {
        this.appointments.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch appointments', err);
        this.isLoading.set(false);
      }
    });
  }

  loadCalendarSyncStatus(): void {
    this.googleCalendarService.getSyncStatus().subscribe({
      next: (status) => {
        this.calendarSyncStatus.set(status);
      },
      error: (err) => {
        console.error('Failed to fetch calendar sync status', err);
      }
    });
  }

  getStatusIcon(status: string): string {
    const statusIcons: { [key: string]: string } = {
      'SCHEDULED': 'event_available',
      'CONFIRMED': 'check_circle',
      'IN_PROGRESS': 'pending',
      'COMPLETED': 'task_alt',
      'CANCELLED': 'cancel',
      'NO_SHOW': 'event_busy'
    };
    return statusIcons[status] || 'help_outline';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatTime(date: string): string {
    return new Date(date).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  getDaysUntil(date: string): number {
    const today = new Date();
    const appointmentDate = new Date(date);
    const diffTime = appointmentDate.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  isWithin24Hours(date: string): boolean {
    const now = new Date();
    const appointmentDate = new Date(date);
    const diffMs = appointmentDate.getTime() - now.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);
    return diffHours < 24;
  }

  canCancel(appointment: AppointmentResponseDto): boolean {
    return appointment.status === 'SCHEDULED' || appointment.status === 'CONFIRMED';
  }

  canReschedule(appointment: AppointmentResponseDto): boolean {
    return appointment.status === 'SCHEDULED' || appointment.status === 'CONFIRMED';
  }

  canSyncToCalendar(appointment: AppointmentWithSyncStatus): boolean {
    return this.calendarSyncStatus().syncEnabled &&
      !appointment.calendarEventId &&
      (appointment.status === 'SCHEDULED' || appointment.status === 'CONFIRMED');
  }

  syncAppointmentToCalendar(appointment: AppointmentWithSyncStatus): void {
    const appointments = this.appointments();
    const index = appointments.findIndex(a => a.appointmentId === appointment.appointmentId);
    if (index !== -1) {
      appointments[index] = { ...appointments[index], isSyncing: true };
      this.appointments.set([...appointments]);
    }

    this.googleCalendarService.syncAppointment(appointment.appointmentId).subscribe({
      next: (response) => {
        const updatedAppointments = this.appointments();
        const idx = updatedAppointments.findIndex(a => a.appointmentId === appointment.appointmentId);
        if (idx !== -1) {
          updatedAppointments[idx] = {
            ...updatedAppointments[idx],
            isSyncing: false,
            calendarEventId: response.calendarEventId
          };
          this.appointments.set([...updatedAppointments]);
        }

        this.snackBar.open('Appointment synced to Google Calendar!', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        const appointments = this.appointments();
        const index = appointments.findIndex(a => a.appointmentId === appointment.appointmentId);
        if (index !== -1) {
          appointments[index] = { ...appointments[index], isSyncing: false };
          this.appointments.set([...appointments]);
        }

        console.error('Failed to sync appointment', err);
        this.snackBar.open('Failed to sync appointment. Please try again.', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  openRescheduleDialog(appointment: AppointmentResponseDto): void {
    const dialogRef = this.dialog.open(RescheduleDialogComponent, {
      width: '600px',
      data: { appointment }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadAppointments();
      }
    });
  }

  openCancelDialog(appointment: AppointmentResponseDto): void {
    const dialogRef = this.dialog.open(CancelConfirmDialogComponent, {
      width: '400px',
      data: { appointment }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.cancelAppointment(appointment);
      }
    });
  }

  cancelAppointment(appointment: AppointmentResponseDto): void {
    this.apiService.put(`appointments/${appointment.appointmentId}/cancel`, {}).subscribe({
      next: () => {
        this.loadAppointments();
      },
      error: (err) => {
        console.error('Failed to cancel appointment', err);
      }
    });
  }

  bookNewAppointment(): void {
    this.router.navigate(['/services']);
  }

  viewServiceDetails(serviceId: number | undefined): void {
    if (serviceId) {
      this.router.navigate(['/services', serviceId]);
    }
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
  }

  getPaymentStatusIcon(status: string | undefined): string {
    if (status === 'COMPLETED') return 'check_circle';
    if (status === 'PENDING') return 'hourglass_empty';
    return 'money_off';
  }

  payForAppointment(appointment: AppointmentResponseDto): void {
    this.isProcessingPayment.set(appointment.appointmentId);
    this.paymentService.createOrder(appointment.appointmentId).subscribe({
      next: (res) => {
        if (res.success && res.data.redirectUri) window.location.href = res.data.redirectUri;
      },
      error: () => {
        this.snackBar.open('Payment failed', 'Close');
        this.isProcessingPayment.set(null);
      }
    });
  }
  payAtStudio(appointment: AppointmentResponseDto): void {
    if (!confirm('Confirm that you will pay cash at the studio?')) return;

    this.isProcessingPayment.set(appointment.appointmentId);

    this.paymentService.initiateCashPayment(appointment.appointmentId).subscribe({
      next: () => {
        this.snackBar.open('Payment method set to Cash. Please pay upon arrival.', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
        this.isProcessingPayment.set(null);
        this.loadAppointments();
      },
      error: () => {
        this.snackBar.open('Failed to set payment method', 'Close', { panelClass: ['error-snackbar'] });
        this.isProcessingPayment.set(null);
      }
    });
  }

}
