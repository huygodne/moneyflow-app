package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.User;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> findAllCategoriesByUser();
    Optional<Category> getCategoryByIdAndUser(Long id, User user);
    Optional<Category> getCategoryByNameAndUser(String name, User user);
    Category saveCategory(Category category);
    void deleteCategory(Long id);
}