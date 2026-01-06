package com.pharmacy.service;

import com.pharmacy.entity.Address;
import com.pharmacy.entity.User;
import com.pharmacy.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    @Autowired
    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
    }

    public Optional<Address> getAddressById(Long id, User user) {
        return addressRepository.findByIdAndUser(id, user);
    }

    public Optional<Address> getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrue(user);
    }

    @Transactional
    public Address createAddress(User user, String title, String fullName, String phone,
                                 String city, String district, String postalCode,
                                 String addressLine, boolean isDefault) {

        // If this is the first address or marked as default, clear other defaults
        if (isDefault || addressRepository.countByUser(user) == 0) {
            addressRepository.clearDefaultForUser(user);
            isDefault = true;
        }

        Address address = new Address();
        address.setUser(user);
        address.setTitle(title);
        address.setFullName(fullName);
        address.setPhone(phone);
        address.setCity(city);
        address.setDistrict(district);
        address.setPostalCode(postalCode);
        address.setAddressLine(addressLine);
        address.setDefault(isDefault);

        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Address address, String title, String fullName, String phone,
                                 String city, String district, String postalCode,
                                 String addressLine) {

        address.setTitle(title);
        address.setFullName(fullName);
        address.setPhone(phone);
        address.setCity(city);
        address.setDistrict(district);
        address.setPostalCode(postalCode);
        address.setAddressLine(addressLine);

        return addressRepository.save(address);
    }

    @Transactional
    public void setDefaultAddress(User user, Long addressId) {
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Adres bulunamadı"));

        addressRepository.clearDefaultForUser(user);
        address.setDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Adres bulunamadı"));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // If deleted address was default, set another one as default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
            if (!remaining.isEmpty()) {
                remaining.get(0).setDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }
    }
}