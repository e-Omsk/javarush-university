package com.javarush.client.repository;

import com.javarush.client.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findTop10ByOrderByVisitedAtDesc();
}
