package com.awal.cineq.media.model;

import jakarta.persistence.*;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import org.hibernate.annotations.Filter;
    import org.hibernate.annotations.FilterDef;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medias")
@Data
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    private String fileName;
    private String url;
    @Enumerated(EnumType.STRING)
    private MediaType type; // e.g., image, video
    private boolean isActive;
    @Column(name = "parent_id", nullable = true)
    private UUID parentId; // e.g., movie or series ID
    @Column(name = "file_path", nullable = false)
    private  String filePath;
    @Column(name = "file_id", nullable = true)
    private  String fileUuid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

