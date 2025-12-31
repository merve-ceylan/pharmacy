# 💊 Pharmacy E-Commerce Platform

A multi-tenant SaaS platform that enables pharmacies to create and manage their own e-commerce stores. Built with Spring Boot 3.2, PostgreSQL, and JWT authentication.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Security](#security)
- [Roadmap](#roadmap)
- [License](#license)

## 🎯 Overview

Pharmacy Platform is a B2B SaaS solution that allows pharmacies to launch their own branded e-commerce websites. Each pharmacy gets a dedicated subdomain (e.g., `ozaneczanesi.pharmacyplatform.com`) or can connect their custom domain.



## ✨ Features

### Multi-Tenant Architecture
- Shared database with pharmacy-level data isolation
- Subdomain and custom domain support
- Pharmacy-specific branding (logo, colors)

### User Management
- Role-based access control (Super Admin, Pharmacy Owner, Staff, Customer)
- JWT authentication with refresh tokens
- Account lockout after failed login attempts
- Strong password validation

### Product Management
- Category hierarchy support
- SKU and barcode tracking
- Stock management with low-stock alerts
- Bulk product import via Excel

### Order Management
- Complete order lifecycle (Pending → Confirmed → Preparing → Shipped → Delivered)
- Multiple delivery options (Courier, Cargo)
- Order cancellation with stock restoration
- Manual tracking number entry

### Payment Integration
- iyzico payment gateway integration
- Full and partial refund support
- Payment status tracking

### Security
- JWT-based authentication
- Rate limiting (brute force protection)
- Token blacklisting for logout
- Request logging and audit trails
- Input sanitization (XSS protection)


## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.0 |
| Security | Spring Security 6, JWT (jjwt 0.12.3) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA, Hibernate 6 |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |
| API Docs | SpringDoc OpenAPI |

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 16 (or use Docker)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/merve-ceylan/pharmacy-platform.git
cd pharmacy-platform
```

2. **Start PostgreSQL with Docker**
```bash
docker-compose up -d
```

3. **Configure application properties**
```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

4. **Build the project**
```bash
mvn clean install
```

5. **Run the application**
```bash
mvn spring-boot:run
```

## 📚 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/login` | User login | Public |
| POST | `/api/auth/register` | Customer registration | Public |
| POST | `/api/auth/register/pharmacy-owner` | Register pharmacy owner | Super Admin |
| POST | `/api/auth/register/staff` | Register staff member | Pharmacy Owner |
| POST | `/api/auth/logout` | Logout (invalidate token) | Authenticated |
| POST | `/api/auth/refresh` | Refresh access token | Public |
| GET | `/api/auth/me` | Get current user info | Authenticated |
| POST | `/api/auth/change-password` | Change password | Authenticated |
| POST | `/api/auth/forgot-password` | Request password reset | Public |

### Request/Response Examples

**Login Request**
```json
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Login Response**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "CUSTOMER"
  }
}
```

## 🔒 Security

### Security Features

- **Password Requirements**: Minimum 8 characters, uppercase, lowercase, digit, special character
- **Account Lockout**: 5 failed attempts = 30 minute lockout
- **Rate Limiting**: 100 requests/minute general, 5 requests/minute for login
- **Token Blacklisting**: Tokens are invalidated on logout
- **Audit Logging**: All security events are logged

## 🗺️ Roadmap

- [ ] Product management controllers
- [ ] Order management controllers
- [ ] Shopping cart controllers
- [ ] Excel bulk import
- [ ] iyzico payment integration
- [ ] Email notifications
- [ ] Admin dashboard
- [ ] Customer mobile app

## 📄 License

This project is proprietary software. All rights reserved.

---

**Built with ❤️ for pharmacies in Turkey*
