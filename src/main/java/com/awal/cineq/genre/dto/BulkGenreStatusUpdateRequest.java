package com.awal.cineq.genre.dto;

import java.util.List;
import java.util.UUID;

public class BulkGenreStatusUpdateRequest {
    private List<UUID> ids;
    // Optionally, you can add a boolean enabled field if you want a single endpoint for both actions
    // private boolean enabled;

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }
}

