package com.javarush.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * Сервис, чьи поля приходят из Config Server'а.
 *
 * @RefreshScope — бин пересоздаётся при POST /actuator/refresh,
 * после чего новые значения @Value подхватятся. Без @RefreshScope
 * @Value-поля «замораживаются» на момент старта приложения.
 */
@Service
@RefreshScope
public class GreetingService {

    @Value("${app.greeting:Hello}")
    private String greeting;

    @Value("${app.banner-color:gray}")
    private String bannerColor;

    @Value("${app.feature.show-history:false}")
    private boolean showHistory;

    public String getGreeting() {
        return greeting;
    }

    public String getBannerColor() {
        return bannerColor;
    }

    public boolean isShowHistory() {
        return showHistory;
    }
}
