import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../../../services/api.service';

@Component({
  selector: 'app-review-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule
  ],
  templateUrl: './review-dialog.component.html',
  styleUrls: ['./review-dialog.component.scss']
})
export class ReviewDialogComponent {
  rating = 0;
  hoverRating = 0;
  comment = '';
  isSubmitting = false;

  isEditMode = false;

  selectedFiles: File[] = [];
  selectedImagePreviews: string[] = [];

  constructor(
    public dialogRef: MatDialogRef<ReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { appointmentId: number, review?: any },
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {
    if (data.review) {
      this.isEditMode = true;
      this.rating = data.review.rating;
      this.comment = data.review.comment;
    }
  }

  setRating(star: number) {
    this.rating = star;
  }

  getRatingText(rating: number): string {
    const texts = ['Poor', 'Fair', 'Good', 'Very Good', 'Excellent'];
    return texts[rating - 1];
  }

  onFileSelected(event: any) {
    const files: FileList = event.target.files;

    if (files && files.length > 0) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];

        if (!file.type.startsWith('image/')) {
          this.snackBar.open(`File "${file.name}" is not an image`, 'Close', { duration: 3000 });
          continue;
        }

        this.selectedFiles.push(file);

        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.selectedImagePreviews.push(e.target.result);
        };
        reader.readAsDataURL(file);
      }
    }
  }

  removeImage(index: number) {
    this.selectedFiles.splice(index, 1);
    this.selectedImagePreviews.splice(index, 1);
  }

  submitReview() {
    if (this.rating === 0) return;

    this.isSubmitting = true;

    const formData = new FormData();

    const reviewData = {
      appointmentId: this.data.appointmentId,
      rating: this.rating,
      comment: this.comment
    };

    formData.append('review', new Blob([JSON.stringify(reviewData)], {
      type: 'application/json'
    }));

    this.selectedFiles.forEach(file => {
      formData.append('files', file);
    });

    if (this.isEditMode) {
      this.apiService.put(`reviews/${this.data.review.reviewId}`, formData).subscribe({
        next: () => {
          this.snackBar.open('Review updated successfully!', 'Close', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to update review', 'Close', { duration: 3000 });
          this.isSubmitting = false;
        }
      });
    } else {
      this.apiService.post('reviews', formData).subscribe({
        next: () => {
          this.snackBar.open('Review submitted successfully!', 'Close', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to submit review', 'Close', { duration: 3000 });
          this.isSubmitting = false;
        }
      });
    }
  }
}
