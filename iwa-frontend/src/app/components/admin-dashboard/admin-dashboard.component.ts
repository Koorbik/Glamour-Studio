import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {MatCardModule} from '@angular/material/card';
import {MatTableModule} from '@angular/material/table';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatNativeDateModule} from '@angular/material/core';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatDialogModule, MatDialog} from '@angular/material/dialog';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatChipsModule} from '@angular/material/chips';
import {MatExpansionModule} from '@angular/material/expansion';
import {FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ApiService} from '../../services/api.service';
import {ConfirmDialogComponent} from '../dialogs/confirm-dialog/confirm-dialog.component';
import {AvailabilitySlotResponseDto} from '../../interfaces/availability.dto';
import {PaymentService} from '../../services/payment.service';
import {ReviewResponseDto} from '../../interfaces/review.dto';
import {PortfolioItemResponseDto} from '../../interfaces/portfolio.dto';
import { AdminPortfolioDialogComponent } from './admin-portfolio-dialog/admin-portfolio-dialog.component';

interface Appointment {
  appointmentId: number;
  userId: number;
  userName: string;
  serviceId: number;
  serviceName: string;
  serviceDescription: string;
  serviceDurationMin: number;
  servicePrice: number;
  status: string;
  location: string;
  scheduledAt: string;
  description: string;
  paymentId?: number;
  paymentStatus?: string;
  paymentMethod?: string;
}

interface Service {
  serviceId: number;
  name: string;
  description: string;
  minDuration: number;
  price: number;
}

interface AppointmentFilters {
  status: string;
  dateFrom: Date | null;
  dateTo: Date | null;
  serviceId: number | null;
  customerId: string;
  appointmentId: string;
  location: string;
  priceMin: number | null;
  priceMax: number | null;
}

interface SlotFilters {
  status: string;
  dateFrom: Date | null;
  dateTo: Date | null;
  serviceId: number | null;
  slotId: string;
  timeFrom: string;
  timeTo: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatDialogModule,
    MatMenuModule,
    MatTooltipModule,
    MatChipsModule,
    MatExpansionModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  appointments: Appointment[] = [];
  availabilitySlots: AvailabilitySlotResponseDto[] = [];
  services: Service[] = [];
  reviews: ReviewResponseDto[] = [];

  portfolioItems: PortfolioItemResponseDto[] = [];
  newPortfolioForm: FormGroup;
  selectedPortfolioFiles: File[] = [];
  selectedPortfolioPreviews: string[] = [];
  isUploadingPortfolio = false;

  displayedReviewColumns: string[] = ['id', 'author', 'rating', 'comment', 'date', 'actions'];

  appointmentDisplayedColumns: string[] = [
    'appointmentId',
    'customer',
    'service',
    'dateTime',
    'status',
    'totalPrice',
    'actions'
  ];

  slotDisplayedColumns: string[] = [
    'slotId',
    'service',
    'date',
    'time',
    'status',
    'actions'
  ];

  newSlotForm: FormGroup;
  selectedServiceId: number | null = null;

  // Enhanced filtering
  appointmentFilters: AppointmentFilters = {
    status: 'all',
    dateFrom: null,
    dateTo: null,
    serviceId: null,
    customerId: '',
    appointmentId: '',
    location: '',
    priceMin: null,
    priceMax: null
  };

  slotFilters: SlotFilters = {
    status: 'all',
    dateFrom: null,
    dateTo: null,
    serviceId: null,
    slotId: '',
    timeFrom: '',
    timeTo: ''
  };

  // Filter panel states
  appointmentFiltersExpanded = false;
  slotFiltersExpanded = false;

  // Status options
  appointmentStatusOptions = [
    {value: 'all', label: 'All'},
    {value: 'PENDING', label: 'Pending'},
    {value: 'CONFIRMED', label: 'Confirmed'},
    {value: 'CANCELLED', label: 'Cancelled'},
    {value: 'COMPLETED', label: 'Completed'}
  ];

  slotStatusOptions = [
    {value: 'all', label: 'All'},
    {value: 'available', label: 'Available'},
    {value: 'booked', label: 'Booked'},
    {value: 'expired', label: 'Expired'}
  ];

