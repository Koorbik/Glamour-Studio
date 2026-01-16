import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { VerifyEmailComponent } from './components/verify-email/verify-email.component';
import { ServicesListComponent } from './components/services-list/services-list.component';
import { LandingComponent } from './components/landing/landing.component';
import { authGuard } from './guards/auth-guard';
import { adminGuard } from './guards/admin-guard';
import { ServiceDetailComponent } from './components/service-detail/service-detail.component';
import { UserDashboardComponent } from './components/user-dashboard/user-dashboard.component';
import { UserSettingsComponent } from './components/user-settings/user-settings.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { OAuthCallbackComponent } from './components/oauth-callback/oauth-callback.component';
import { ServiceReviewsComponent } from './components/service-reviews/service-reviews.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'services', component: ServicesListComponent },
  { path: 'services/:id', component: ServiceDetailComponent },
  { path: 'services/:id/reviews', component: ServiceReviewsComponent },
  {
    path: 'dashboard',
    component: UserDashboardComponent,
    canActivate: [authGuard],
  },
  {
    path: 'settings',
    component: UserSettingsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'oauth/callback',
    component: OAuthCallbackComponent
  }
];
