export interface ReviewResponseDto {
  reviewId: number;
  authorName: string;
  rating: number;
  comment: string;
  createdAt: string;
  attachmentUrl?: string;
}

export interface CreateReviewDto {
  appointmentId: number;
  rating: number;
  comment: string;
}
