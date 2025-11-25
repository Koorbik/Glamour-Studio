# Glamour Studio - Makeup Artist Booking System 💄✨

A full-stack web application for booking makeup artist appointments, featuring user authentication, appointment management, Google Calendar integration, and admin dashboard functionality.
## 📋 Project Overview

**Glamour Studio** is a modern, responsive booking system designed for makeup artists and their clients. The application provides a seamless experience for customers to browse services, book appointments, and manage their bookings, while giving administrators powerful tools to manage their business.

## 🚀 Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.4.5**
- **Spring Security** with JWT authentication
- **Spring Data JPA** for data persistence
- **H2 Database** (in-memory) for development, **PostgreSQL** for production
- **OAuth2** integration (Google Authentication)
- **Google Calendar API** for appointment synchronization
- **Spring Mail** for email notifications
- **Maven** for dependency management

### Frontend
- **Angular 20** with TypeScript
- **Angular Material** for UI components
- **RxJS** for reactive programming
- **JWT handling** for authentication
- **Responsive design** with SCSS

### DevOps & Tools
- **Docker & Docker Compose** for containerization
- **Git** version control

## 🎯 Features Implemented

### 🔐 Authentication & User Management
- [x] User registration with email verification
- [x] Traditional login/logout functionality
- [x] Google OAuth integration
- [x] Role-based access control (USER/ADMIN)
- [x] JWT token management
- [x] Password encryption with BCrypt
- [x] User profile management

### 💳 Payment Integration
- [x] PayU payment gateway integration
- [x] Secure payment order creation
- [x] Webhook notifications with MD5 signature verification
- [x] Payment status tracking (PENDING, COMPLETED, CANCELED)
- [x] Automatic payment confirmation
- [x] Integration with appointment system

### 📅 Appointment System
- [x] Service browsing and selection
- [x] Available time slot viewing
- [x] Appointment booking
- [x] Appointment status tracking (PENDING, CONFIRMED, COMPLETED, CANCELLED)
- [x] Appointment cancellation and rescheduling
- [x] Admin appointment management

### 🗓️ Google Calendar Integration
- [x] OAuth2 flow for Google Calendar access
- [x] Automatic calendar event creation
- [x] Calendar synchronization
- [x] Token management and refresh

### 👨‍💼 Admin Dashboard
- [x] Appointment overview and management
- [x] User management
- [x] Service management
- [x] Availability slot configuration
- [x] Admin-only access controls

### 🎨 User Interface
- [x] Professional landing page with portfolio showcase
- [x] Responsive design for all devices
- [x] Material Design components
- [x] Interactive forms with validation
- [x] Modern, accessible UI/UX
- [x] Error handling and user feedback

### 🔧 Technical Features
- [x] RESTful API architecture
- [x] Database migrations and seeding
- [x] Environment-based configuration
- [x] CORS configuration for frontend-backend communication
- [x] Request/response validation
- [x] Comprehensive error handling

## 📁 Project Structure

```
IWA/
├── iwa_backend/                 # Spring Boot backend
│   ├── src/main/java/com/hszadkowski/iwa_backend/
│   │   ├── config/             # Security, CORS, JWT configuration
│   │   ├── controllers/        # REST API endpoints
│   │   ├── dto/                # Data Transfer Objects
│   │   ├── exceptions/         # Custom exception handling
│   │   ├── models/            # JPA entities
│   │   ├── repos/             # Repository interfaces
│   │   └── services/          # Business logic services
│   ├── src/main/resources/
│   │   ├── application*.properties  # Environment configs
│   │   └── data.sql           # Database seeding
│   └── pom.xml                # Maven dependencies
├── iwa-frontend/               # Angular frontend
│   ├── src/app/
│   │   ├── components/        # Angular components
│   │   ├── guards/           # Route guards
│   │   ├── interfaces/       # TypeScript interfaces
│   │   ├── services/         # Angular services
│   │   └── utils/           # Utility functions
│   └── package.json          # npm dependencies
├── db/                        # Database Docker configuration
├── docker-compose.yml         # Multi-container setup
└── user-stories.md           # Project requirements
```

## 🚀 Getting Started

### Prerequisites
- **Java 21**
- **Node.js 18+** 
- **Maven** (or use the included wrapper)

### 🚀 Quick Start

1. **Clone the repository**
   ```powershell
   git clone <repository-url>
   cd IWA
   ```

2. **Configure environment variables (optional)**
   ```powershell
   # Create backend .env file for Google OAuth (optional)
   # Copy .env.example if it exists, or create new .env file
   # The app works without OAuth for basic functionality
   ```

3. **Start the backend**
   ```powershell
   cd iwa_backend
   ./mvnw spring-boot:run
   ```
   > 💡 The backend uses H2 in-memory database by default - no external database setup needed!

