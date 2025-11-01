package com.awal.cineq.client.supabase.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SupabaseConfig {

    @Value("${supabase.api.key}")
    private String apiKey;

    @Value("${supabase.api.url}")
    private String apiUrl;
    @Value("${supabase.api.bucket:cineq}")
    private String bucket;
}
