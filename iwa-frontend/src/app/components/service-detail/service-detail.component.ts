import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatListModule} from '@angular/material/list';
import {MatButtonModule} from '@angular/material/button';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatIconModule} from '@angular/material/icon';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {Observable} from 'rxjs';
import {switchMap} from 'rxjs/operators';

import {ApiService} from '../../services/api.service';
import {AuthService} from '../../services/auth.service';
import {PaymentService} from '../../services/payment.service';

import {ServiceResponseDto} from '../../interfaces/service.dto';
import {AvailabilitySlotResponseDto} from '../../interfaces/availability.dto';
import {BookAppointmentDto, AppointmentResponseDto} from '../../interfaces/appointment.dto';
import {BookingDialogComponent} from '../dialogs/booking-dialog/booking-dialog.component';
import {ReviewResponseDto} from '../../interfaces/review.dto';

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
    MatIconModule,
    MatDialogModule
  ],
  templateUrl: './service-detail.component.html',
  styleUrls: ['./service-detail.component.scss']
})
export class ServiceDetailComponent implements OnInit {
  service: ServiceResponseDto | null = null;
  availableSlots: AvailabilitySlotResponseDto[] = [];
  isLoggedIn$: Observable<boolean>;
  isLoggedIn = false;
  reviews: ReviewResponseDto[] = [];
  averageRating = 0;

  today = new Date().toISOString();
  farFuture = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString();

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService,
    private authService: AuthService,
    private paymentService: PaymentService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
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

      const queryString = `serviceId=${serviceId}&startTime=${this.today}&endTime=${this.farFuture}`;
      this.apiService.get<AvailabilitySlotResponseDto[]>(`availability?${queryString}`).subscribe(slots => {
        this.availableSlots = slots;
      });

      this.apiService.get<ReviewResponseDto[]>(`reviews/service/${serviceId}`).subscribe(data => {
        this.reviews = data;
        this.calculateAverageRating();
      });
    }
  }

  bookAppointment(slot: AvailabilitySlotResponseDto): void {
    if (!this.service) return;

    const dialogRef = this.dialog.open(BookingDialogComponent, {
      width: '500px',
      data: {
        service: this.service,
        slot: slot
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.bookingRequest) {
        this.processBooking(result.bookingRequest, result.paymentMethod);
      }
    });
  }

  private processBooking(bookingData: BookAppointmentDto, paymentMethod: string): void {
    this.apiService.post<AppointmentResponseDto>('appointments', bookingData)
      .pipe(
        switchMap((appointment) => {
          this.snackBar.open('Appointment booked successfully! Processing payment...', 'Close', {
            duration: 2000,
          });

          // Branch logic based on payment method
          if (paymentMethod === 'ONLINE') {
            return this.paymentService.createOrder(appointment.appointmentId);
          } else {
            return this.paymentService.initiateCashPayment(appointment.appointmentId);
          }
        })
      )
      .subscribe({
        next: (paymentResponse: any) => {
          if (paymentMethod === 'ONLINE' && paymentResponse.success && paymentResponse.data?.redirectUri) {
            window.location.href = paymentResponse.data.redirectUri;
          } else {
            this.router.navigate(['/dashboard']);
            this.snackBar.open('Booking confirmed. You have selected to pay at the studio.', 'OK', {
              duration: 5000,
            });
          }
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to process request. The slot may no longer be available.', 'Close', {
            duration: 3000,
          });
          this.ngOnInit();
        }
      });
  }

  private calculateAverageRating(): void {
    if (this.reviews.length === 0) {
      this.averageRating = 0;
      return;
    }
    const total = this.reviews.reduce((acc, review) => acc + review.rating, 0);
    this.averageRating = total / this.reviews.length;
  }

  showLoginPrompt(): void {
    this.snackBar.open('Please log in to book an appointment', 'Login', {
      duration: 5000,
    }).onAction().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
