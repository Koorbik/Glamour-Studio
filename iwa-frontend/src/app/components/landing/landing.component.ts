import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ApiService } from '../../services/api.service';
import { PortfolioItemResponseDto } from '../../interfaces/portfolio.dto';
import { PortfolioDetailDialogComponent } from './portfolio-detail-dialog/portfolio-detail-dialog.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDialogModule
  ],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit {
  @ViewChild('carousel') carousel!: ElementRef<HTMLElement>;

  portfolioItems: PortfolioItemResponseDto[] = [];
  filteredItems: PortfolioItemResponseDto[] = [];
  categories: string[] = ['All'];
  selectedCategory: string = 'All';
  isLoading = true;

  constructor(
    private router: Router,
    private apiService: ApiService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadPortfolio();
  }

  loadPortfolio() {
    this.isLoading = true;
    this.apiService.get<PortfolioItemResponseDto[]>('portfolio').subscribe({
      next: (data) => {
        this.portfolioItems = data.sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.filteredItems = this.portfolioItems;
        this.extractCategories();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load portfolio', err);
        this.isLoading = false;
      }
    });
  }

  extractCategories() {
    const uniqueCategories = new Set(this.portfolioItems.map(item => item.category));
    this.categories = ['All', ...Array.from(uniqueCategories).sort()];
  }

  filterCategory(category: string) {
    this.selectedCategory = category;
    if (category === 'All') {
      this.filteredItems = this.portfolioItems;
    } else {
      this.filteredItems = this.portfolioItems.filter(item => item.category === category);
    }
  }

  openPortfolioDetail(item: PortfolioItemResponseDto) {
    this.dialog.open(PortfolioDetailDialogComponent, {
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'portfolio-detail-modal',
      data: item,
      autoFocus: false
    });
  }

  scrollCarousel(direction: number) {
    if (!this.carousel) return;

    const container = this.carousel.nativeElement;
    const scrollAmount = 350; // Approx width of card + gap

    const targetScroll = container.scrollLeft + (direction * scrollAmount);

    container.scrollTo({
      left: targetScroll,
      behavior: 'smooth'
    });
  }

  navigateToServices() {
    this.router.navigate(['/services']);
  }

  navigateToBooking() {
    this.router.navigate(['/services']);
  }
}
