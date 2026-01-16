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

  selectedFile: File | null = null;
  selectedImagePreview: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<ReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { appointmentId: number },
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  setRating(star: number) { this.rating = star; }

  getRatingText(rating: number): string {
    const texts = ['Poor', 'Fair', 'Good', 'Very Good', 'Excellent'];
    return texts[rating - 1];
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        this.snackBar.open('Please select an image file', 'Close', { duration: 3000 });
        return;
      }
      this.selectedFile = file;

      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.selectedImagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage() {
    this.selectedFile = null;
    this.selectedImagePreview = null;
  }

  submitReview() {
    if (this.rating === 0) return;

    this.isSubmitting = true;

    // Use FormData to send file + JSON
    const formData = new FormData();

    // Create the JSON part
    const reviewData = {
      appointmentId: this.data.appointmentId,
      rating: this.rating,
      comment: this.comment
    };

    // Append JSON as a Blob to specify content type
    formData.append('review', new Blob([JSON.stringify(reviewData)], {
      type: 'application/json'
    }));

    if (this.selectedFile) {
      formData.append('file', this.selectedFile);
    }

    // Call API (Ensure your API service can handle FormData)
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
