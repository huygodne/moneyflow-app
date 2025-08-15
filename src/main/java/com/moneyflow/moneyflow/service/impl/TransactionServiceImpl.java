package com.moneyflow.moneyflow.service.impl;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import com.moneyflow.moneyflow.repository.TransactionRepository;
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.CategoryService;
import com.moneyflow.moneyflow.service.TransactionService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountService accountService,
                                  CategoryService categoryService, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @Override
    public List<Transaction> getTransactionsByUser(User user) {
        validateUser(user);
        return transactionRepository.findByUserOrderByTransactionDateDesc(user);
    }

    @Override
    public Page<Transaction> getTransactionsByUser(User user, Pageable pageable) {
        return transactionRepository.findByUser(user, pageable);
    }

    @Override
    public List<Transaction> getRecentTransactions(User user, int limit) {
        validateUser(user);
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("transactionDate").descending());
        return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageRequest);
    }

    @Override
    public Optional<Transaction> getTransactionByIdAndUser(Long transactionId, User user) {
        if (user == null) {
            return Optional.empty();
        }
        return transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user));
    }

    @Override
    @Transactional
    public Transaction createTransaction(Transaction transaction, User user) {
        validateUser(user);
        transaction.setUser(user);
        Account account = validateAndGetAccount(transaction.getAccount().getId(), user);
        Category category = validateAndGetCategory(transaction.getCategory().getId(), user);
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountService.updateAccountBalance(account, savedTransaction.getAmount(), savedTransaction.getType() == TransactionType.INCOME);
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction updateTransaction(Long transactionId, Transaction transactionDetails, User user) {
        validateUser(user);
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found or you do not have permission."));

        reverseBalanceUpdate(existingTransaction);
        Account newAccount = validateAndGetAccount(transactionDetails.getAccount().getId(), user);
        Category newCategory = validateAndGetCategory(transactionDetails.getCategory().getId(), user);

        updateTransactionDetails(existingTransaction, transactionDetails, newAccount, newCategory);
        Transaction updatedTransaction = transactionRepository.save(existingTransaction);
        accountService.updateAccountBalance(newAccount, updatedTransaction.getAmount(), updatedTransaction.getType() == TransactionType.INCOME);
        return updatedTransaction;
    }

    @Override
    @Transactional
    public void deleteTransaction(Long transactionId, User user) {
        validateUser(user);
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().equals(user))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found or you do not have permission."));

        reverseBalanceUpdate(existingTransaction);
        transactionRepository.delete(existingTransaction);
    }

    @Override
    public BigDecimal getTotalIncome(User user) {
        validateUser(user);
        return transactionRepository.findByUserAndType(user, TransactionType.INCOME)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalExpense(User user) {
        validateUser(user);
        return transactionRepository.findByUserAndType(user, TransactionType.EXPENSE)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<LocalDate, BigDecimal> getDailyExpensesLast7Days(User user) {
        validateUser(user);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Transaction> expenses = transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user, TransactionType.EXPENSE, startDateTime, endDateTime);

        Map<LocalDate, BigDecimal> dailyExpenses = initializeDailyExpensesMap(startDate, endDate);
        expenses.forEach(transaction -> {
            LocalDate date = transaction.getTransactionDate().toLocalDate();
            dailyExpenses.compute(date, (key, value) -> value != null ? value.add(transaction.getAmount()) : transaction.getAmount());
        });

        return dailyExpenses;
    }

    @Override
    public List<Map<String, Object>> getExpensesByCategory() {
        User currentUser = userService.getCurrentUser();
        validateUser(currentUser);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return transactionRepository.findExpensesByCategory(currentUser, sevenDaysAgo);
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalStateException("User not authenticated.");
        }
    }

    private Account validateAndGetAccount(Long accountId, User user) {
        return accountService.getAccountByIdAndUser(accountId, user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid account or not owned by user."));
    }

    private Category validateAndGetCategory(Long categoryId, User user) {
        return categoryService.getCategoryByIdAndUser(categoryId, user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category or not owned by user."));
    }

    private void reverseBalanceUpdate(Transaction transaction) {
        accountService.updateAccountBalance(transaction.getAccount(), transaction.getAmount(),
                transaction.getType() == TransactionType.EXPENSE);
    }

    private void updateTransactionDetails(Transaction existing, Transaction details, Account account, Category category) {
        existing.setAccount(account);
        existing.setCategory(category);
        existing.setAmount(details.getAmount());
        existing.setType(details.getType());
        existing.setDescription(details.getDescription());
        existing.setTransactionDate(details.getTransactionDate());
    }

    private Map<LocalDate, BigDecimal> initializeDailyExpensesMap(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, BigDecimal> dailyExpenses = new TreeMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyExpenses.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }
        return dailyExpenses;
    }
}