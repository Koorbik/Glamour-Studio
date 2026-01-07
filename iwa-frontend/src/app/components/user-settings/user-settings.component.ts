import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { GoogleCalendarService, GoogleCalendarSyncStatus } from '../../services/google-calendar.service';
import { UserProfileDto, UserProfileUpdateDto } from '../../interfaces/user.dto';

@Component({
  selector: 'app-user-settings',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatTooltipModule,
    ReactiveFormsModule
  ],
  templateUrl: './user-settings.component.html',
  styleUrls: ['./user-settings.component.scss']
})
export class UserSettingsComponent implements OnInit {
  profileForm: FormGroup;
  isUpdatingProfile = signal(false);
  isLoading = signal(true);
  userProfile = signal<UserProfileDto | null>(null);

  // Google Calendar sync related
  calendarSyncStatus = signal<GoogleCalendarSyncStatus>({
    isSynced: false,
    syncEnabled: false
  });
  isSyncingCalendar = signal(false);
  isLoadingCalendarStatus = signal(true);

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private googleCalendarService: GoogleCalendarService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      surname: ['', [Validators.required, Validators.minLength(2)]],
      email: [{value: '', disabled: true}, [Validators.required, Validators.email]],
      phoneNum: ['', [Validators.required, Validators.pattern(/^\+?[1-9]\d{1,14}$/)]],
      smsNotificationsEnabled: [false] // Added this field
    });
  }

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadCalendarSyncStatus();
  }

  loadUserProfile(): void {
    this.isLoading.set(true);
    this.apiService.get<UserProfileDto>('users/profile').subscribe({
      next: (data) => {
        this.userProfile.set(data);
        this.profileForm.patchValue({
          name: data.name || '',
          surname: data.surname || '',
          email: data.email || '',
          phoneNum: data.phoneNum || '',
          smsNotificationsEnabled: data.smsNotificationsEnabled || false // Patch value
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch user profile', err);
        this.isLoading.set(false);

        if (err.status === 401) {
          this.snackBar.open('Session expired. Please log in again.', 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
          this.authService.logout();
        } else {
          this.snackBar.open('Failed to load profile data', 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  loadCalendarSyncStatus(): void {
    this.isLoadingCalendarStatus.set(true);
    this.googleCalendarService.getSyncStatus().subscribe({
      next: (status) => {
        this.calendarSyncStatus.set(status);
        this.isLoadingCalendarStatus.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch calendar sync status', err);
        this.isLoadingCalendarStatus.set(false);
      }
    });
  }

  updateProfile(): void {
    if (this.profileForm.valid && this.profileForm.dirty) {
      this.isUpdatingProfile.set(true);
      const updateData: UserProfileUpdateDto = {
        name: this.profileForm.get('name')?.value,
        surname: this.profileForm.get('surname')?.value,
        phoneNum: this.profileForm.get('phoneNum')?.value,
        smsNotificationsEnabled: this.profileForm.get('smsNotificationsEnabled')?.value // Add this
      };

      this.apiService.put<UserProfileDto>('users/profile', updateData).subscribe({
        next: (data) => {
          this.userProfile.set(data);
          this.snackBar.open('Profile updated successfully!', 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          });
          this.profileForm.markAsPristine();
          this.isUpdatingProfile.set(false);
        },
        error: (err) => {
          this.snackBar.open('Failed to update profile. Please try again.', 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
          this.isUpdatingProfile.set(false);
        }
      });
    }
  }

  resetForm(): void {
    const profile = this.userProfile();
    if (profile) {
      this.profileForm.patchValue({
        name: profile.name || '',
        surname: profile.surname || '',
        email: profile.email || '',
        phoneNum: profile.phoneNum || '',
        smsNotificationsEnabled: profile.smsNotificationsEnabled || false // Reset value
      });
      this.profileForm.markAsPristine();
    }
  }

  // Google Calendar sync methods
  toggleCalendarSync(): void {
    const currentStatus = this.calendarSyncStatus();

    if (currentStatus.syncEnabled) {
      this.disableCalendarSync();
    } else {
      this.enableCalendarSync();
    }
  }

  enableCalendarSync(): void {
    this.isSyncingCalendar.set(true);

    this.googleCalendarService.enableSync().subscribe({
      next: () => {
        this.snackBar.open('Google Calendar sync enabled successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
        this.loadCalendarSyncStatus();
        this.isSyncingCalendar.set(false);
        this.syncAllAppointments();
      },
      error: (err) => {
        console.error('Failed to enable calendar sync', err);
        this.snackBar.open('Failed to enable Google Calendar sync. Please try again.', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
        this.isSyncingCalendar.set(false);
      }
    });
  }

  disableCalendarSync(): void {
    this.isSyncingCalendar.set(true);

    this.googleCalendarService.disableSync().subscribe({
      next: () => {
        this.snackBar.open('Google Calendar sync disabled', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['info-snackbar']
        });
        this.loadCalendarSyncStatus();
        this.isSyncingCalendar.set(false);
      },
      error: (err) => {
        console.error('Failed to disable calendar sync', err);
        this.snackBar.open('Failed to disable calendar sync. Please try again.', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
        this.isSyncingCalendar.set(false);
      }
    });
  }

  syncAllAppointments(): void {
    this.googleCalendarService.syncAllAppointments().subscribe({
      next: () => {
        this.snackBar.open('All appointments synced to Google Calendar!', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error('Failed to sync appointments', err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
