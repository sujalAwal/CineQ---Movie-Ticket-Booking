package com.awal.cineq.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "user_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed(unique = true)
    @Field("token")
    private String token;

    @Field("token_type")
    private String tokenType = "PASSWORD_SETUP";

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("expires_at")
    private LocalDateTime expiresAt;

    @Field("used_at")
    private LocalDateTime usedAt;

    @Field("is_invalidated")
    private Boolean isInvalidated = false;

    public boolean isValid() {
        return !Boolean.TRUE.equals(isInvalidated)
                && usedAt == null
                && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
