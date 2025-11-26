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
- **ngrok** (required for PayU webhook functionality) - [Download here](https://ngrok.com/download)

### 🚀 Quick Start (Recommended: Docker)

The recommended way to run the application is using **Docker Compose**, which sets up PostgreSQL database and all services automatically.

1. **Clone the repository**
   ```powershell
   git clone <repository-url>
   cd IWA
   ```

2. **Configure environment variables**
   
   Copy the example environment file:
   ```powershell
   cp .env.docker.example .env.docker
   ```
   
   Edit `.env.docker` file with your configuration. See the [Configuration](#-configuration) section below for detailed setup instructions.

3. **Start the application with Docker**
   ```powershell
   docker-compose up --build
   ```
   > 💡 This will start PostgreSQL database, backend, and frontend in containers.

4. **Expose backend with ngrok (REQUIRED for PayU webhooks)** (in a new terminal)
   ```powershell
   ngrok http 8080
   ```
   
   > ⚠️ **Important:** Copy the HTTPS forwarding URL (e.g., `https://abc123.ngrok-free.app`) and update your `PAYU_NOTIFY_URL` in the `.env.docker` file:
   > ```
   > PAYU_NOTIFY_URL=https://your-ngrok-url.ngrok-free.app/api/payments/notify
   > ```
   > Then restart the containers:
   > ```powershell
   > docker-compose down
   > docker-compose up
   > ```
   
   > 💡 **Why ngrok?** PayU sends payment notifications (webhooks) to your backend. Since PayU servers need to reach your local machine, ngrok creates a secure tunnel to expose your localhost:8080 to the internet.

5. **Access the application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080
   - PostgreSQL Database: localhost:5432

6. **Stop the application**
   ```powershell
   docker-compose down
   ```

### 🔧 Alternative: Local Development (Without Docker)

If you prefer to run the backend locally without Docker (uses H2 in-memory database):

1. **Set the application profile to `local`**
   
   Edit `iwa_backend/src/main/resources/application.properties` and ensure the active profile is set to `local`:
   ```properties
   spring.profiles.active=local
   ```

2. **Configure environment variables**
   
   Create `iwa_backend/.env` file with your configuration. See the [Configuration](#-configuration) section below for detailed setup instructions.

3. **Start the backend**
   ```powershell
   cd iwa_backend
   ./mvnw spring-boot:run
   ```
   > 💡 The backend uses H2 in-memory database with `local` profile - no external database setup needed!

4. **Expose backend with ngrok (REQUIRED for PayU webhooks)** (in a new terminal)
   ```powershell
   ngrok http 8080
   ```
   
   Update your `PAYU_NOTIFY_URL` in the `.env` file and restart the backend.

5. **Start the frontend** (in a new terminal)
   ```powershell
   cd iwa-frontend
   npm install  # First time only
   ng serve
   ```

6. **Access the application**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080
   - Backend via ngrok: https://your-ngrok-url.ngrok-free.app
   - H2 Database Console: http://localhost:8080/h2-console (for local development)

### 👤 Default Users

The application comes with pre-seeded users for testing:

| Email | Password | Role |
|-------|----------|------|
| `alice@acme.com` | `admin123` | ADMIN |
| `bob@acme.com` | `user123` | USER |

## 🔧 Configuration

### Backend Configuration

The backend supports two profiles:
- **`local`** - Uses H2 in-memory database (for local development without Docker)
- **`dev`** - Uses PostgreSQL database (for Docker deployment)

#### Docker Deployment (PostgreSQL) - RECOMMENDED
Docker uses profile `dev` with PostgreSQL database.

Copy the example environment file and edit it:
```bash
cp .env.docker.example .env.docker
```

Then configure `.env.docker` with your values:
```bash
# Database Configuration
POSTGRES_USER=user
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=service_db

# JWT (REQUIRED)
JWT_SECRET_KEY=your_secret_key_here
JWT_EXPIRATION=86400000

# Email (REQUIRED)
SUPPORT_EMAIL=your_email@gmail.com
APP_PASSWORD=your_gmail_app_password

# Google Calendar OAuth (optional)
GOOGLE_CALENDAR_CLIENT_ID=your_client_id
GOOGLE_CALENDAR_SECRET=your_client_secret
GOOGLE_AUTH_CLIENT_ID=your_auth_client_id
GOOGLE_AUTH_FRONTEND_CLIENT_ID=your_frontend_client_id

# PayU Configuration (REQUIRED for payments)
PAYU_BASE_URL=https://secure.snd.payu.com
PAYU_OAUTH_CLIENT_ID=your_payu_client_id
PAYU_OAUTH_CLIENT_SECRET=your_payu_client_secret
PAYU_POS_ID=your_payu_pos_id
PAYU_SECOND_KEY_MD5=your_payu_second_key
# ⚠️ IMPORTANT: Use ngrok HTTPS URL here, NOT localhost!
# Run: ngrok http 8080
# Then copy the HTTPS URL (e.g., https://abc123.ngrok-free.app)
PAYU_NOTIFY_URL=https://your-ngrok-url.ngrok-free.app/api/payments/notify
```

> ⚠️ **Critical for PayU Webhooks:** The `PAYU_NOTIFY_URL` MUST be a publicly accessible HTTPS URL. Run `ngrok http 8080` to create a tunnel and use the provided HTTPS URL.

#### Local Development (H2 Database) - ALTERNATIVE
The backend uses profile `local` when running locally without Docker.

Create `iwa_backend/.env` file with:
```bash
# JWT (REQUIRED)
JWT_SECRET_KEY=your_secret_key_here
JWT_EXPIRATION=86400000

# Email (REQUIRED)
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

# PayU Configuration (REQUIRED for payments)
PAYU_BASE_URL=https://secure.snd.payu.com
PAYU_OAUTH_CLIENT_ID=your_payu_client_id
PAYU_OAUTH_CLIENT_SECRET=your_payu_client_secret
PAYU_POS_ID=your_payu_pos_id
PAYU_SECOND_KEY_MD5=your_payu_second_key
# ⚠️ IMPORTANT: Use ngrok HTTPS URL here, NOT localhost!
# Run: ngrok http 8080
# Then copy the HTTPS URL (e.g., https://abc123.ngrok-free.app)
PAYU_NOTIFY_URL=https://your-ngrok-url.ngrok-free.app/api/payments/notify
```

> ⚠️ **Critical for PayU Webhooks:** The `PAYU_NOTIFY_URL` MUST be a publicly accessible HTTPS URL. Run `ngrok http 8080` to create a tunnel and use the provided HTTPS URL.

> 💡 **Note:** H2 is an in-memory database, so all data is lost when the application stops. For persistent data, use Docker with PostgreSQL.

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

