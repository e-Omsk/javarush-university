package com.javarush.controller;

import com.javarush.entity.User;
import com.javarush.exception.BusinessException;
import com.javarush.service.AsyncWelcomeProcessor;
import com.javarush.service.RegistrationService;
import com.javarush.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final UserService userService;
    private final RegistrationService registrationService;
    private final AsyncWelcomeProcessor asyncWelcomeProcessor;

    @GetMapping("/")
    public String sayHello() {
        return "Hello World!";
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/users/update-emails")
    public ResponseEntity<String> updateEmails(@RequestParam Long id1,
                                               @RequestParam Long id2,
                                               @RequestParam String email1,
                                               @RequestParam String email2) {
        userService.updateUserEmails(id1, id2, email1, email2);
        return ResponseEntity.ok("Emails updated successfully");
    }

    @PostMapping("/users/{id}/email")
    public ResponseEntity<String> updateEmailWithChecked(@PathVariable Long id, @RequestParam String email) {
        try {
            userService.updateUserEmailWithChecked(id, email);
            return ResponseEntity.ok("Email updated successfully");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String name, @RequestParam String email) {
        try {
            User user = registrationService.registerUser(name, email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/async-welcome")
    public ResponseEntity<String> asyncWelcome(@RequestParam String email) {
        asyncWelcomeProcessor.process(email);
        return ResponseEntity.accepted().body("Accepted");
    }

}
