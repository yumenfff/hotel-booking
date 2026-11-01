package com.hotelbooking.booking.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotel")
public record HotelClientProperties(
    String baseUrl, Duration connectTimeout, Duration readTimeout) {}
