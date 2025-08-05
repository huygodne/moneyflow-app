package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.service.CategoryService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*; // Đảm bảo có @DeleteMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @Autowired
    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    // Hiển thị danh sách các danh mục của người dùng hiện tại
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listCategories(Model model, @ModelAttribute("message") String message, @ModelAttribute("errorMessage") String errorMessage) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        List<Category> categories = categoryService.findAllCategoriesByUser();
        model.addAttribute("categories", categories);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        model.addAttribute("currentPage", "categories");
        return "categories/list";
    }

    // Hiển thị form thêm danh mục mới
    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String showNewCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("currentPage", "categories");
        return "categories/form";
    }

    // Xử lý submit form thêm danh mục
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String createCategory(@Valid @ModelAttribute("category") Category category,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            return "categories/form";
        }
        try {
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("message", "Danh mục đã được thêm thành công!");
            return "redirect:/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "categories/form";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Hiển thị form sửa danh mục
    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String showEditCategoryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<Category> categoryOptional = categoryService.getCategoryByIdAndUser(id, currentUser);
        if (categoryOptional.isPresent()) {
            model.addAttribute("category", categoryOptional.get());
            model.addAttribute("currentPage", "categories");
            return "categories/form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Danh mục không tìm thấy hoặc bạn không có quyền chỉnh sửa.");
            return "redirect:/categories";
        }
    }

    // Xử lý submit form sửa danh mục
    @PostMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") Category categoryDetails,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            return "categories/form";
        }
        try {
            categoryDetails.setId(id);
            categoryService.saveCategory(categoryDetails);
            redirectAttributes.addFlashAttribute("message", "Danh mục đã được cập nhật thành công!");
            return "redirect:/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "categories/form";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Xử lý xóa danh mục
    // Đã sửa từ @PostMapping sang @DeleteMapping để đúng chuẩn RESTful
    @DeleteMapping("/delete/{id}") // <--- Đã sửa ở đây
    @PreAuthorize("isAuthenticated()")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("message", "Danh mục đã được xóa thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/categories";
    }
}