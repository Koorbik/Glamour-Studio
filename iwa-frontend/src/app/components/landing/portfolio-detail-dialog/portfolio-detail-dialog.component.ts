import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PortfolioItemResponseDto } from '../../../interfaces/portfolio.dto';

@Component({
  selector: 'app-portfolio-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './portfolio-detail-dialog.component.html',
  styleUrls: ['./portfolio-detail-dialog.component.scss']
})
export class PortfolioDetailDialogComponent {
  activeImage: string;

  constructor(
    public dialogRef: MatDialogRef<PortfolioDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PortfolioItemResponseDto
  ) {
    // Default to first image or placeholder
    this.activeImage = (data.imageUrls && data.imageUrls.length > 0)
      ? data.imageUrls[0]
      : 'assets/placeholder.jpg';
  }

  setActiveImage(url: string) {
    this.activeImage = url;
  }

  close() {
    this.dialogRef.close();
  }
}
