package com.gamereleasetracker.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A JPA entity that maps to the database table used for Spring Security's
 * persistent "remember-me" functionality. Schema from
 * <a href="https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html#remember-me-persistent-token">
 *     Spring Security Persistent Token Approach</a>
 */
@Setter
@Getter
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {

    @Id
    @Column(length = 64)
    private String series;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private Instant lastUsed;

}