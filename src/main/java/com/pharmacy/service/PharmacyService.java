package com.pharmacy.service;

import com.pharmacy.entity.Pharmacy;
import com.pharmacy.enums.PharmacyStatus;
import com.pharmacy.enums.SubscriptionPlan;
import com.pharmacy.enums.PaymentPeriod;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.repository.PharmacyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    public PharmacyService(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    public Pharmacy createPharmacy(Pharmacy pharmacy) {
        if (pharmacy.getSubdomain() != null && pharmacyRepository.existsBySubdomain(pharmacy.getSubdomain())) {
            throw new DuplicateResourceException("Pharmacy", "subdomain", pharmacy.getSubdomain());
        }

        if (pharmacy.getCustomDomain() != null && pharmacyRepository.existsByCustomDomain(pharmacy.getCustomDomain())) {
            throw new DuplicateResourceException("Pharmacy", "customDomain", pharmacy.getCustomDomain());
        }

        pharmacy.setStatus(PharmacyStatus.ACTIVE);
        pharmacy.setSubscriptionStartDate(LocalDate.now());
        setSubscriptionFees(pharmacy);
        calculateNextPaymentDate(pharmacy);

        return pharmacyRepository.save(pharmacy);
    }

    public Optional<Pharmacy> findById(Long id) {
        return pharmacyRepository.findById(id);
    }

    public Pharmacy getById(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", id));
    }

    public Optional<Pharmacy> findBySubdomain(String subdomain) {
        return pharmacyRepository.findBySubdomain(subdomain);
    }

    public Optional<Pharmacy> findByCustomDomain(String customDomain) {
        return pharmacyRepository.findByCustomDomain(customDomain);
    }

    public Optional<Pharmacy> findByDomain(String domain) {
        Optional<Pharmacy> pharmacy = pharmacyRepository.findBySubdomain(domain);
        if (pharmacy.isPresent()) {
            return pharmacy;
        }
        return pharmacyRepository.findByCustomDomain(domain);
    }

    public List<Pharmacy> findAllActive() {
        return pharmacyRepository.findByStatus(PharmacyStatus.ACTIVE);
    }

    public List<Pharmacy> findAll() {
        return pharmacyRepository.findAll();
    }

    public Pharmacy updatePharmacy(Pharmacy pharmacy) {
        return pharmacyRepository.save(pharmacy);
    }

    public Pharmacy upgradePlan(Long pharmacyId, SubscriptionPlan newPlan) {
        Pharmacy pharmacy = getById(pharmacyId);

        if (pharmacy.getSubscriptionPlan() == SubscriptionPlan.PRO) {
            throw new BusinessException("Already on PRO plan", "ALREADY_PRO");
        }

        pharmacy.setSubscriptionPlan(newPlan);
        setSubscriptionFees(pharmacy);

        return pharmacyRepository.save(pharmacy);
    }

    public Pharmacy suspendPharmacy(Long pharmacyId) {
        Pharmacy pharmacy = getById(pharmacyId);

        pharmacy.setStatus(PharmacyStatus.SUSPENDED);
        pharmacy.setSuspendedAt(java.time.LocalDateTime.now());

        return pharmacyRepository.save(pharmacy);
    }

    public Pharmacy reactivatePharmacy(Long pharmacyId) {
        Pharmacy pharmacy = getById(pharmacyId);

        pharmacy.setStatus(PharmacyStatus.ACTIVE);
        pharmacy.setSuspendedAt(null);
        pharmacy.setGracePeriodEnd(null);
        calculateNextPaymentDate(pharmacy);

        return pharmacyRepository.save(pharmacy);
    }

    public void validatePharmacyActive(Long pharmacyId) {
        Pharmacy pharmacy = getById(pharmacyId);
        if (pharmacy.getStatus() == PharmacyStatus.SUSPENDED) {
            throw BusinessException.pharmacySuspended();
        }
        if (pharmacy.getStatus() == PharmacyStatus.CANCELLED) {
            throw BusinessException.subscriptionExpired();
        }
    }

    public boolean isSubdomainAvailable(String subdomain) {
        return !pharmacyRepository.existsBySubdomain(subdomain);
    }

    public boolean isCustomDomainAvailable(String customDomain) {
        return !pharmacyRepository.existsByCustomDomain(customDomain);
    }

    private void setSubscriptionFees(Pharmacy pharmacy) {
        if (pharmacy.getSubscriptionPlan() == SubscriptionPlan.STANDARD) {
            pharmacy.setSetupFee(new BigDecimal("3000"));
            pharmacy.setMonthlyFee(new BigDecimal("3500"));
        } else {
            pharmacy.setSetupFee(new BigDecimal("5000"));
            pharmacy.setMonthlyFee(new BigDecimal("5000"));
        }
    }

    private void calculateNextPaymentDate(Pharmacy pharmacy) {
        LocalDate startDate = pharmacy.getSubscriptionStartDate();
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        if (pharmacy.getPaymentPeriod() == PaymentPeriod.MONTHLY) {
            pharmacy.setNextPaymentDate(startDate.plusMonths(1));
        } else {
            pharmacy.setNextPaymentDate(startDate.plusYears(1));
        }
    }
}