package com.fatmanur.ecommerce.user.service;

import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.user.dto.AddressRequest;
import com.fatmanur.ecommerce.user.dto.AddressResponse;
import com.fatmanur.ecommerce.user.entity.User;
import com.fatmanur.ecommerce.user.entity.UserAddress;
import com.fatmanur.ecommerce.user.repository.UserAddressRepository;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserAddress address = UserAddress.builder()
                .user(user)
                .address(request.address())
                .street(request.street())
                .district(request.district())
                .city(request.city())
                .country(request.country())
                .zipCode(request.zipCode())
                .isDefault(true)
                .build();

        addressRepository.findByUserIdAndDeletedFalse(userId).stream()
                .filter(UserAddress::isDefault)
                .forEach(a -> a.setDefault(false));

        UserAddress saved = addressRepository.save(address);
        return toResponse(saved);
    }

    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AddressResponse getAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserIdAndDeletedFalse(addressId, userId)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));
        return toResponse(address);
    }

    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserIdAndDeletedFalse(addressId, userId)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));
        address.setDeleted(true);
        addressRepository.save(address);
    }

    private AddressResponse toResponse(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getAddress(),
                address.getStreet(),
                address.getDistrict(),
                address.getCity(),
                address.getCountry(),
                address.getZipCode(),
                address.isDefault()
        );
    }
}
