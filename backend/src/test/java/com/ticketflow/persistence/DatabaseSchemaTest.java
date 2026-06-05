package com.ticketflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devProfileCreatesCoreTablesInH2() {
        Set<String> tableNames = jdbcTemplate.queryForList(
                        "select table_name from information_schema.tables where lower(table_schema) = 'public'",
                        String.class)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(tableNames)
                .contains("users", "tickets", "ticket_comments", "alerts");
    }
}
