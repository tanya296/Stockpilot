package com.stockpilot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public "what is running?" endpoint. The frontend shows it on the login page and sidebar,
 * which makes the CI/CD demo visible: change the version or note, push, and the new values
 * appear once Kubernetes has rolled out the new image.
 */
@RestController
@RequestMapping("/api/info")
public class InfoController {

    public record AppInfo(String app, String version, String commit, String releaseNote) {}

    private final AppInfo info;

    public InfoController(@Value("${app.version:dev}") String version,
                          @Value("${app.commit:local}") String commit,
                          @Value("${app.release-note:}") String releaseNote) {
        this.info = new AppInfo("StockPilot", version, commit, releaseNote);
    }

    @GetMapping
    public AppInfo info() {
        return info;
    }
}
