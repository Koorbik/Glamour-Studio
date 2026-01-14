import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../services/api.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CreateReviewDto } from '../../../interfaces/review.dto';

@Component({
  selector: 'app-review-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    MatSnackBarModule
  ],
  templateUrl: './review-dialog.component.html',
  styleUrls: ['./review-dialog.component.scss']
})
export class ReviewDialogComponent {
  rating = 0;
  comment = '';
  hoverRating = 0;
  stars = [1, 2, 3, 4, 5];
  isSubmitting = false;

  constructor(
    public dialogRef: MatDialogRef<ReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { appointmentId: number },
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  setRating(star: number): void {
    this.rating = star;
  }

  submitReview(): void {
    if (this.rating === 0) {
      this.snackBar.open('Please select a star rating', 'Close', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;
    const reviewDto: CreateReviewDto = {
      appointmentId: this.data.appointmentId,
      rating: this.rating,
      comment: this.comment
    };

    this.apiService.post('reviews', reviewDto).subscribe({
      next: () => {
        this.snackBar.open('Thank you for your review!', 'Close', { duration: 3000 });
        this.dialogRef.close(true); // Return true to indicate success
      },
      error: (err) => {
        console.error(err);
        this.snackBar.open('Failed to submit review', 'Close', { duration: 3000 });
        this.isSubmitting = false;
      }
    });
  }
}
