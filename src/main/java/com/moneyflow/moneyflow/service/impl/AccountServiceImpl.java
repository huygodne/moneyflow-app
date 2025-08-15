package com.moneyflow.moneyflow.service.impl;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.AccountRepository;
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountServiceImpl(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Override
    public List<Account> findAllAccountsForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        return accountRepository.findByUser(currentUser);
    }

    @Override
    public Optional<Account> getAccountByIdAndUser(Long id, User user) {
        return accountRepository.findByIdAndUser(id, user);
    }

    @Override
    public Optional<Account> getAccountByNameAndUser(String name, User user) {
        return accountRepository.findByNameAndUser(name, user);
    }

    @Override
    public List<Account> getAccountsByUser(User user) {
        validateUser(user);
        return accountRepository.findByUser(user);
    }

    @Override
    @Transactional
    public Account saveAccount(Account account) {
        User currentUser = getAuthenticatedUser();
        if (account.getId() == null) {
            account.setUser(currentUser);
            if (account.getBalance() == null) {
                account.setBalance(BigDecimal.ZERO);
            }
            return accountRepository.save(account);
        }
        return updateExistingAccount(account, currentUser);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        User currentUser = getAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Account not found or you do not have permission to delete."));
        accountRepository.delete(account);
    }

    @Override
    @Transactional
    public void updateAccountBalance(Account account, BigDecimal amount, boolean isIncome) {
        User currentUser = getAuthenticatedUser();
        Account managedAccount = accountRepository.findByIdAndUser(account.getId(), currentUser)
                .orElseThrow(() -> new IllegalStateException("Account not found or you do not have permission to update."));
        updateBalance(managedAccount, amount, isIncome);
        accountRepository.save(managedAccount);
    }

    @Override
    public BigDecimal getTotalBalance(User user) {
        validateUser(user);
        return accountRepository.findByUser(user)
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getAuthenticatedUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated.");
        }
        return currentUser;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalStateException("User not authenticated.");
        }
    }

    private Account updateExistingAccount(Account account, User user) {
        Account existingAccount = accountRepository.findByIdAndUser(account.getId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Account not found or you do not have permission to edit."));
        existingAccount.setName(account.getName());
        existingAccount.setCurrency(account.getCurrency());
        return accountRepository.save(existingAccount);
    }

    private void updateBalance(Account account, BigDecimal amount, boolean isIncome) {
        if (isIncome) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }
}