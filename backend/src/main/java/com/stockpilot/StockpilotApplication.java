package com.stockpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Entry point. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan,
 * so Spring scans com.stockpilot and every package under it for beans.
 */
@SpringBootApplication
public class StockpilotApplication {

    public static void main(String[] args) {
        // Servers should run in UTC; the UI converts to local time for display.
        // Also avoids Windows reporting the legacy zone "Asia/Calcutta", which Postgres rejects.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StockpilotApplication.class, args);
    }
}
