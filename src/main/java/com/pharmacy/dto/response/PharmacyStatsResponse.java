package com.pharmacy.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class PharmacyStatsResponse {

    private Long pharmacyId;
    private String pharmacyName;
    private List<MonthlyStats> monthlyStats;
    private BigDecimal totalRevenue;
    private Long totalOrders;

    public PharmacyStatsResponse() {}

    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public List<MonthlyStats> getMonthlyStats() { return monthlyStats; }
    public void setMonthlyStats(List<MonthlyStats> monthlyStats) { this.monthlyStats = monthlyStats; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public static class MonthlyStats {
        private String month;
        private Long orders;
        private BigDecimal revenue;

        public MonthlyStats() {}

        public MonthlyStats(String month, Long orders, BigDecimal revenue) {
            this.month = month;
            this.orders = orders;
            this.revenue = revenue;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }

        public Long getOrders() { return orders; }
        public void setOrders(Long orders) { this.orders = orders; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }
}
