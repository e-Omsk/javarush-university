package com.javarush.client.controller;

import com.javarush.client.entity.Visit;
import com.javarush.client.repository.VisitRepository;
import com.javarush.client.service.GreetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GreetingController {

    private final GreetingService greetingService;
    private final VisitRepository visitRepository;

    @GetMapping("/")
    public String index() {
        return "greeting-service is up. См. /greeting, /config, /visits";
    }

    /**
     * Возвращает приветствие из конфигурации и фиксирует факт визита в БД.
     * Если в конфиге включён флаг show-history — добавляет последние 10 визитов.
     */
    @GetMapping("/greeting")
    public Map<String, Object> greeting() {
        String message = greetingService.getGreeting();
        String color = greetingService.getBannerColor();

        visitRepository.save(new Visit(message, color));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("greeting", message);
        response.put("bannerColor", color);

        if (greetingService.isShowHistory()) {
            response.put("recent", visitRepository.findTop10ByOrderByVisitedAtDesc());
        }
        return response;
    }

    /** Снимок текущих значений конфигурации (то, что сейчас в бине). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("app.greeting", greetingService.getGreeting());
        snapshot.put("app.banner-color", greetingService.getBannerColor());
        snapshot.put("app.feature.show-history", greetingService.isShowHistory());
        return snapshot;
    }

    @GetMapping("/visits")
    public List<Visit> visits() {
        return visitRepository.findAll();
    }
}
