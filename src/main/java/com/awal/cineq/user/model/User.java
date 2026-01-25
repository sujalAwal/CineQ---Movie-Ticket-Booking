package com.awal.cineq.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "users")
@CompoundIndexes({
    @CompoundIndex(name = "email_active_idx", def = "{'email': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    private String id;  // MongoDB uses String for ObjectId

    @Field("email")
    private String email;  // Uniqueness enforced by compound index (email, deletedAt)

    @Field("password")
    private String password;
    
    @Field("phone_number")
    private String phoneNumber;
    
    @Field("name")
    private String name;

    /**
     * Reference to the roles collection document ID.
     * Stores the ObjectId of the role document (e.g., "69765bae092751d1429dcbf0").
     * This is a MongoDB-style reference instead of embedding the role data.
     */
    @Field("role_id")
    @Indexed
    private String roleId;

    @Field("is_active")
    private Boolean isActive = true;
    
    @Field("created_at")
    @CreatedDate  // Spring Data MongoDB auto-populates on insert
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    @LastModifiedDate  // Spring Data MongoDB auto-populates on update
    private LocalDateTime updatedAt;
    
    @Field("deleted_at")
    private LocalDateTime deletedAt;
    
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
}