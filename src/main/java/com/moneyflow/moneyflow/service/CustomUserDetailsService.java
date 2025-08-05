package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import này để định nghĩa quyền

import java.util.Collections; // Import này cho Collections.singletonList

//@Service // Đánh dấu đây là một Spring Service và cũng là một Bean
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm kiếm người dùng trong database của bạn bằng username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Trả về một đối tượng UserDetails mà Spring Security có thể sử dụng
        // Mật khẩu đã được mã hóa trong database, Spring Security sẽ tự động so sánh
        // Hiện tại, chúng ta gán một vai trò đơn giản là "ROLE_USER". Sau này có thể mở rộng.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Gán quyền ROLE_USER
        );
    }
}