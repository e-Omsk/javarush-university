package com.javarush.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO, который мы шлём во внешний Notification Service.
 * Сериализуется в JSON, поэтому имена полей фиксированы – их же видит контракт Pact.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private String email;
    private String userName;

}
