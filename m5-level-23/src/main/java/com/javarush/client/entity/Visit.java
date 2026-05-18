package com.javarush.client.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "VISITS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "greeting_shown", nullable = false, length = 255)
    private String greetingShown;

    @Column(name = "banner_color", length = 32)
    private String bannerColor;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    public Visit(String greetingShown, String bannerColor) {
        this.greetingShown = greetingShown;
        this.bannerColor = bannerColor;
        this.visitedAt = LocalDateTime.now();
    }
}
