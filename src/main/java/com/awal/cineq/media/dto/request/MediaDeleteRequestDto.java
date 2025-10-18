package com.awal.cineq.media.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;


@Data
public class MediaDeleteRequestDto {

    @NotNull(message = "Media IDs are required")
    public List<UUID> mediaIds;
}
