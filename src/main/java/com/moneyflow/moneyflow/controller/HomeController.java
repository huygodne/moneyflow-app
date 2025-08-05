package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.TransactionService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final UserService userService;

    @Autowired
    public HomeController(TransactionService transactionService, AccountService accountService, UserService userService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy dữ liệu thống kê
        BigDecimal totalIncome = transactionService.getTotalIncome(currentUser);
        BigDecimal totalExpense = transactionService.getTotalExpense(currentUser);
        BigDecimal totalBalance = accountService.getTotalBalance(currentUser);

        // Lấy dữ liệu chi tiêu 7 ngày qua
        Map<LocalDate, BigDecimal> dailyExpenses = transactionService.getDailyExpensesLast7Days(currentUser);

        // Chuẩn bị labels và data cho biểu đồ
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartData = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = date.plusDays(i);
            chartLabels.add(currentDate.toString());
            chartData.add(dailyExpenses.getOrDefault(currentDate, BigDecimal.ZERO));
        }

        // Thêm dữ liệu vào model
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("chartLabels", String.join(",", chartLabels));
        model.addAttribute("chartData", chartData.stream().map(BigDecimal::toString).collect(Collectors.joining(",")));
        System.out.println("chartLabels: " + String.join(",", chartLabels));
        System.out.println("chartData: " + chartData.stream().map(BigDecimal::toString).collect(Collectors.joining(",")));
        // Thêm dữ liệu giao dịch gần đây (từ nội dung cũ)
        model.addAttribute("recentTransactions", transactionService.getRecentTransactions(currentUser, 5));

        return "index";
    }
}