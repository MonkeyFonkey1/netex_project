package com.netex.addressbook.auth;

import com.netex.addressbook.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ADMIN_EMAIL=  Admin@Test.Example  ",
        "ADMIN_PASSWORD=test-admin-password"
})
@Import(PostgresTestConfiguration.class)
class AdminAccountInitializerTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void serverConfigurationCreatesAnAdminWithAHashedPassword() {
        var row = jdbc.queryForMap("SELECT email, password_hash, role FROM users WHERE email = ?",
                "admin@test.example");
        assertThat(row.get("role")).isEqualTo("ADMIN");
        assertThat(passwordEncoder.matches("test-admin-password", (String) row.get("password_hash"))).isTrue();
    }
}
