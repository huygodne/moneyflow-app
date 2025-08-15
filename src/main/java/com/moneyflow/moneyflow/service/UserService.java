package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.dto.UserUpdateDTO;
import com.moneyflow.moneyflow.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    List<User> getAllUsers();
    Optional<User> getUserById(Long id);
    User registerUser(User user);
    User updateUser(Long id, User userDetails);
    void deleteUser(Long id);
    Optional<User> findByUsername(String username);
    User getCurrentUser();
    User updateUserProfile(User currentUser, UserUpdateDTO userUpdateDTO);
    void changePassword(User currentUser, String oldPassword, String newPassword);
}