4. **Start the frontend** (in a new terminal)
   ```powershell
   cd iwa-frontend
   npm install  # First time only
   ng serve
   ```

5. **Access the application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080
   - H2 Database Console: http://localhost:8080/h2-console (for development)

### 🐳 Docker Deployment

To run the application with Docker (using PostgreSQL database):

1. **Create environment file**
   ```powershell
   # Copy the example environment file
   cp .env.docker.example .env.docker
   ```

2. **Configure environment variables**
   Edit `.env.docker` and fill in the required values:
   - Database credentials
   - JWT secret key
   - Email configuration
   - Google OAuth credentials (optional)
   - PayU payment credentials (if using payment features)

3. **Start the application**
   ```powershell
   docker-compose up --build
   ```

4. **Access the application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080
   - PostgreSQL Database: localhost:5432

5. **Stop the application**
   ```powershell
   docker-compose down
   ```

> 💡 **Note:** The Docker setup uses PostgreSQL database and runs all services (frontend, backend, and database) in containers.

### 👤 Default Users

The application comes with pre-seeded users for testing:

| Email | Password | Role |
|-------|----------|------|
| `alice@acme.com` | `admin123` | ADMIN |
| `bob@acme.com` | `user123` | USER |

## 🔧 Configuration

### Backend Configuration

#### Local Development (H2 Database)
The backend uses profile `local` by default (configured in `application.properties`).

Create `iwa_backend/.env` file with:
```bash
# JWT
JWT_SECRET_KEY=your_secret_key_here
JWT_EXPIRATION=86400000

# Email
SUPPORT_EMAIL=your_email@gmail.com
APP_PASSWORD=your_gmail_app_password

# Google Calendar OAuth (optional)
GOOGLE_CALENDAR_CLIENT_ID=your_client_id
GOOGLE_CALENDAR_SECRET=your_client_secret
GOOGLE_AUTH_CLIENT_ID=your_auth_client_id
GOOGLE_AUTH_FRONTEND_CLIENT_ID=your_frontend_client_id

# Facebook OAuth (optional)
FACEBOOK_APP_ID=your_app_id
FACEBOOK_APP_SECRET=your_app_secret

# PayU Configuration (optional)
PAYU_BASE_URL=https://secure.snd.payu.com
PAYU_OAUTH_CLIENT_ID=your_payu_client_id
PAYU_OAUTH_CLIENT_SECRET=your_payu_client_secret
PAYU_POS_ID=your_payu_pos_id
PAYU_SECOND_KEY_MD5=your_payu_second_key
PAYU_NOTIFY_URL=http://localhost:8080/api/payments/notify
```

#### Docker Deployment (PostgreSQL)
Docker uses profile `dev` with PostgreSQL database.

1. Copy the example: `cp .env.docker.example .env.docker`
2. Fill in all required credentials (database, JWT, email, and optionally PayU for payment features)
3. Start: `docker-compose up --build`

### Frontend Configuration
Environment settings in `iwa-frontend/src/environments/`:
- API endpoints
- Google OAuth client ID
- Production/development flags

## 🎯 Future Enhancements

### 🔔 Enhanced Notifications
- [ ] SMS notifications
- [ ] WhatsApp integration
- [ ] Email templates customization
- [ ] Notification preferences

### 📊 Analytics & Reporting
- [ ] Business analytics dashboard
- [ ] Revenue tracking
- [ ] Customer insights
- [ ] Appointment statistics
- [ ] Performance metrics

### 🎨 Enhanced Features
- [ ] Photo portfolio management system
- [ ] Before/after photo uploads
- [ ] Customer review system
- [ ] Multi-language support
- [ ] Dark/light theme toggle

### 🔐 Advanced Security
- [ ] Two-factor authentication (2FA)
- [ ] Rate limiting
- [ ] Advanced user permissions
- [ ] Audit logging

### 🤖 AI/ML Features
- [ ] Smart appointment scheduling
- [ ] Personalized service recommendations
- [ ] Automated customer support chatbot
- [ ] Demand forecasting

## 🤝 Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 🐛 Issues & Bug Reports

Found a bug or have a feature request? We'd love to hear from you!

### 🔍 Before Reporting an Issue
- Check if the issue already exists in our [Issues](../../issues) section
- Make sure you're using the latest version of the application
- Try to reproduce the issue with minimal steps

### 📝 How to Report an Issue
1. Go to the [Issues](../../issues) page
2. Click **"New Issue"**

### 💬 Need Help?
- Search existing issues for similar problems
- For urgent issues, mention `@Koorbik` in your issue

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Development Team

- **Hubert Szadkowski ([Koorbik](https://github.com/Koorbik))** - Full Stack Developer

---

