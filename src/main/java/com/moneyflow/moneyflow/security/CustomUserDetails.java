package com.moneyflow.moneyflow.security;

import com.moneyflow.moneyflow.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // Import này cũng quan trọng
import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    private final String fullName; // Thêm trường fullName

    // Constructor để ánh xạ từ entity User của bạn
    public CustomUserDetails(User user) {
        super(user.getUsername(),
                user.getPassword(),
                true, true, true, true,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))); // Hoặc vai trò cụ thể của bạn

        this.fullName = user.getFullName(); // Gán fullName
    }

    // Getter cho fullName
    public String getFullName() {
        return fullName;
    }

    // (Tùy chọn) Nếu bạn muốn ghi đè hoặc thêm các logic khác của UserDetails, bạn có thể làm ở đây.
    // Hiện tại, chúng ta đang kế thừa gần như tất cả các phương thức từ org.springframework.security.core.userdetails.User
}