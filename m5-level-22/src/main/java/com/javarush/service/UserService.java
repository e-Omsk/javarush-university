package com.javarush.service;

import com.javarush.entity.User;
import com.javarush.exception.BusinessException;
import com.javarush.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateUserEmails(Long id1, Long id2, String email1, String email2) {
        User user1 = userRepository.findById(id1)
                .orElseThrow(() -> new RuntimeException("User not found: " + id1));
        User user2 = userRepository.findById(id2)
                .orElseThrow(() -> new RuntimeException("User not found: " + id2));

        user1.setEmail(email1);
        user2.setEmail(email2);

        userRepository.save(user1);
        userRepository.save(user2);
    }

    @Transactional
    public void updateUserEmailWithChecked(Long id, String newEmail) throws BusinessException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setEmail(newEmail);
        userRepository.save(user);

        throw new BusinessException("Business error after update");
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

}
