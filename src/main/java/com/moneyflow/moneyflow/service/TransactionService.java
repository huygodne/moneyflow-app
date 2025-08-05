package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import com.moneyflow.moneyflow.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final UserService userService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, AccountService accountService,
                              CategoryService categoryService, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    public List<Transaction> getTransactionsByUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        return transactionRepository.findByUserOrderByTransactionDateDesc(user);
    }

    public List<Transaction> getRecentTransactions(User user, int limit) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("transactionDate").descending());
        return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageRequest);
    }

    public Optional<Transaction> getTransactionByIdAndUser(Long transactionId, User user) {
        if (user == null) {
            return Optional.empty();
        }
        return transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user));
    }

    @Transactional
    public Transaction createTransaction(Transaction transaction, User user) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }

        transaction.setUser(user);

        Account account = accountService.getAccountByIdAndUser(transaction.getAccount().getId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Ví không hợp lệ hoặc không thuộc về bạn."));
        transaction.setAccount(account);

        Category category = categoryService.getCategoryByIdAndUser(transaction.getCategory().getId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ hoặc không thuộc về bạn."));
        transaction.setCategory(category);

        Transaction savedTransaction = transactionRepository.save(transaction);

        accountService.updateAccountBalance(account, savedTransaction.getAmount(), savedTransaction.getType() == TransactionType.INCOME);

        return savedTransaction;
    }

    @Transactional
    public Transaction updateTransaction(Long transactionId, Transaction transactionDetails, User user) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user))
                .orElseThrow(() -> new IllegalArgumentException("Giao dịch không tìm thấy hoặc không thuộc về bạn."));

        Account oldAccount = existingTransaction.getAccount();
        BigDecimal oldAmount = existingTransaction.getAmount();
        TransactionType oldType = existingTransaction.getType();
        accountService.updateAccountBalance(oldAccount, oldAmount, oldType == TransactionType.EXPENSE);

        Account newAccount = accountService.getAccountByIdAndUser(transactionDetails.getAccount().getId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Ví mới không hợp lệ hoặc không thuộc về bạn."));
        Category newCategory = categoryService.getCategoryByIdAndUser(transactionDetails.getCategory().getId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục mới không hợp lệ hoặc không thuộc về bạn."));

        existingTransaction.setAccount(newAccount);
        existingTransaction.setCategory(newCategory);
        existingTransaction.setAmount(transactionDetails.getAmount());
        existingTransaction.setType(transactionDetails.getType());
        existingTransaction.setDescription(transactionDetails.getDescription());
        existingTransaction.setTransactionDate(transactionDetails.getTransactionDate());

        Transaction updatedTransaction = transactionRepository.save(existingTransaction);

        accountService.updateAccountBalance(newAccount, updatedTransaction.getAmount(), updatedTransaction.getType() == TransactionType.INCOME);

        return updatedTransaction;
    }

    @Transactional
    public void deleteTransaction(Long transactionId, User user) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user))
                .orElseThrow(() -> new IllegalArgumentException("Giao dịch không tìm thấy hoặc không thuộc về bạn."));

        Account account = existingTransaction.getAccount();
        BigDecimal amount = existingTransaction.getAmount();
        TransactionType type = existingTransaction.getType();

        accountService.updateAccountBalance(account, amount, type == TransactionType.EXPENSE);

        transactionRepository.delete(existingTransaction);
    }

//    public List<Transaction> getTransactionsInDateRange(User user, LocalDate startDate, LocalDate endDate) {
//        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
//        return transactionRepository.findByUserAndTransactionDateBetween(user, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
//    }
//
//    public List<Transaction> getTransactionsByAccount(User user, Long accountId) {
//        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
//        Account account = accountService.getAccountByIdAndUser(accountId, user)
//                .orElseThrow(() -> new IllegalArgumentException("Ví không hợp lệ hoặc không thuộc về bạn."));
//        return transactionRepository.findByUserAndAccount(user, account);
//    }
//
//    public List<Transaction> getTransactionsByCategory(User user, Long categoryId) {
//        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
//        Category category = categoryService.getCategoryByIdAndUser(categoryId, user)
//                .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ hoặc không thuộc về bạn."));
//        return transactionRepository.findByUserAndCategory(user, category);
//    }
//
//    public List<Transaction> getTransactionsByType(User user, TransactionType type) {
//        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
//        return transactionRepository.findByUserAndType(user, type);
//    }

    public BigDecimal getTotalIncome(User user) {
        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
        return transactionRepository.findByUserAndType(user, TransactionType.INCOME)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExpense(User user) {
        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
        return transactionRepository.findByUserAndType(user, TransactionType.EXPENSE)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<LocalDate, BigDecimal> getDailyExpensesLast7Days(User user) {
        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 7 ngày bao gồm hôm nay

        // Chuyển đổi LocalDate thành LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Transaction> expenses = transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user, TransactionType.EXPENSE, startDateTime, endDateTime);

        System.out.println("Found " + expenses.size() + " expense transactions in last 7 days.");

        // Tạo bản đồ với tất cả 7 ngày
        Map<LocalDate, BigDecimal> dailyExpenses = new TreeMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyExpenses.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }

        // Cập nhật tổng chi tiêu cho các ngày có giao dịch
        expenses.forEach(transaction -> {
            LocalDate date = transaction.getTransactionDate().toLocalDate();
            if (dailyExpenses.containsKey(date)) {
                dailyExpenses.compute(date, (key, value) -> value != null ? value.add(transaction.getAmount()) : transaction.getAmount());
            }
        });

        return dailyExpenses;
    }
}