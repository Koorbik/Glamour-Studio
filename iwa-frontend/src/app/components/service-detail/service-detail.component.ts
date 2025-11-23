import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { PaymentService } from '../../services/payment.service';

import { ServiceResponseDto } from '../../interfaces/service.dto';
import { AvailabilitySlotResponseDto } from '../../interfaces/availability.dto';
import { BookAppointmentDto, AppointmentResponseDto } from '../../interfaces/appointment.dto';

@Component({
  selector: 'app-service-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatListModule,
    MatButtonModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './service-detail.component.html',
  styleUrls: ['./service-detail.component.scss']
})
export class ServiceDetailComponent implements OnInit {
  service: ServiceResponseDto | null = null;
  availableSlots: AvailabilitySlotResponseDto[] = [];
  isLoggedIn$: Observable<boolean>;
  isLoggedIn = false;

  today = new Date().toISOString();
  farFuture = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString();

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService,
    private authService: AuthService,
    private paymentService: PaymentService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.isLoggedIn$ = this.authService.isLoggedIn();
    this.isLoggedIn$.subscribe(status => this.isLoggedIn = status);
  }

  ngOnInit(): void {
    const serviceId = this.route.snapshot.paramMap.get('id');
    if (serviceId) {
      this.apiService.get<ServiceResponseDto>(`services/${serviceId}`).subscribe(data => {
        this.service = data;
      });

      // Construct a query string for getAvailableSlots
      const queryString = `serviceId=${serviceId}&startTime=${this.today}&endTime=${this.farFuture}`;
      this.apiService.get<AvailabilitySlotResponseDto[]>(`availability?${queryString}`).subscribe(slots => {
        this.availableSlots = slots;
      });
    }
  }

  bookAppointment(slot: AvailabilitySlotResponseDto): void {
    if (!this.service) return;

    // A simple prompt for location, in a future app this would be a more robust form/dialog
    const location = prompt("Please enter the location for the appointment:", "My Home Address");
    if (!location) {
      this.snackBar.open('Location is required to book an appointment.', 'Close', { duration: 3000 });
      return;
    }

    const bookingData: BookAppointmentDto = {
      slotId: slot.slotId,
      serviceId: this.service.serviceId,
      location: location,
      description: 'Booked via frontend'
    };

    this.apiService.post<AppointmentResponseDto>('appointments', bookingData)
      .pipe(
        switchMap((appointment) => {
          this.snackBar.open('Appointment booked successfully! Redirecting to payment...', 'Close', {
            duration: 2000,
          });

          return this.paymentService.createOrder(appointment.appointmentId);
        })
      )
      .subscribe({
        next: (paymentResponse) => {
          if (paymentResponse.success && paymentResponse.data.redirectUri) {
            window.location.href = paymentResponse.data.redirectUri;
          } else {
            this.router.navigate(['/dashboard']);
                                    this.snackBar.open('Booking successful. Unable to process payment at this time. Please complete payment from your dashboard.', 'OK', {
              duration: 5000,
            });
          }
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to process request. The slot may no longer be available.', 'Close', {
            duration: 3000,
          });

          // Refresh slots to show current availability
          this.ngOnInit();
        }
      });
  }

  showLoginPrompt(): void {
    this.snackBar.open('Please log in to book an appointment', 'Login', {
      duration: 5000,
    }).onAction().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
