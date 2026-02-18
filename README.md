# 💊 Pharmacy E-Commerce Platform - Backend API

Multi-tenant SaaS e-commerce platform for pharmacies in Turkey.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 🎯 Features

- **Multi-tenant Architecture**: Each pharmacy gets their own subdomain
- **Role-based Access**: Super Admin, Pharmacy Owner, Staff, Customer
- **Product Management**: Categories, stock tracking, discounts
- **Shopping Cart**: Real-time stock validation
- **Order Management**: Full lifecycle with status tracking
- **Staff Management**: Owner can manage pharmacy staff ⭐ NEW
- **Favorites System**: Save products for later
- **Address Management**: Multiple delivery addresses per user
- **Profile Management**: Update user info and password
- **JWT Authentication**: Secure token-based auth with refresh tokens
- **Audit Logging**: Complete action history

## 🏗️ Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                   NEXT.JS FRONTEND                          │
│              http://localhost:3000                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    SPRING BOOT API                          │
│              http://localhost:8080                          │
├─────────────────────────────────────────────────────────────┤
│  Controllers │ Services │ Repositories │ Security          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     POSTGRESQL / H2                         │
│  pharmacies│users│products│orders│carts│favorites│addresses │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 21
- Maven 3.8+
- Docker & Docker Compose (optional)
- PostgreSQL 16 (or use H2 for development)

### 1. Clone & Setup
```bash
git clone https://github.com/merve-ceylan/pharmacy.git
cd pharmacy
```

### 2. Run Application
```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. API Documentation
Open Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 4. Test Accounts
| Role | Email | Password |
|------|-------|----------|
| Super Admin | admin@pharmacy.com | Admin123!@# |
| Pharmacy Owner | owner@demo.com | Owner123!@# |
| Staff | staff@demo.com | Staff123!@# |
| Customer | test@test.com | Pharmacy2024!@# |

## 📚 API Documentation

### Authentication
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Customer registration | Public |
| POST | `/api/auth/login` | Login | Public |
| POST | `/api/auth/logout` | Logout | Authenticated |
| POST | `/api/auth/refresh` | Refresh token | Public |
| GET | `/api/auth/me` | Current user info | Authenticated |
| POST | `/api/auth/change-password` | Change password | Authenticated |

### Products
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/public/pharmacies/{id}/products` | List products | Public |
| GET | `/api/public/pharmacies/{id}/products/featured` | Featured products | Public |
| GET | `/api/public/pharmacies/{id}/products/slug/{slug}` | Product by slug | Public |
| GET | `/api/public/pharmacies/{id}/products/search?q=` | Search products | Public |
| GET | `/api/staff/products` | All products (admin) | Staff |
| POST | `/api/staff/products` | Create product | Staff |
| PUT | `/api/staff/products/{id}` | Update product | Staff |
| PATCH | `/api/staff/products/{id}/activate` | Activate product | Staff |
| PATCH | `/api/staff/products/{id}/deactivate` | Deactivate product | Staff |

### Categories
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/public/categories` | List categories | Public |
| GET | `/api/admin/categories` | All categories (admin) | Super Admin |
| POST | `/api/admin/categories` | Create category | Super Admin |
| PUT | `/api/admin/categories/{id}` | Update category | Super Admin |
| PATCH | `/api/admin/categories/{id}/activate` | Activate | Super Admin |
| PATCH | `/api/admin/categories/{id}/deactivate` | Deactivate | Super Admin |

### Cart
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/customer/cart/{pharmacyId}` | Get cart | Customer |
| POST | `/api/customer/cart/{pharmacyId}/items` | Add item | Customer |
| PUT | `/api/customer/cart/{pharmacyId}/items/{id}` | Update quantity | Customer |
| DELETE | `/api/customer/cart/{pharmacyId}/items/{id}` | Remove item | Customer |
| DELETE | `/api/customer/cart/{pharmacyId}` | Clear cart | Customer |

### Orders
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/customer/orders` | Create order | Customer |
| GET | `/api/customer/orders` | My orders | Customer |
| GET | `/api/customer/orders/{orderNumber}` | Order details | Customer |
| GET | `/api/staff/orders` | Pharmacy orders | Staff |
| GET | `/api/staff/orders/recent` | Recent orders | Staff |
| GET | `/api/staff/orders/stats` | Order statistics | Staff |
| PATCH | `/api/staff/orders/{orderNumber}/status` | Update status | Staff |

### Staff Management ⭐ NEW
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/pharmacy/staff` | List pharmacy staff | Owner |
| GET | `/api/pharmacy/staff/stats` | Staff statistics | Owner |
| POST | `/api/pharmacy/staff` | Add new staff member | Owner |
| PATCH | `/api/pharmacy/staff/{id}/activate` | Activate staff | Owner |
| PATCH | `/api/pharmacy/staff/{id}/deactivate` | Deactivate staff | Owner |

