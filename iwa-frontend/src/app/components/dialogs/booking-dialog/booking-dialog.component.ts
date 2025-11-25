import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AvailabilitySlotResponseDto } from '../../../interfaces/availability.dto';
import { ServiceResponseDto } from '../../../interfaces/service.dto';

export interface BookingDialogData {
  service: ServiceResponseDto;
  slot: AvailabilitySlotResponseDto;
}

@Component({
  selector: 'app-booking-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule
  ],
  templateUrl: './booking-dialog.component.html',
  styleUrls: ['./booking-dialog.component.scss']
})
export class BookingDialogComponent {
  bookingForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<BookingDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BookingDialogData
  ) {
    this.bookingForm = this.fb.group({
      location: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.bookingForm.valid) {
      this.dialogRef.close(this.bookingForm.value);
    }
  }
}
