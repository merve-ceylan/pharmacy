package com.pharmacy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pharmacyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pharmacy E-Commerce Platform API")
                        .description("""
                    Multi-tenant SaaS e-commerce platform for pharmacies in Turkey.
                    
                    ## Features
                    - 🔐 JWT Authentication with refresh tokens
                    - 👥 Role-based access control (Super Admin, Pharmacy Owner, Staff, Customer)
                    - 🏪 Multi-tenant architecture (each pharmacy has subdomain)
                    - 🛒 Shopping cart and order management
                    - 💳 Payment integration ready
                    
                    ## Authentication
                    Use the `/api/auth/login` endpoint to get a JWT token, then click the **Authorize** button 
                    and enter: `Bearer <your_token>`
                    
                    ## Test Accounts
                    | Role | Email | Password |
                    |------|-------|----------|
                    | Super Admin | admin@pharmacy.com | Admin123!@# |
                    | Pharmacy Owner | owner@demo.com | Owner123!@# |
                    | Staff | staff@demo.com | Staff123!@# |
                    | Customer | test@test.com | Pharmacy2024!@# |
                    """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pharmacy Platform Team")
                                .email("support@pharmacyplatform.com")
                                .url("https://pharmacyplatform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.pharmacyplatform.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Login, register, logout, token refresh"),
                        new Tag().name("Products").description("Product management (CRUD, stock, search)"),
                        new Tag().name("Categories").description("Category management"),
                        new Tag().name("Cart").description("Shopping cart operations"),
                        new Tag().name("Orders").description("Order management and status tracking"),
                        new Tag().name("Pharmacy").description("Pharmacy management (Admin & Owner)"),
                        new Tag().name("Payments").description("Payment processing and refunds"),
                        new Tag().name("Users").description("User management")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .name("Bearer Authentication")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")));
    }
}