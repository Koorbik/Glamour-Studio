import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {ApiResponse, PaymentInitiationDto} from '../interfaces/payment.dto';
import {AuthService} from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/payments`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
  }

  createOrder(appointmentId: number): Observable<ApiResponse<PaymentInitiationDto>> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    return this.http.post<ApiResponse<PaymentInitiationDto>>(
      `${this.apiUrl}/create-order/${appointmentId}`,
      {},
      {headers}
    );
  }

  initiateCashPayment(appointmentId: number): Observable<ApiResponse<void>> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/pay-at-studio/${appointmentId}`,
      {},
      {headers}
    );
  }

  confirmCashPayment(paymentId: number): Observable<ApiResponse<void>> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/${paymentId}/confirm-cash`,
      {},
      {headers}
    );
  }
}
