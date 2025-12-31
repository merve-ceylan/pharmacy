# 💊 Pharmacy E-Commerce Platform

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
- **JWT Authentication**: Secure token-based auth with refresh tokens
- **Audit Logging**: Complete action history

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENTS                                 │
│  (demo.eczanem.com)  (ozan.eczanem.com)  (admin.eczanem.com)│
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    SPRING BOOT API                           │
├─────────────────────────────────────────────────────────────┤
│  Controllers │ Services │ Repositories │ Security           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     POSTGRESQL                               │
│  pharmacies│users│products│orders│carts│payments│audit_logs │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 21
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)

### 1. Clone & Setup
```bash
git clone https://github.com/yourusername/pharmacy-platform.git
cd pharmacy-platform
```

### 2. Start Database
```bash
docker-compose up -d
```

### 3. Run Application
```bash
./mvnw spring-boot:run
```

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

### Products
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/public/pharmacies/{id}/products` | List products | Public |
| GET | `/api/public/pharmacies/{id}/products/featured` | Featured products | Public |
| GET | `/api/public/pharmacies/{id}/products/search?q=` | Search products | Public |
| GET | `/api/staff/products` | All products (admin) | Staff |
| POST | `/api/staff/products` | Create product | Staff |
| PUT | `/api/staff/products/{id}` | Update product | Staff |
| PATCH | `/api/staff/products/{id}/stock` | Update stock | Staff |

### Categories
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/public/categories` | List categories | Public |
| POST | `/api/admin/categories` | Create category | Super Admin |
| PUT | `/api/admin/categories/{id}` | Update category | Super Admin |

### Cart
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/customer/cart/{pharmacyId}` | Get cart | Customer |
| POST | `/api/customer/cart/{pharmacyId}/items` | Add item | Customer |
| PUT | `/api/customer/cart/{pharmacyId}/items/{id}` | Update quantity | Customer |
| DELETE | `/api/customer/cart/{pharmacyId}/items/{id}` | Remove item | Customer |

### Orders
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/customer/orders` | Create order | Customer |
| GET | `/api/customer/orders` | My orders | Customer |
| GET | `/api/customer/orders/{orderNumber}` | Order details | Customer |
| GET | `/api/staff/orders` | Pharmacy orders | Staff |
| PATCH | `/api/staff/orders/{orderNumber}/status` | Update status | Staff |

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
│   └── DataSeeder.java
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── OrderController.java
│   ├── CartController.java
│   ├── PharmacyController.java
│   └── PaymentController.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ProductCreateRequest.java
│   │   ├── OrderCreateRequest.java
│   │   └── ...
│   └── response/
│       ├── AuthResponse.java
│       ├── ProductResponse.java
│       ├── OrderResponse.java
│       ├── ApiResponse.java
│       ├── PageResponse.java
│       └── ...
├── entity/
│   ├── BaseEntity.java
│   ├── User.java
│   ├── Pharmacy.java
│   ├── Product.java
│   ├── Category.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Payment.java
│   └── AuditLog.java
├── enums/
│   ├── UserRole.java
│   ├── OrderStatus.java
│   ├── PaymentStatus.java
│   └── ...
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   └── ...
├── mapper/
│   ├── ProductMapper.java
│   ├── OrderMapper.java
│   ├── CartMapper.java
│   └── ...
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   └── ...
├── security/
│   ├── SecurityConfig.java
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── ...
└── service/
    ├── AuthService.java
    ├── ProductService.java
    ├── OrderService.java
    ├── CartService.java
    └── ...
```

## 🔒 Security Features

- JWT authentication with refresh tokens
- Password strength validation
- Account lockout after failed attempts
- Rate limiting (100 req/min general, 5 req/min login)
- Token blacklisting on logout
- Role-based access control

## 🧪 API Testing

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@demo.com","password":"Owner123!@#"}'
```

### Get Products
```bash
curl http://localhost:8080/api/staff/products \
  -H "Authorization: Bearer <token>"
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/customer/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "pharmacyId": 1,
    "deliveryType": "CARGO",
    "shippingAddress": "Test Address",
    "shippingCity": "İstanbul",
    "shippingDistrict": "Kadıköy",
    "shippingPostalCode": "34700",
    "shippingPhone": "05551234567"
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
- [x] API testing
- [ ] Multi-tenant domain resolver
- [ ] iyzico payment integration
- [ ] Email notifications
- [ ] Frontend (React/Next.js)
- [ ] Admin dashboard
- [ ] Excel product import

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 👨‍💻 Author

Built with ❤️ for Turkish pharmacies*