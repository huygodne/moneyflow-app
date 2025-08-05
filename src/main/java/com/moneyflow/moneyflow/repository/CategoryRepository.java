package com.moneyflow.moneyflow.repository;

import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm tất cả các danh mục thuộc về một người dùng cụ thể.
    // Đây sẽ là các danh mục cá nhân của người dùng.
    List<Category> findByUser(User user);

    // Tìm tất cả các danh mục không thuộc về người dùng nào (danh mục hệ thống/chung).
    List<Category> findByUserIsNull();

    // Tìm tất cả danh mục của một người dùng cụ thể và theo loại (INCOME/EXPENSE)
    List<Category> findByUserAndType(User user, TransactionType type);

    // Tìm danh mục hệ thống theo ID và loại
    Optional<Category> findByIdAndUserIsNullAndType(Long id, TransactionType type);

    // Tìm danh mục cá nhân theo ID và User sở hữu
    Optional<Category> findByIdAndUser(Long id, User user);

    List<Category> findByUserOrUserIsNull(User user);
    Optional<Category> findByIdAndUserIsNull(Long id);

    Optional<Category> findByNameAndUser(String name, User user);
}