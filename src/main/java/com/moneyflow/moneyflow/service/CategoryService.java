package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    public List<Category> findAllCategoriesByUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        return categoryRepository.findByUserOrUserIsNull(currentUser);
    }

    public Optional<Category> getCategoryByIdAndUser(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .or(() -> categoryRepository.findByIdAndUserIsNull(id));
    }

    public Optional<Category> getCategoryByNameAndUser(String name, User user) {
        return categoryRepository.findByNameAndUser(name, user);
    }

    @Transactional
    public Category saveCategory(Category category) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        category.setUser(currentUser);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        Optional<Category> categoryOptional = categoryRepository.findByIdAndUser(id, currentUser)
                .or(() -> categoryRepository.findByIdAndUserIsNull(id));
        if (categoryOptional.isPresent()) {
            if (!categoryOptional.get().getTransactions().isEmpty()) {
                throw new IllegalStateException("Không thể xóa danh mục đã có giao dịch.");
            }
            categoryRepository.delete(categoryOptional.get());
        } else {
            throw new IllegalArgumentException("Danh mục không tìm thấy hoặc bạn không có quyền xóa.");
        }
    }
}