package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.User; // Đảm bảo import User
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.UserService; // Import UserService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService; // Tiêm UserService

    @Autowired
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    // Hiển thị danh sách các tài khoản/ví của người dùng hiện tại
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Yêu cầu người dùng đã đăng nhập
    public String listAccounts(Model model, @ModelAttribute("message") String message, @ModelAttribute("errorMessage") String errorMessage) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            // Trường hợp không có người dùng đăng nhập, chuyển hướng về trang đăng nhập
            // hoặc hiển thị lỗi
            return "redirect:/login";
        }
        // Gọi phương thức đúng: findAllAccountsForCurrentUser()
        List<Account> accounts = accountService.findAllAccountsForCurrentUser();
        model.addAttribute("accounts", accounts);

        // Đảm bảo thông báo hiển thị trên trang
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        model.addAttribute("currentPage", "accounts");
        return "accounts/list";
    }

    // Hiển thị form thêm tài khoản/ví mới
    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String showNewAccountForm(Model model) {
        // Không cần lấy currentUser ở đây vì saveAccount sẽ tự lấy
        model.addAttribute("account", new Account());
        model.addAttribute("currentPage", "accounts");
        return "accounts/form";
    }

    // Xử lý submit form thêm tài khoản/ví
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String createAccount(@Valid @ModelAttribute("account") Account account,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            return "accounts/form";
        }
        try {
            // saveAccount sẽ tự gán người dùng hiện tại
            accountService.saveAccount(account);
            redirectAttributes.addFlashAttribute("message", "Ví đã được thêm thành công!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi trùng tên ví hoặc các lỗi logic khác từ service
            model.addAttribute("errorMessage", e.getMessage());
            return "accounts/form";
        } catch (IllegalStateException e) {
            // Xử lý lỗi nếu người dùng không được xác thực khi gọi service
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Hiển thị form sửa tài khoản/ví
    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Gọi phương thức getAccountByIdAndUser để kiểm tra quyền sở hữu
        Optional<Account> accountOptional = accountService.getAccountByIdAndUser(id, currentUser);
        if (accountOptional.isPresent()) {
            model.addAttribute("account", accountOptional.get());
            model.addAttribute("currentPage", "accounts");
            return "accounts/form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ví không tìm thấy hoặc bạn không có quyền chỉnh sửa.");
            return "redirect:/accounts";
        }
    }

    // Xử lý submit form sửa tài khoản/ví
    @PostMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateAccount(@PathVariable Long id,
                                @Valid @ModelAttribute("account") Account accountDetails,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Có lỗi trong dữ liệu nhập vào.");
            return "accounts/form";
        }
        try {
            // Gán ID cho accountDetails để service biết đây là thao tác cập nhật
            accountDetails.setId(id);
            // saveAccount sẽ xử lý cập nhật và gán người dùng
            accountService.saveAccount(accountDetails);
            redirectAttributes.addFlashAttribute("message", "Ví đã được cập nhật thành công!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "accounts/form";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // Xử lý xóa tài khoản/ví
    @DeleteMapping("/delete/{id}") // <--- THAY ĐỔI Ở ĐÂY
    @PreAuthorize("isAuthenticated()")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Bạn có thể cần lấy người dùng hiện tại để kiểm tra quyền sở hữu trước khi xóa
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }
        try {
            // Đảm bảo service của bạn có phương thức xóa ví dựa trên ID và người dùng
            // Ví dụ: accountService.deleteAccount(id, currentUser);
            accountService.deleteAccount(id); // Nếu service không cần currentUser hoặc kiểm tra quyền bên trong nó
            redirectAttributes.addFlashAttribute("message", "Ví đã được xóa thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/accounts";
    }
}