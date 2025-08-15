package com.moneyflow.moneyflow.service.impl;

import com.moneyflow.moneyflow.dto.UserUpdateDTO;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.repository.UserRepository;
import com.moneyflow.moneyflow.security.CustomUserDetails;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Primary
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public User registerUser(User user) {
        validateUniqueUsername(user.getUsername());
        validateUniqueEmail(user.getEmail());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(user.getUsername());
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        validateUniqueUsername(userDetails.getUsername(), id);
        validateUniqueEmail(userDetails.getEmail(), id);
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        String username = principal instanceof CustomUserDetails
                ? ((CustomUserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    @Transactional
    public User updateUserProfile(User currentUser, UserUpdateDTO userUpdateDTO) {
        validateUniqueEmail(userUpdateDTO.getEmail(), currentUser.getId());
        currentUser.setEmail(userUpdateDTO.getEmail());
        currentUser.setFullName(userUpdateDTO.getFullName());
        return userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect.");
        }
        if (passwordEncoder.matches(newPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    private void validateUniqueUsername(String username, Long excludeId) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent() && (excludeId == null || !existingUser.get().getId().equals(excludeId))) {
            throw new IllegalArgumentException("Username already exists!");
        }
    }

    private void validateUniqueUsername(String username) {
        validateUniqueUsername(username, null);
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && (excludeId == null || !existingUser.get().getId().equals(excludeId))) {
            throw new IllegalArgumentException("Email already exists!");
        }
    }

    private void validateUniqueEmail(String email) {
        validateUniqueEmail(email, null);
    }
}