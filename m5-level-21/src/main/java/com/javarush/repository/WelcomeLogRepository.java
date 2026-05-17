package com.javarush.repository;

import com.javarush.entity.WelcomeLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WelcomeLogRepository extends JpaRepository<WelcomeLogEntry, Long> {

    Optional<WelcomeLogEntry> findByEmail(String email);

}
