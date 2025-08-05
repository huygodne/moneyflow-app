package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.enums.TransactionType;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.entity.Category; // THÊM DÒNG NÀY
import com.moneyflow.moneyflow.service.TransactionService;
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.CategoryService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, AccountService accountService,
                                 CategoryService categoryService, UserService userService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    // Hiển thị danh sách giao dịch
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listTransactions(Model model, @ModelAttribute("message") String message, @ModelAttribute("errorMessage") String errorMessage) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        List<Transaction> transactions = transactionService.getTransactionsByUser(currentUser);
        model.addAttribute("transactions", transactions);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        model.addAttribute("currentPage", "transactions");
        return "transactions/list";
    }

    // Hiển thị form thêm giao dịch mới
    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String showNewTransactionForm(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(LocalDateTime.now());
        System.out.println("DEBUG: transactionDate khi gửi đến form: " + transaction.getTransactionDate());

        model.addAttribute("transaction", transaction);
        model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
        model.addAttribute("categories", categoryService.findAllCategoriesByUser());
        model.addAttribute("currentPage", "transactions");
        return "transactions/form";
    }

    // Xử lý submit form thêm giao dịch
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String createTransaction(@Valid @ModelAttribute("transaction") Transaction transaction,
                                    BindingResult result,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
            model.addAttribute("categories", categoryService.findAllCategoriesByUser());
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            model.addAttribute("currentPage", "transactions");
            return "transactions/form";
        }

        try {
            Long accountId = transaction.getAccount() != null ? transaction.getAccount().getId() : null;
            Long categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;

            if (accountId == null) {
                throw new IllegalArgumentException("Ví chưa được chọn.");
            }
            if (categoryId == null) {
                throw new IllegalArgumentException("Danh mục chưa được chọn.");
            }

            // Lấy danh mục để xác nhận type
            TransactionType categoryType = categoryService.getCategoryByIdAndUser(categoryId, currentUser)
                    .map(Category::getType)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ."));
            transaction.setType(categoryType); // Gán type từ danh mục

            transaction.setAccount(accountService.getAccountByIdAndUser(accountId, currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("Ví không hợp lệ.")));
            transaction.setCategory(categoryService.getCategoryByIdAndUser(categoryId, currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ.")));

            transactionService.createTransaction(transaction, currentUser);
            redirectAttributes.addFlashAttribute("message", "Giao dịch đã được thêm thành công!");
            return "redirect:/transactions";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
            model.addAttribute("categories", categoryService.findAllCategoriesByUser());
            model.addAttribute("currentPage", "transactions");
            return "transactions/form";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Hiển thị form sửa giao dịch
    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String showEditTransactionForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<Transaction> transactionOptional = transactionService.getTransactionByIdAndUser(id, currentUser);
        if (transactionOptional.isPresent()) {
            model.addAttribute("transaction", transactionOptional.get());
            model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
            model.addAttribute("categories", categoryService.findAllCategoriesByUser());
            model.addAttribute("currentPage", "transactions");
            return "transactions/form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Giao dịch không tìm thấy hoặc bạn không có quyền chỉnh sửa.");
            return "redirect:/transactions";
        }
    }

    // Xử lý submit form sửa giao dịch
    @PostMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateTransaction(@PathVariable Long id,
                                    @Valid @ModelAttribute("transaction") Transaction transactionDetails,
                                    BindingResult result,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
            model.addAttribute("categories", categoryService.findAllCategoriesByUser());
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            model.addAttribute("currentPage", "transactions");
            return "transactions/form";
        }

        try {
            Long accountId = transactionDetails.getAccount() != null ? transactionDetails.getAccount().getId() : null;
            Long categoryId = transactionDetails.getCategory() != null ? transactionDetails.getCategory().getId() : null;

            if (accountId == null) {
                throw new IllegalArgumentException("Ví chưa được chọn.");
            }
            if (categoryId == null) {
                throw new IllegalArgumentException("Danh mục chưa được chọn.");
            }

            // Lấy danh mục để xác nhận type
            TransactionType categoryType = categoryService.getCategoryByIdAndUser(categoryId, currentUser)
                    .map(Category::getType)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ."));
            transactionDetails.setType(categoryType); // Gán type từ danh mục

            transactionDetails.setAccount(accountService.getAccountByIdAndUser(accountId, currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("Ví không hợp lệ.")));
            transactionDetails.setCategory(categoryService.getCategoryByIdAndUser(categoryId, currentUser)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ.")));

            transactionService.updateTransaction(id, transactionDetails, currentUser);
            redirectAttributes.addFlashAttribute("message", "Giao dịch đã được cập nhật thành công!");
            return "redirect:/transactions";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("accounts", accountService.findAllAccountsForCurrentUser());
            model.addAttribute("categories", categoryService.findAllCategoriesByUser());
            model.addAttribute("currentPage", "transactions");
            return "transactions/form";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Xóa giao dịch
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteTransaction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }
        try {
            transactionService.deleteTransaction(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Giao dịch đã được xóa thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/transactions";
    }
}