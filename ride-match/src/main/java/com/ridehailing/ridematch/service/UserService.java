package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.UserRequest;
import com.ridehailing.ridematch.entity.User;
import com.ridehailing.ridematch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(UserRequest request) {
        User user = User.builder()
                .phone(request.getPhone())
                .build();
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
}
