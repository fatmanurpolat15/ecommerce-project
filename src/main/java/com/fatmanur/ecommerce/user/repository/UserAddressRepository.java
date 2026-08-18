package com.fatmanur.ecommerce.user.repository;

import com.fatmanur.ecommerce.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdAndDeletedFalse(Long userId);

    Optional<UserAddress> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
}