  constructor(
    private apiService: ApiService,
    private paymentService: PaymentService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.newSlotForm = this.fb.group({
      serviceId: ['', Validators.required],
      date: ['', Validators.required],
      startTime: ['', Validators.required],
      endTime: ['', Validators.required]
    });

    this.newPortfolioForm = this.fb.group({
      description: ['', [Validators.maxLength(500)]],
      category: ['', [Validators.maxLength(50)]]
    });
  }

  ngOnInit(): void {
    this.loadAppointments();
    this.loadAvailabilitySlots();
    this.loadServices();
    this.loadReviews();
    this.loadPortfolio();
  }

  loadAppointments(): void {
    this.apiService.get<Appointment[]>('appointments').subscribe({
      next: (appointments) => {
        this.appointments = appointments;
        console.log('Loaded appointments:', appointments);
      },
      error: (error) => {
        console.error('Error loading appointments:', error);
        this.snackBar.open('Failed to load appointments', 'Close', {duration: 3000});
      }
    });
  }

  loadAvailabilitySlots(): void {
    this.apiService.get<AvailabilitySlotResponseDto[]>('availability/all').subscribe({
      next: (slots) => {
        console.log('Raw slots response:', JSON.stringify(slots));

        // Sort slots: non-expired first (by date), then expired (by date)
        this.availabilitySlots = slots.sort((a, b) => {
          const aExpired = this.isSlotExpired(a);
          const bExpired = this.isSlotExpired(b);

          // If one is expired and the other isn't, non-expired comes first
          if (aExpired && !bExpired) return 1;
          if (!aExpired && bExpired) return -1;

          // Otherwise sort by date (newest first for non-expired, oldest first for expired)
          const dateA = new Date(a.startTime);
          const dateB = new Date(b.startTime);

          if (!aExpired && !bExpired) {
            // For non-expired: newest first
            return dateB.getTime() - dateA.getTime();
          } else {
            // For expired: oldest first (so recently expired are at top of expired section)
            return dateA.getTime() - dateB.getTime();
          }
        });

        console.log('Loaded availability slots:', slots);
        // Let's log the isBooked values to debug
        slots.forEach(slot => {
          console.log(`Slot ${slot.slotId}: isBooked = ${slot.isBooked}, type = ${typeof slot.isBooked}`);
        });
      },
      error: (error) => {
        console.error('Error loading availability slots:', error);
        this.snackBar.open('Failed to load availability slots', 'Close', {duration: 3000});
      }
    });
  }

  loadServices(): void {
    this.apiService.get<Service[]>('services').subscribe({
      next: (services) => {
        this.services = services;
        console.log('Loaded services:', services);
      },
      error: (error) => {
        console.error('Error loading services:', error);
        this.snackBar.open('Failed to load services', 'Close', {duration: 3000});
      }
    });
  }

  loadReviews() {
    this.apiService.get<ReviewResponseDto[]>('reviews').subscribe({
      next: (data) => this.reviews = data,
      error: (err) => console.error('Failed to load reviews', err)
    });
  }

  loadPortfolio(): void {
    this.apiService.get<PortfolioItemResponseDto[]>('portfolio').subscribe({
      next: (items) => this.portfolioItems = items,
      error: (err) => {
        console.error('Error loading portfolio:', err);
        this.snackBar.open('Failed to load portfolio items', 'Close', {duration: 3000});
      }
    });
  }

  getUniqueCategories(): string[] {
    const categories = new Set(this.portfolioItems.map(i => i.category));
    return Array.from(categories).sort();
  }

  openPortfolioDialog(item?: PortfolioItemResponseDto) {
    const dialogRef = this.dialog.open(AdminPortfolioDialogComponent, {
      width: '600px',
      maxWidth: '95vw',
      disableClose: true,
      data: {
        item: item, // If item is passed, it's Edit mode. If null, it's Add mode.
        existingCategories: this.getUniqueCategories()
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (item) {
          const index = this.portfolioItems.findIndex(p => p.id === result.id);
          if (index !== -1) {
            this.portfolioItems[index] = result;
          }
        } else {
          this.portfolioItems.unshift(result);
        }
      }
    });
  }

