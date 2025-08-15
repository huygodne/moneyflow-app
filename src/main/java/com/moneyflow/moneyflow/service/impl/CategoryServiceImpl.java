package com.moneyflow.moneyflow.service.impl;

import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.CategoryRepository;
import com.moneyflow.moneyflow.service.CategoryService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Override
    public List<Category> findAllCategoriesByUser() {
        User currentUser = getAuthenticatedUser();
        return categoryRepository.findByUserOrUserIsNull(currentUser);
    }

    @Override
    public Optional<Category> getCategoryByIdAndUser(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .or(() -> categoryRepository.findByIdAndUserIsNull(id));
    }

    @Override
    public Optional<Category> getCategoryByNameAndUser(String name, User user) {
        return categoryRepository.findByNameAndUser(name, user);
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        User currentUser = getAuthenticatedUser();
        category.setUser(currentUser);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        User currentUser = getAuthenticatedUser();
        Optional<Category> categoryOptional = categoryRepository.findByIdAndUser(id, currentUser)
                .or(() -> categoryRepository.findByIdAndUserIsNull(id));
        if (categoryOptional.isPresent()) {
            Category category = categoryOptional.get();
            if (!category.getTransactions().isEmpty()) {
                throw new IllegalStateException("Cannot delete category with associated transactions.");
            }
            categoryRepository.delete(category);
        } else {
            throw new IllegalArgumentException("Category not found or you do not have permission to delete.");
        }
    }

    private User getAuthenticatedUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated.");
        }
        return currentUser;
    }
}