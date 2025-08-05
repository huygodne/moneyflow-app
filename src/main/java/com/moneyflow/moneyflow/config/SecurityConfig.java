package com.moneyflow.moneyflow.config;

import com.moneyflow.moneyflow.service.UserService; // Import đúng UserService
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // THÊM DÒNG NÀY
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter; // THÊM IMPORT NÀY

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // THÊM ANNOTATION NÀY ĐỂ BẬT @PreAuthorize
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Sử dụng UserService làm UserDetailsService chính
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Sử dụng UserService đã được @Primary làm UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean // Để xử lý các phương thức DELETE/PUT/PATCH từ form HTML
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF tạm thời để dễ phát triển, nên bật lại cho production
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/error").permitAll() // Cho phép truy cập các trang này mà không cần xác thực
                        .anyRequest().authenticated() // Tất cả các request khác yêu cầu xác thực
                )
                .formLogin(form -> form
                        .loginPage("/login") // Trang đăng nhập tùy chỉnh
                        .defaultSuccessUrl("/", true) // Chuyển hướng đến trang chủ sau khi đăng nhập thành công
                        .permitAll() // Cho phép tất cả truy cập trang đăng nhập
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL để đăng xuất
                        .logoutSuccessUrl("/login?logout") // Chuyển hướng sau khi đăng xuất thành công
                        .permitAll()
                );

        http.authenticationProvider(authenticationProvider);

        return http.build();
    }
}