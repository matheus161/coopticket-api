package br.com.coopticket.infra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public Map<String, Object> health() {
        Integer dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of(
            "status", "UP",
            "service", "coopticket-api",
            "database", dbCheck != null && dbCheck == 1 ? "connected" : "error"
        );
    }
}