### Favorites
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/customer/favorites` | List favorites | Customer |
| POST | `/api/customer/favorites` | Add to favorites | Customer |
| DELETE | `/api/customer/favorites/{id}` | Remove from favorites | Customer |
| GET | `/api/customer/favorites/check/{productId}` | Check if favorited | Customer |

### Addresses
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/customer/addresses` | List addresses | Customer |
| GET | `/api/customer/addresses/{id}` | Get address | Customer |
| POST | `/api/customer/addresses` | Create address | Customer |
| PUT | `/api/customer/addresses/{id}` | Update address | Customer |
| DELETE | `/api/customer/addresses/{id}` | Delete address | Customer |
| PATCH | `/api/customer/addresses/{id}/default` | Set as default | Customer |

### Profile
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/customer/profile` | Get profile | Customer |
| PUT | `/api/customer/profile` | Update profile | Customer |

### Pharmacy Management
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/admin/pharmacies` | List all | Super Admin |
| POST | `/api/admin/pharmacies` | Create pharmacy | Super Admin |
| GET | `/api/pharmacy/info` | My pharmacy | Owner |
| PUT | `/api/pharmacy/info` | Update pharmacy | Owner |

## 📁 Project Structure
```
src/main/java/com/pharmacy/
├── config/
│   ├── SecurityConfig.java
│   └── DataSeeder.java
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── OrderController.java
│   ├── CartController.java
│   ├── FavoriteController.java
│   ├── AddressController.java
│   ├── ProfileController.java
│   ├── PharmacyController.java
│   ├── PharmacyStaffController.java    ⭐ NEW
│   └── PaymentController.java
├── entity/
│   ├── User.java
│   ├── Pharmacy.java
│   ├── Product.java
│   ├── Category.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Favorite.java
│   ├── Address.java
│   ├── Payment.java
│   └── AuditLog.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── FavoriteRepository.java
│   ├── AddressRepository.java
│   └── ...
├── service/
│   ├── AuthService.java
│   ├── ProductService.java
│   ├── FavoriteService.java
│   ├── AddressService.java
│   └── ...
├── security/
│   ├── SecurityConfig.java
│   ├── JwtService.java
│   └── JwtAuthenticationFilter.java
└── exception/
    └── GlobalExceptionHandler.java
```

## 🔒 Security Features

- JWT authentication with refresh tokens
- Password strength validation
- Account lockout after failed attempts
- Rate limiting (100 req/min general, 5 req/min login)
- Token blacklisting on logout
- Role-based access control
- CORS configuration for frontend

## 🧪 API Testing

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Pharmacy2024!@#"}'
```

### Add Staff Member
```bash
curl -X POST http://localhost:8080/api/pharmacy/staff \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newstaff@demo.com",
    "password": "SecurePass123!@#",
    "firstName": "Ahmet",
    "lastName": "Yılmaz",
    "phone": "05559876543"
  }'
```

### Add to Favorites
```bash
curl -X POST http://localhost:8080/api/customer/favorites \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1}'
```

### Create Address
```bash
curl -X POST http://localhost:8080/api/customer/addresses \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Ev",
    "fullName": "Test User",
    "phone": "5551234567",
    "city": "İstanbul",
    "district": "Kadıköy",
    "postalCode": "34710",
    "addressLine": "Test Mahallesi Test Sokak No:1",
    "isDefault": true
  }'
```

## 📋 Order Status Flow
```
PENDING → CONFIRMED → PREPARING → SHIPPED → DELIVERED
    ↓         ↓
 CANCELLED  CANCELLED
```

## 🛣️ Roadmap

- [x] Project setup & configuration
- [x] Entity & repository layer
- [x] Service layer with business logic
- [x] Security & JWT authentication
- [x] Exception handling
- [x] Controller & DTO layer
- [x] Favorites system
- [x] Address management
- [x] Profile management
- [x] Password change
- [x] Staff management ⭐ NEW
- [ ] Multi-tenant domain resolver
- [ ] iyzico payment integration
- [ ] Email notifications
- [ ] Product image upload
- [ ] Excel product import

## 🔗 Related Repositories

- **Frontend**: [pharmacy-frontend](https://github.com/merve-ceylan/pharmacy-frontend) - Next.js 14 Frontend

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 👨‍💻 Author

Built with ❤️ for Turkish pharmacies

## 📝 Project Status

**Public Version (Current):** ✅ Core features complete - Portfolio demonstration

**Private Development:** Additional enterprise features including:
- Payment integration (iyzico)
- Email notification system
- Advanced reporting & analytics
- Image upload & management
- Multi-tenant domain routing
- Production optimizations

This public repository demonstrates technical capabilities and architecture.
Production version with business-specific features is privately maintained.

---

**Last Updated:** February 2026  
**Status:** Portfolio Version - Complete