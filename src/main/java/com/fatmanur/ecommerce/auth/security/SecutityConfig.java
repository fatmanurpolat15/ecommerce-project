package com.fatmanur.ecommerce.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecutityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();  //There is no actual password in dataabsede.
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())  //Şimdilik Postman'den REST API test ediyoruz. CSRF koruması tarayıcı tabanlı senaryolarda önemlidir; API'mizi bu aşamada Postman'den test edebilmek için kapatıyoruz.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register").permitAll()  //auth/register adresine login olmadan herkes erişebilir.
                        .anyRequest().authenticated() //Endpoints other than this should require authentication.
                );

        return http.build();
    }


}
