package com.moneyflow.moneyflow.repository;

import com.moneyflow.moneyflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Sử dụng Optional để xử lý trường hợp không tìm thấy

@Repository // Đánh dấu đây là một Spring component và repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm người dùng theo username. Optional giúp xử lý trường hợp không tìm thấy.
    Optional<User> findByUsername(String username);

    // Tìm người dùng theo email.
    Optional<User> findByEmail(String email);

    // Kiểm tra xem username đã tồn tại trong hệ thống chưa.
    boolean existsByUsername(String username);

    // Kiểm tra xem email đã tồn tại trong hệ thống chưa.
    boolean existsByEmail(String email);
}