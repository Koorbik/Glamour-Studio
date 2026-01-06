import {Component, Inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatDialogModule, MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {AvailabilitySlotResponseDto} from '../../../interfaces/availability.dto';
import {ServiceResponseDto} from '../../../interfaces/service.dto';
import {BookAppointmentDto} from '../../../interfaces/appointment.dto';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatRadioModule} from '@angular/material/radio';

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
    MatIconModule,
    MatCheckboxModule,
    MatRadioModule
  ],
  templateUrl: './booking-dialog.component.html',
  styleUrls: ['./booking-dialog.component.scss']
})
export class BookingDialogComponent {
  bookingForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<BookingDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.bookingForm = this.fb.group({
      location: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', [Validators.maxLength(500)]],
      paymentMethod: ['ONLINE', [Validators.required]],
      acceptsTerms: [false, [Validators.requiredTrue]]
    });
  }

  onSubmit(): void {
    if (this.bookingForm.valid) {
      const formValue = this.bookingForm.value;

      const bookingRequest: BookAppointmentDto = {
        slotId: this.data.slot.slotId,
        serviceId: this.data.service.serviceId,
        location: formValue.location,
        description: formValue.description,
        acceptsTerms: formValue.acceptsTerms
      };

      this.dialogRef.close({
        bookingRequest,
        paymentMethod: formValue.paymentMethod
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
