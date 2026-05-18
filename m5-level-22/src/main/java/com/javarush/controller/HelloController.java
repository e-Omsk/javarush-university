package com.javarush.controller;

import com.javarush.entity.User;
import com.javarush.exception.BusinessException;
import com.javarush.service.RegistrationService;
import com.javarush.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final UserService userService;
    private final RegistrationService registrationService;

    // app.greeting приходит из application.properties; в docker-compose.yml
    // мы переопределяем её через переменную окружения, чтобы показать механизм.
    @Value("${app.greeting:Hello World!}")
    private String greeting;

    @GetMapping("/")
    public String sayHello() {
        return greeting;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "greeting", greeting,
                "host", System.getenv().getOrDefault("HOSTNAME", "unknown"),
                "java", System.getProperty("java.version")
        );
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
}
