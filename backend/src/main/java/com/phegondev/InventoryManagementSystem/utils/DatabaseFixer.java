package com.phegondev.InventoryManagementSystem.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseFixer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixCheckConstraints() {
        log.info("Checking and fixing database constraints for ANALYST role...");
        try {
            // Drop naming-dependent constraint if it exists. 
            // In PostgreSQL, Hibernate often creates it as 'users_role_check'
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            
            // Re-add the constraint with ANALYST included
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'PROCUREMENT_OFFICER', 'WAREHOUSE_MANAGER', 'STAFF', 'ANALYST'))");
            
            log.info("Successfully updated users_role_check constraint to include ANALYST.");
            
            // Also ensure the transactions status column is VARCHAR if Hibernate failed to revert it
            // This is a safety measure for the 'bytea' cast error
            jdbcTemplate.execute("ALTER TABLE transactions ALTER COLUMN status TYPE VARCHAR(255) USING status::varchar");
            log.info("Confirmed transactions.status is VARCHAR.");

        } catch (Exception e) {
            log.warn("Could not update database constraints automatically: {}. This may be normal if the constraint name differs.", e.getMessage());
        }
    }
}
