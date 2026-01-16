import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { ReviewResponseDto } from '../../interfaces/review.dto';
import { ServiceResponseDto } from '../../interfaces/service.dto';

@Component({
  selector: 'app-service-reviews',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './service-reviews.component.html',
  styleUrls: ['./service-reviews.component.scss']
})
export class ServiceReviewsComponent implements OnInit {
  reviews: ReviewResponseDto[] = [];
  service: ServiceResponseDto | null = null;
  averageRating = 0;
  isLoading = true;

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    const serviceId = this.route.snapshot.paramMap.get('id');
    if (serviceId) {
      this.loadData(serviceId);
    }
  }

  private loadData(serviceId: string): void {
    this.isLoading = true;

    this.apiService.get<ServiceResponseDto>(`services/${serviceId}`).subscribe({
      next: (data) => this.service = data,
      error: (err) => console.error('Failed to load service', err)
    });

    this.apiService.get<ReviewResponseDto[]>(`reviews/service/${serviceId}`).subscribe({
      next: (data) => {
        this.reviews = data;
        this.calculateAverageRating();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load reviews', err);
        this.isLoading = false;
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

  openImage(url: string): void {
    window.open(url, '_blank');
  }
}
