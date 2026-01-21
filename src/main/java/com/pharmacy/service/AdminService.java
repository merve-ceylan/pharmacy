package com.pharmacy.service;

import com.pharmacy.dto.response.AdminStatsResponse;
import com.pharmacy.dto.response.PharmacyStatsResponse;
import com.pharmacy.entity.Order;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.entity.User;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.enums.PharmacyStatus;
import com.pharmacy.enums.SubscriptionPlan;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.*;
import com.pharmacy.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;

    public AdminService(PharmacyRepository pharmacyRepository,
                        UserRepository userRepository,
                        OrderRepository orderRepository,
                        ProductRepository productRepository,
                        SecurityUtils securityUtils) {
        this.pharmacyRepository = pharmacyRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Super Admin Dashboard Stats
     */
    public AdminStatsResponse getSuperAdminStats() {
        long totalPharmacies = pharmacyRepository.count();
        long activePharmacies = pharmacyRepository.countByStatus(PharmacyStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long premiumSubscriptions = pharmacyRepository.countBySubscriptionPlan(SubscriptionPlan.PRO);

        AdminStatsResponse response = new AdminStatsResponse();
        response.setTotalPharmacies(totalPharmacies);
        response.setActivePharmacies(activePharmacies);
        response.setTotalUsers(totalUsers);
        response.setPremiumSubscriptions(premiumSubscriptions);

        return response;
    }

    /**
     * Pharmacy Monthly Stats (for Super Admin)
     */
    public PharmacyStatsResponse getPharmacyStats(Long pharmacyId, int months) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Eczane bulunamadı"));

        List<PharmacyStatsResponse.MonthlyStats> monthlyStats = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < months; i++) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            List<Order> orders = orderRepository.findByPharmacyIdAndCreatedAtBetween(
                    pharmacyId, startOfMonth, endOfMonth);

            long orderCount = orders.size();
            BigDecimal revenue = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyStats.add(new PharmacyStatsResponse.MonthlyStats(
                    month.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    orderCount,
                    revenue
            ));

            totalRevenue = totalRevenue.add(revenue);
            totalOrders += orderCount;
        }

        PharmacyStatsResponse response = new PharmacyStatsResponse();
        response.setPharmacyId(pharmacyId);
        response.setPharmacyName(pharmacy.getName());
        response.setMonthlyStats(monthlyStats);
        response.setTotalRevenue(totalRevenue);
        response.setTotalOrders(totalOrders);

        return response;
    }

    /**
     * Pharmacy Owner/Staff Reports
     */
    public AdminStatsResponse getPharmacyReports(String range) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        Long pharmacyId = currentUser.getPharmacy() != null ? currentUser.getPharmacy().getId() : null;

        if (pharmacyId == null) {
            throw new ResourceNotFoundException("Kullanıcıya bağlı eczane bulunamadı");
        }

        LocalDateTime startDate = getStartDate(range);
        LocalDateTime endDate = LocalDateTime.now();

        List<Order> orders = orderRepository.findByPharmacyIdAndCreatedAtBetween(
                pharmacyId, startDate, endDate);

        // Basic stats
        long totalOrdersCount = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrdersCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrdersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Orders by status
        Map<String, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));

        // Revenue by day
        List<AdminStatsResponse.DailyRevenueDto> revenueByDay = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AdminStatsResponse.DailyRevenueDto(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList());

        // Top products
        Map<String, Long> productQuantities = new HashMap<>();
        Map<String, BigDecimal> productRevenues = new HashMap<>();

        orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    String productName = item.getProduct().getName();
                    productQuantities.merge(productName, (long) item.getQuantity(), Long::sum);
                    BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    productRevenues.merge(productName, subtotal, BigDecimal::add);
                });

        List<AdminStatsResponse.TopProductDto> topProducts = productQuantities.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new AdminStatsResponse.TopProductDto(
                        e.getKey(),
                        e.getValue(),
                        productRevenues.get(e.getKey())
                ))
                .collect(Collectors.toList());

        // Unique customers
        long totalCustomers = orders.stream()
                .map(o -> o.getCustomer().getId())
                .distinct()
                .count();

        // Total products
        long totalProducts = productRepository.countByPharmacyId(pharmacyId);

        // Today stats
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOrders = orders.stream()
                .filter(o -> o.getCreatedAt().isAfter(todayStart))
                .count();

        BigDecimal todayRevenue = orders.stream()
                .filter(o -> o.getCreatedAt().isAfter(todayStart))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        AdminStatsResponse response = new AdminStatsResponse();
        response.setTotalOrders(totalOrdersCount);
        response.setTotalRevenue(totalRevenue);
        response.setAverageOrderValue(averageOrderValue);
        response.setTotalCustomers(totalCustomers);
        response.setTotalProducts(totalProducts);
        response.setTodayOrders(todayOrders);
        response.setTodayRevenue(todayRevenue);
        response.setPendingOrders(pendingOrders);
        response.setOrdersByStatus(ordersByStatus);
        response.setRevenueByDay(revenueByDay);
        response.setTopProducts(topProducts);

        return response;
    }

    private LocalDateTime getStartDate(String range) {
        LocalDate today = LocalDate.now();
        switch (range.toLowerCase()) {
            case "month":
                return today.minusMonths(1).atStartOfDay();
            case "year":
                return today.minusYears(1).atStartOfDay();
            case "week":
            default:
                return today.minusWeeks(1).atStartOfDay();
        }
    }
}
