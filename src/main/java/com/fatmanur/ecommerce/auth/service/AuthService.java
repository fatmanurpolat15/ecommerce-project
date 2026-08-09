package com.fatmanur.ecommerce.auth.service;

import com.fatmanur.ecommerce.auth.dto.RegisterRequest;
import com.fatmanur.ecommerce.user.entity.User;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.fatmanur.ecommerce.user.entity.User;

@Service
public class AuthService {
    //Cunsractors
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository ,
                       PasswordEncoder passwordEncoder){

        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }

    public void register(RegisterRequest request){
        if (userRepository.existsByEmail(request.email())){
            throw new RuntimeException("Email already registered");  //The process is halted if the email is registered.
        }

        String encodedPassword = passwordEncoder.encode(request.password()); //It is hashed with BCrypt


        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .build();
        userRepository.save(user);
    }

}