  deletePortfolioItem(item: PortfolioItemResponseDto): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Portfolio Item',
        message: 'Are you sure you want to delete this item? All associated images will be removed.'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.delete(`portfolio/${item.id}`).subscribe({
          next: () => {
            this.snackBar.open('Item deleted successfully', 'Close', {duration: 3000});
            this.portfolioItems = this.portfolioItems.filter(p => p.id !== item.id);
          },
          error: (err) => {
            console.error('Error deleting item:', err);
            this.snackBar.open('Failed to delete item', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  resetPortfolioForm(): void {
    this.newPortfolioForm.reset();
    this.selectedPortfolioFiles = [];
    this.selectedPortfolioPreviews = [];
  }

  deleteReview(review: ReviewResponseDto) {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Review',
        message: `Are you sure you want to delete the review by ${review.authorName}?`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.delete(`reviews/${review.reviewId}`).subscribe({
          next: () => {
            this.snackBar.open('Review deleted successfully', 'Close', {duration: 3000});
            this.loadReviews(); // Refresh list
          },
          error: (err) => {
            console.error(err);
            this.snackBar.open('Failed to delete review', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i < rating ? 1 : 0);
  }

  markAsPaid(paymentId: number | undefined): void {
    if (!paymentId) return;

    if (!confirm('Confirm receiving cash payment? This will generate an invoice.')) return;

    this.paymentService.confirmCashPayment(paymentId).subscribe({
      next: () => {
        this.snackBar.open('Payment confirmed! Invoice sent to user.', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
        this.loadAppointments(); // Refresh list
      },
      error: (err) => {
        console.error('Payment confirmation failed', err);
        this.snackBar.open('Failed to confirm payment.', 'Close', {
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  getFilteredAppointments(): Appointment[] {
    let filtered = [...this.appointments];

    // Status filter
    if (this.appointmentFilters.status !== 'all') {
      filtered = filtered.filter(app => app.status === this.appointmentFilters.status);
    }

    // Date range filter
    if (this.appointmentFilters.dateFrom) {
      const fromDate = new Date(this.appointmentFilters.dateFrom);
      fromDate.setHours(0, 0, 0, 0);
      filtered = filtered.filter(app => {
        const appDate = new Date(app.scheduledAt);
        return appDate >= fromDate;
      });
    }

    if (this.appointmentFilters.dateTo) {
      const toDate = new Date(this.appointmentFilters.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(app => {
        const appDate = new Date(app.scheduledAt);
        return appDate <= toDate;
      });
    }

    // Service filter
    if (this.appointmentFilters.serviceId) {
      filtered = filtered.filter(app => app.serviceId === this.appointmentFilters.serviceId);
    }

    // Customer ID/Name filter
    if (this.appointmentFilters.customerId.trim()) {
      const searchTerm = this.appointmentFilters.customerId.toLowerCase().trim();
      filtered = filtered.filter(app =>
        app.userName.toLowerCase().includes(searchTerm) ||
        app.userId.toString().includes(searchTerm)
      );
    }

    // Appointment ID filter
    if (this.appointmentFilters.appointmentId.trim()) {
      const searchId = this.appointmentFilters.appointmentId.trim();
      filtered = filtered.filter(app =>
        app.appointmentId.toString().includes(searchId)
      );
    }

    // Location filter
    if (this.appointmentFilters.location.trim()) {
      const searchLocation = this.appointmentFilters.location.toLowerCase().trim();
      filtered = filtered.filter(app =>
        app.location.toLowerCase().includes(searchLocation)
      );
    }

    // Price range filter
    if (this.appointmentFilters.priceMin !== null) {
      filtered = filtered.filter(app => app.servicePrice >= this.appointmentFilters.priceMin!);
    }

    if (this.appointmentFilters.priceMax !== null) {
      filtered = filtered.filter(app => app.servicePrice <= this.appointmentFilters.priceMax!);
    }

    return filtered;
  }

  getFilteredSlots(): AvailabilitySlotResponseDto[] {
    let filtered = [...this.availabilitySlots];

    // Status filter
    if (this.slotFilters.status !== 'all') {
      filtered = filtered.filter(slot => {
        const isExpired = this.isSlotExpired(slot);
        const isBooked = slot.isBooked;

        switch (this.slotFilters.status) {
          case 'available':
            return !isBooked && !isExpired;
          case 'booked':
            return isBooked && !isExpired;
          case 'expired':
            return isExpired;
          default:
            return true;
        }
      });
    }

    // Date range filter
    if (this.slotFilters.dateFrom) {
      const fromDate = new Date(this.slotFilters.dateFrom);
      fromDate.setHours(0, 0, 0, 0);
      filtered = filtered.filter(slot => {
        const slotDate = new Date(slot.startTime);
        return slotDate >= fromDate;
      });
    }

    if (this.slotFilters.dateTo) {
      const toDate = new Date(this.slotFilters.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(slot => {
        const slotDate = new Date(slot.startTime);
        return slotDate <= toDate;
      });
    }

    // Service filter
    if (this.slotFilters.serviceId) {
      filtered = filtered.filter(slot => slot.serviceId === this.slotFilters.serviceId);
    }

    // Slot ID filter
    if (this.slotFilters.slotId.trim()) {
      const searchId = this.slotFilters.slotId.trim();
      filtered = filtered.filter(slot =>
        slot.slotId.toString().includes(searchId)
      );
    }

    // Time range filter
    if (this.slotFilters.timeFrom) {
      filtered = filtered.filter(slot => {
        const slotTime = this.getTimeFromDateTime(slot.startTime);
        return slotTime >= this.slotFilters.timeFrom;
      });
    }

    if (this.slotFilters.timeTo) {
      filtered = filtered.filter(slot => {
        const slotTime = this.getTimeFromDateTime(slot.endTime);
        return slotTime <= this.slotFilters.timeTo;
      });
    }

    return filtered;
  }

  clearAppointmentFilters(): void {
    this.appointmentFilters = {
      status: 'all',
      dateFrom: null,
      dateTo: null,
      serviceId: null,
      customerId: '',
      appointmentId: '',
      location: '',
      priceMin: null,
      priceMax: null
    };
  }

  clearSlotFilters(): void {
    this.slotFilters = {
      status: 'all',
      dateFrom: null,
      dateTo: null,
      serviceId: null,
      slotId: '',
      timeFrom: '',
      timeTo: ''
    };
  }

  getActiveAppointmentFiltersCount(): number {
    let count = 0;
    if (this.appointmentFilters.status !== 'all') count++;
    if (this.appointmentFilters.dateFrom) count++;
    if (this.appointmentFilters.dateTo) count++;
    if (this.appointmentFilters.serviceId) count++;
    if (this.appointmentFilters.customerId.trim()) count++;
    if (this.appointmentFilters.appointmentId.trim()) count++;
    if (this.appointmentFilters.location.trim()) count++;
    if (this.appointmentFilters.priceMin !== null) count++;
    if (this.appointmentFilters.priceMax !== null) count++;
    return count;
  }

  getActiveSlotFiltersCount(): number {
    let count = 0;
    if (this.slotFilters.status !== 'all') count++;
    if (this.slotFilters.dateFrom) count++;
    if (this.slotFilters.dateTo) count++;
    if (this.slotFilters.serviceId) count++;
    if (this.slotFilters.slotId.trim()) count++;
    if (this.slotFilters.timeFrom) count++;
    if (this.slotFilters.timeTo) count++;
    return count;
  }

  cancelAppointment(appointment: Appointment): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Cancel Appointment',
        message: `Are you sure you want to cancel appointment #${appointment.appointmentId} for ${appointment.userName}?`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.put(`appointments/${appointment.appointmentId}/cancel`, {})
          .subscribe({
            next: () => {
              this.snackBar.open('Appointment cancelled successfully', 'Close', {duration: 3000});
              this.loadAppointments();
            },
            error: (error) => {
              console.error('Error cancelling appointment:', error);
              this.snackBar.open('Failed to cancel appointment', 'Close', {duration: 3000});
            }
          });
      }
    });
  }

  updateAppointmentStatus(appointment: Appointment, newStatus: string): void {
    this.apiService.put(`appointments/${appointment.appointmentId}/status`, {status: newStatus})
      .subscribe({
        next: () => {
          this.snackBar.open(`Appointment status updated to ${newStatus}`, 'Close', {duration: 3000});
          this.loadAppointments();
        },
        error: (error) => {
          console.error('Error updating appointment status:', error);
          this.snackBar.open('Failed to update appointment status', 'Close', {duration: 3000});
        }
      });
  }

  createAvailabilitySlot(): void {
    if (this.newSlotForm.valid) {
      const formValue = this.newSlotForm.value;
      const date = this.formatDate(formValue.date);
      const slotData = {
        serviceId: formValue.serviceId,
        startTime: `${date}T${formValue.startTime}:00`,
        endTime: `${date}T${formValue.endTime}:00`
      };

      this.apiService.post('availability', slotData).subscribe({
        next: () => {
          this.snackBar.open('Availability slot created successfully', 'Close', {duration: 3000});
          this.newSlotForm.reset();
          this.loadAvailabilitySlots();
        },
        error: (error) => {
          console.error('Error creating availability slot:', error);
          this.snackBar.open('Failed to create availability slot', 'Close', {duration: 3000});
        }
      });
    }
  }

  deleteAvailabilitySlot(slot: AvailabilitySlotResponseDto): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Availability Slot',
        message: `Are you sure you want to delete this availability slot?`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.delete(`availability/${slot.slotId}`).subscribe({
          next: () => {
            this.snackBar.open('Availability slot deleted successfully', 'Close', {duration: 3000});
            this.loadAvailabilitySlots();
          },
          error: (error) => {
            console.error('Error deleting availability slot:', error);
            this.snackBar.open('Failed to delete availability slot', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  toggleSlotAvailability(slot: AvailabilitySlotResponseDto): void {
    if (this.isSlotExpired(slot)) {
      this.snackBar.open('Cannot modify expired slots', 'Close', {duration: 3000});
      return;
    }

    const action = slot.isBooked ? 'release' : 'book';
    const confirmMessage = slot.isBooked ?
      'Are you sure you want to release this booked slot? This will make it available for booking again.' :
      'Are you sure you want to manually mark this slot as booked?';

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: slot.isBooked ? 'Release Booking' : 'Mark as Booked',
        message: confirmMessage
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const endpoint = `availability/${slot.slotId}/${action}`;

        this.apiService.put(endpoint, {}).subscribe({
          next: () => {
            const message = slot.isBooked ?
              'Booking released successfully - slot is now available' :
              'Slot marked as booked';
            this.snackBar.open(message, 'Close', {duration: 3000});
            this.loadAvailabilitySlots();
            this.loadAppointments(); // Reload appointments in case this affects them
          },
          error: (error) => {
            console.error('Error updating slot availability:', error);
            this.snackBar.open('Failed to update slot availability', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'warn';
      case 'CONFIRMED':
        return 'primary';
      case 'CANCELLED':
        return 'accent';
      case 'COMPLETED':
        return 'primary';
      default:
        return '';
    }
  }

  // Helper methods to extract date and time from ISO string
  getDateFromDateTime(dateTime: string): string {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    return date.toLocaleDateString();
  }

  getTimeFromDateTime(dateTime: string): string {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    return date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit', hour12: false});
  }

  getTimeRangeFromSlot(slot: AvailabilitySlotResponseDto): string {
    const startTime = this.getTimeFromDateTime(slot.startTime);
    const endTime = this.getTimeFromDateTime(slot.endTime);
    return `${startTime} - ${endTime}`;
  }

  // Check if a slot is expired (past the current date/time)
  isSlotExpired(slot: AvailabilitySlotResponseDto): boolean {
    if (!slot || !slot.endTime) return false;
    return new Date(slot.endTime) < new Date();
  }

  // Get slot status display
  getSlotStatus(slot: AvailabilitySlotResponseDto): string {
    if (this.isSlotExpired(slot)) {
      return 'Expired';
    }
    return slot.isBooked ? 'Booked' : 'Available';
  }

  // Get CSS class for slot status
  getSlotStatusClass(slot: AvailabilitySlotResponseDto): string {
    if (this.isSlotExpired(slot)) {
      return 'expired';
    }
    return slot.isBooked ? 'booked' : 'available';
  }

  // Check if slot actions should be disabled
  shouldDisableSlotActions(slot: AvailabilitySlotResponseDto): boolean {
    return this.isSlotExpired(slot);
  }
}
