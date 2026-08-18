package com.fatmanur.ecommerce.user.controller;

import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.user.dto.AddressRequest;
import com.fatmanur.ecommerce.user.dto.AddressResponse;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import com.fatmanur.ecommerce.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal UserDetails user,
                                                      @Valid @RequestBody AddressRequest request) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(addressService.addAddress(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(addressService.getAddresses(userId));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddress(@AuthenticationPrincipal UserDetails user,
                                                      @PathVariable Long addressId) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(addressService.getAddress(userId, addressId));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal UserDetails user,
                                              @PathVariable Long addressId) {
        Long userId = getUserId(user);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails user) {
        String email = user.getUsername();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"))
                .getId();
    }
}
