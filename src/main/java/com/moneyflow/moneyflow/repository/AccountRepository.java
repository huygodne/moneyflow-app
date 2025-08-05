package com.moneyflow.moneyflow.repository;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.User; // Import User entity để dùng trong query
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Tìm tất cả các tài khoản thuộc về một người dùng cụ thể (sử dụng User entity)
    List<Account> findByUser(User user);

    // Tìm một tài khoản theo ID và User sở hữu (đảm bảo người dùng chỉ có thể truy cập tài khoản của mình)
    Optional<Account> findByIdAndUser(Long id, User user);

    Optional<Account> findByNameAndUser(String name, User user);
}