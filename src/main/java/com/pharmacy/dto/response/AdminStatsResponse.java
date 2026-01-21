package com.pharmacy.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminStatsResponse {

    // Super Admin Stats
    private Long totalPharmacies;
    private Long activePharmacies;
    private Long totalUsers;
    private Long premiumSubscriptions;

    // Pharmacy Owner Stats
    private Long todayOrders;
    private Long pendingOrders;
    private Long totalProducts;
    private BigDecimal todayRevenue;

    // Reports
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Long totalCustomers;
    private Map<String, Long> ordersByStatus;
    private List<DailyRevenueDto> revenueByDay;
    private List<TopProductDto> topProducts;

    public AdminStatsResponse() {}

    // Getters and Setters
    public Long getTotalPharmacies() { return totalPharmacies; }
    public void setTotalPharmacies(Long totalPharmacies) { this.totalPharmacies = totalPharmacies; }

    public Long getActivePharmacies() { return activePharmacies; }
    public void setActivePharmacies(Long activePharmacies) { this.activePharmacies = activePharmacies; }

    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }

    public Long getPremiumSubscriptions() { return premiumSubscriptions; }
    public void setPremiumSubscriptions(Long premiumSubscriptions) { this.premiumSubscriptions = premiumSubscriptions; }

    public Long getTodayOrders() { return todayOrders; }
    public void setTodayOrders(Long todayOrders) { this.todayOrders = todayOrders; }

    public Long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(Long pendingOrders) { this.pendingOrders = pendingOrders; }

    public Long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }

    public Long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Long totalCustomers) { this.totalCustomers = totalCustomers; }

    public Map<String, Long> getOrdersByStatus() { return ordersByStatus; }
    public void setOrdersByStatus(Map<String, Long> ordersByStatus) { this.ordersByStatus = ordersByStatus; }

    public List<DailyRevenueDto> getRevenueByDay() { return revenueByDay; }
    public void setRevenueByDay(List<DailyRevenueDto> revenueByDay) { this.revenueByDay = revenueByDay; }

    public List<TopProductDto> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProductDto> topProducts) { this.topProducts = topProducts; }

    // Inner Classes
    public static class DailyRevenueDto {
        private String date;
        private BigDecimal revenue;

        public DailyRevenueDto() {}

        public DailyRevenueDto(String date, BigDecimal revenue) {
            this.date = date;
            this.revenue = revenue;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }

    public static class TopProductDto {
        private String name;
        private Long quantity;
        private BigDecimal revenue;

        public TopProductDto() {}

        public TopProductDto(String name, Long quantity, BigDecimal revenue) {
            this.name = name;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Long getQuantity() { return quantity; }
        public void setQuantity(Long quantity) { this.quantity = quantity; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }
}