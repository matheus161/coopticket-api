package br.com.coopticket.infra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
