package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    public List<Account> findAllAccountsForCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        return accountRepository.findByUser(currentUser);
    }

    public Optional<Account> getAccountByIdAndUser(Long id, User user) {
        return accountRepository.findByIdAndUser(id, user);
    }

    public Optional<Account> getAccountByNameAndUser(String name, User user) {
        return accountRepository.findByNameAndUser(name, user);
    }

    public List<Account> getAccountsByUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        return accountRepository.findByUser(user);
    }

    @Transactional
    public Account saveAccount(Account account) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }

        if (account.getId() == null) {
            account.setUser(currentUser);
            if (account.getBalance() == null) {
                account.setBalance(BigDecimal.ZERO);
            }
            return accountRepository.save(account);
        } else {
            Optional<Account> existingAccountOptional = accountRepository.findByIdAndUser(account.getId(), currentUser);
            if (existingAccountOptional.isPresent()) {
                Account existingAccount = existingAccountOptional.get();
                existingAccount.setName(account.getName());
                existingAccount.setCurrency(account.getCurrency());
                return accountRepository.save(existingAccount);
            } else {
                throw new IllegalArgumentException("Ví không tìm thấy hoặc bạn không có quyền chỉnh sửa.");
            }
        }
    }

    @Transactional
    public void deleteAccount(Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        Optional<Account> accountOptional = accountRepository.findByIdAndUser(id, currentUser);
        if (accountOptional.isPresent()) {
            accountRepository.delete(accountOptional.get());
        } else {
            throw new IllegalArgumentException("Ví không tìm thấy hoặc bạn không có quyền xóa.");
        }
    }

    @Transactional
    public void updateAccountBalance(Account account, BigDecimal amount, boolean isIncome) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        Account managedAccount = accountRepository.findByIdAndUser(account.getId(), currentUser)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ví để cập nhật số dư hoặc bạn không có quyền."));

        if (isIncome) {
            managedAccount.setBalance(managedAccount.getBalance().add(amount));
        } else {
            managedAccount.setBalance(managedAccount.getBalance().subtract(amount));
        }
        accountRepository.save(managedAccount);
    }

    public BigDecimal getTotalBalance(User user) {
        if (user == null) throw new IllegalStateException("Người dùng chưa được xác thực.");
        return accountRepository.findByUser(user)
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}