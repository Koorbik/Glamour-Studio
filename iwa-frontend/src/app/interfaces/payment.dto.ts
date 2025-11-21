export interface PaymentInitiationDto {
  redirectUri: string;
  orderId: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
