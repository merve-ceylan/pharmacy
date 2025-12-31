package com.pharmacy.config;

import com.pharmacy.entity.*;
import com.pharmacy.enums.*;
import com.pharmacy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            PharmacyRepository pharmacyRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Super Admin oluştur
            if (userRepository.findByEmail("admin@pharmacy.com").isEmpty()) {
                User superAdmin = new User();
                superAdmin.setEmail("admin@pharmacy.com");
                superAdmin.setPassword(passwordEncoder.encode("Admin123!@#"));
                superAdmin.setFirstName("Super");
                superAdmin.setLastName("Admin");
                superAdmin.setPhone("05001234567");
                superAdmin.setRole(UserRole.SUPER_ADMIN);
                superAdmin.setActive(true);
                superAdmin.setEmailVerified(true);
                userRepository.save(superAdmin);
                log.info("✅ Super Admin created: admin@pharmacy.com / Admin123!@#");
            }

            // Test Eczane oluştur
            Pharmacy pharmacy = null;
            if (pharmacyRepository.findBySubdomain("demo").isEmpty()) {
                pharmacy = new Pharmacy();
                pharmacy.setName("Demo Eczanesi");
                pharmacy.setSubdomain("demo");
                pharmacy.setEmail("demo@eczane.com");
                pharmacy.setPhone("02121234567");
                pharmacy.setAddress("Atatürk Cad. No:123");
                pharmacy.setCity("İstanbul");
                pharmacy.setDistrict("Kadıköy");
                pharmacy.setPostalCode("34000");
                pharmacy.setTaxNumber("1234567890");
                pharmacy.setTaxOffice("Kadıköy VD");
                pharmacy.setSubscriptionPlan(SubscriptionPlan.PRO);
                pharmacy.setPaymentPeriod(PaymentPeriod.MONTHLY);
                pharmacy.setStatus(PharmacyStatus.ACTIVE);
                pharmacy.setSetupFee(new BigDecimal("500"));
                pharmacy.setMonthlyFee(new BigDecimal("300"));
                pharmacy.setSubscriptionStartDate(LocalDate.now());
                pharmacy.setNextPaymentDate(LocalDate.now().plusMonths(1));
                pharmacy.setPrimaryColor("#1B4F72");
                pharmacy.setSecondaryColor("#2E86AB");
                pharmacy = pharmacyRepository.save(pharmacy);
                log.info("✅ Demo Pharmacy created: demo.pharmacyplatform.com");
            } else {
                pharmacy = pharmacyRepository.findBySubdomain("demo").get();
            }

            // Pharmacy Owner oluştur
            final Pharmacy finalPharmacy = pharmacy;
            if (userRepository.findByEmail("owner@demo.com").isEmpty()) {
                User owner = new User();
                owner.setEmail("owner@demo.com");
                owner.setPassword(passwordEncoder.encode("Owner123!@#"));
                owner.setFirstName("Mehmet");
                owner.setLastName("Eczacı");
                owner.setPhone("05321234567");
                owner.setRole(UserRole.PHARMACY_OWNER);
                owner.setPharmacy(finalPharmacy);
                owner.setActive(true);
                owner.setEmailVerified(true);
                userRepository.save(owner);
                log.info("✅ Pharmacy Owner created: owner@demo.com / Owner123!@#");
            }

            // Staff oluştur
            if (userRepository.findByEmail("staff@demo.com").isEmpty()) {
                User staff = new User();
                staff.setEmail("staff@demo.com");
                staff.setPassword(passwordEncoder.encode("Staff123!@#"));
                staff.setFirstName("Ayşe");
                staff.setLastName("Personel");
                staff.setPhone("05331234567");
                staff.setRole(UserRole.STAFF);
                staff.setPharmacy(finalPharmacy);
                staff.setActive(true);
                staff.setEmailVerified(true);
                userRepository.save(staff);
                log.info("✅ Staff created: staff@demo.com / Staff123!@#");
            }

            // Kategoriler oluştur
            Category catAgrıKesici = null;
            Category catVitamin = null;
            Category catCiltBakım = null;

            if (categoryRepository.findBySlug("agri-kesici").isEmpty()) {
                catAgrıKesici = new Category();
                catAgrıKesici.setName("Ağrı Kesici");
                catAgrıKesici.setSlug("agri-kesici");
                catAgrıKesici.setDescription("Ağrı kesici ilaçlar");
                catAgrıKesici.setDisplayOrder(1);
                catAgrıKesici.setActive(true);
                catAgrıKesici = categoryRepository.save(catAgrıKesici);
                log.info("✅ Category created: Ağrı Kesici");
            } else {
                catAgrıKesici = categoryRepository.findBySlug("agri-kesici").get();
            }

            if (categoryRepository.findBySlug("vitamin").isEmpty()) {
                catVitamin = new Category();
                catVitamin.setName("Vitamin & Mineral");
                catVitamin.setSlug("vitamin");
                catVitamin.setDescription("Vitamin ve mineral takviyeleri");
                catVitamin.setDisplayOrder(2);
                catVitamin.setActive(true);
                catVitamin = categoryRepository.save(catVitamin);
                log.info("✅ Category created: Vitamin & Mineral");
            } else {
                catVitamin = categoryRepository.findBySlug("vitamin").get();
            }

            if (categoryRepository.findBySlug("cilt-bakim").isEmpty()) {
                catCiltBakım = new Category();
                catCiltBakım.setName("Cilt Bakım");
                catCiltBakım.setSlug("cilt-bakim");
                catCiltBakım.setDescription("Cilt bakım ürünleri");
                catCiltBakım.setDisplayOrder(3);
                catCiltBakım.setActive(true);
                catCiltBakım = categoryRepository.save(catCiltBakım);
                log.info("✅ Category created: Cilt Bakım");
            } else {
                catCiltBakım = categoryRepository.findBySlug("cilt-bakim").get();
            }

            // Ürünler oluştur
            if (productRepository.findByPharmacyIdAndSku(finalPharmacy.getId(), "PRD-001").isEmpty()) {
                Product p1 = new Product();
                p1.setPharmacy(finalPharmacy);
                p1.setCategory(catAgrıKesici);
                p1.setName("Parol 500mg");
                p1.setSlug("parol-500mg-" + finalPharmacy.getId());
                p1.setSku("PRD-001");
                p1.setBarcode("8699999999901");
                p1.setDescription("Parasetamol içeren ağrı kesici. Baş ağrısı, diş ağrısı ve ateş düşürücü olarak kullanılır.");
                p1.setPrice(new BigDecimal("45.90"));
                p1.setDiscountedPrice(new BigDecimal("39.90"));
                p1.setStockQuantity(100);
                p1.setLowStockThreshold(10);
                p1.setActive(true);
                p1.setFeatured(true);
                productRepository.save(p1);
                log.info("✅ Product created: Parol 500mg");
            }

            if (productRepository.findByPharmacyIdAndSku(finalPharmacy.getId(), "PRD-002").isEmpty()) {
                Product p2 = new Product();
                p2.setPharmacy(finalPharmacy);
                p2.setCategory(catAgrıKesici);
                p2.setName("Majezik 100mg");
                p2.setSlug("majezik-100mg-" + finalPharmacy.getId());
                p2.setSku("PRD-002");
                p2.setBarcode("8699999999902");
                p2.setDescription("Flurbiprofen içeren güçlü ağrı kesici ve iltihap giderici.");
                p2.setPrice(new BigDecimal("89.90"));
                p2.setStockQuantity(50);
                p2.setLowStockThreshold(5);
                p2.setActive(true);
                p2.setFeatured(false);
                productRepository.save(p2);
                log.info("✅ Product created: Majezik 100mg");
            }

            if (productRepository.findByPharmacyIdAndSku(finalPharmacy.getId(), "PRD-003").isEmpty()) {
                Product p3 = new Product();
                p3.setPharmacy(finalPharmacy);
                p3.setCategory(catVitamin);
                p3.setName("Supradyn Energy");
                p3.setSlug("supradyn-energy-" + finalPharmacy.getId());
                p3.setSku("PRD-003");
                p3.setBarcode("8699999999903");
                p3.setDescription("Multivitamin ve mineral içeren enerji takviyesi. 30 tablet.");
                p3.setPrice(new BigDecimal("185.00"));
                p3.setDiscountedPrice(new BigDecimal("159.00"));
                p3.setStockQuantity(75);
                p3.setLowStockThreshold(10);
                p3.setActive(true);
                p3.setFeatured(true);
                productRepository.save(p3);
                log.info("✅ Product created: Supradyn Energy");
            }

            if (productRepository.findByPharmacyIdAndSku(finalPharmacy.getId(), "PRD-004").isEmpty()) {
                Product p4 = new Product();
                p4.setPharmacy(finalPharmacy);
                p4.setCategory(catVitamin);
                p4.setName("Centrum Silver");
                p4.setSlug("centrum-silver-" + finalPharmacy.getId());
                p4.setSku("PRD-004");
                p4.setBarcode("8699999999904");
                p4.setDescription("50 yaş üstü için özel formül multivitamin. 60 tablet.");
                p4.setPrice(new BigDecimal("245.00"));
                p4.setStockQuantity(40);
                p4.setLowStockThreshold(5);
                p4.setActive(true);
                p4.setFeatured(false);
                productRepository.save(p4);
                log.info("✅ Product created: Centrum Silver");
            }

            if (productRepository.findByPharmacyIdAndSku(finalPharmacy.getId(), "PRD-005").isEmpty()) {
                Product p5 = new Product();
                p5.setPharmacy(finalPharmacy);
                p5.setCategory(catCiltBakım);
                p5.setName("La Roche-Posay Effaclar");
                p5.setSlug("la-roche-posay-effaclar-" + finalPharmacy.getId());
                p5.setSku("PRD-005");
                p5.setBarcode("8699999999905");
                p5.setDescription("Yağlı ve akneye eğilimli ciltler için temizleme jeli. 200ml.");
                p5.setPrice(new BigDecimal("320.00"));
                p5.setDiscountedPrice(new BigDecimal("289.00"));
                p5.setStockQuantity(30);
                p5.setLowStockThreshold(5);
                p5.setActive(true);
                p5.setFeatured(true);
                productRepository.save(p5);
                log.info("✅ Product created: La Roche-Posay Effaclar");
            }

            log.info("========================================");
            log.info("🚀 Database seeding completed!");
            log.info("========================================");
            log.info("Test Accounts:");
            log.info("  Super Admin : admin@pharmacy.com / Admin123!@#");
            log.info("  Owner       : owner@demo.com / Owner123!@#");
            log.info("  Staff       : staff@demo.com / Staff123!@#");
            log.info("  Customer    : test@test.com / Pharmacy2024!@#");
            log.info("========================================");
        };
    }
}
