package com.ticketing.system.service;

import com.ticketing.system.dto.UserResponse;
import com.ticketing.system.exception.ApiException;
import com.ticketing.system.model.Role;
import com.ticketing.system.model.User;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    public List<UserResponse> getAllAgents() {
        return userRepository.findByRole(Role.AGENT).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getCurrentUser(String userId) {
        User user = getUserById(userId);
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
