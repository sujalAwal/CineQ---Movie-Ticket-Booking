package com.awal.cineq.config.service;

public interface BackendSettingsService {

    boolean getBooleanValue(String slug);

    String getStringValue(String slug);

    Object getValue(String slug);
}
