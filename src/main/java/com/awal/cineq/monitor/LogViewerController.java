package com.awal.cineq.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * LOG VIEWER API - For debugging production logs
 *
 * Endpoints:
 * - GET /api/logs/tail?lines=50&filter=error
 * - GET /api/logs/search?keyword=slow&limit=100
 * - GET /api/logs/latest-errors?limit=20
 *
 * TODO: Remove this controller in production after debugging is complete
 */
@Slf4j
@RestController
public class LogViewerController {

    @Value("${logging.file.name:application.log}")
    private String logFileName;

    /**
     * Get last N lines of log file (like tail command)
     * GET /api/logs/tail?lines=50&filter=ERROR
     */
    @GetMapping("/logs/tail")
    public ResponseEntity<Map<String, Object>> tailLogs(
            @RequestParam(defaultValue = "50") int lines,
            @RequestParam(required = false) String filter) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("log_file", logFileName);
        response.put("requested_lines", lines);
        response.put("filter", filter != null ? filter : "NONE");

        try {
            File logFile = new File(logFileName);

            if (!logFile.exists()) {
                response.put("status", "ERROR");
                response.put("message", "Log file not found: " + logFileName);
                return ResponseEntity.ok(response);
            }

            response.put("log_file_size_mb", String.format("%.2f", logFile.length() / 1024.0 / 1024.0));

            List<String> logLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                List<String> allLines = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }

                // Get last N lines
                int startIdx = Math.max(0, allLines.size() - lines);
                for (int i = startIdx; i < allLines.size(); i++) {
                    String logLine = allLines.get(i);

                    // Apply filter if provided
                    if (filter == null || logLine.contains(filter)) {
                        logLines.add(logLine);
                    }
                }
            }

            response.put("status", "SUCCESS");
            response.put("lines_returned", logLines.size());
            response.put("logs", logLines);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Search logs for specific keyword
     * GET /api/logs/search?keyword=SLOW&limit=100
     */
    @GetMapping("/logs/search")
    public ResponseEntity<Map<String, Object>> searchLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "100") int limit) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("search_keyword", keyword);
        response.put("limit", limit);

        try {
            File logFile = new File(logFileName);

            if (!logFile.exists()) {
                response.put("status", "ERROR");
                response.put("message", "Log file not found: " + logFileName);
                return ResponseEntity.ok(response);
            }

            List<String> matchingLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null && matchingLines.size() < limit) {
                    if (line.toUpperCase().contains(keyword.toUpperCase())) {
                        matchingLines.add(line);
                    }
                }
            }

            response.put("status", "SUCCESS");
            response.put("matches_found", matchingLines.size());
            response.put("logs", matchingLines);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get latest ERROR and WARN logs
     * GET /api/logs/latest-errors?limit=20
     */
    @GetMapping("/logs/latest-errors")
    public ResponseEntity<Map<String, Object>> getLatestErrors(
            @RequestParam(defaultValue = "20") int limit) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        try {
            File logFile = new File(logFileName);

            if (!logFile.exists()) {
                response.put("status", "ERROR");
                response.put("message", "Log file not found: " + logFileName);
                return ResponseEntity.ok(response);
            }

            List<String> errorLogs = new ArrayList<>();
            List<String> warnLogs = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ERROR") && errorLogs.size() < limit) {
                        errorLogs.add(line);
                    } else if (line.contains("WARN") && warnLogs.size() < limit) {
                        warnLogs.add(line);
                    }
                }
            }

            // Reverse to get latest first
            Collections.reverse(errorLogs);
            Collections.reverse(warnLogs);

            response.put("status", "SUCCESS");
            response.put("errors", errorLogs);
            response.put("warnings", warnLogs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get logs related to MongoDB issues
     * GET /api/logs/mongodb-issues
     */
    @GetMapping("/logs/mongodb-issues")
    public ResponseEntity<Map<String, Object>> getMongoDBIssues() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        try {
            File logFile = new File(logFileName);

            if (!logFile.exists()) {
                response.put("status", "ERROR");
                response.put("message", "Log file not found: " + logFileName);
                return ResponseEntity.ok(response);
            }

            List<String> mongoIssues = new ArrayList<>();
            List<String> connectionIssues = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String upperLine = line.toUpperCase();

                    if (upperLine.contains("MONGODB") || upperLine.contains("MONGO")) {
                        mongoIssues.add(line);
                    }

                    if (upperLine.contains("CONNECTION") || upperLine.contains("TIMEOUT")
                            || upperLine.contains("WAITING FOR POOL")) {
                        connectionIssues.add(line);
                    }
                }
            }

            response.put("status", "SUCCESS");
            response.put("mongodb_related_logs", mongoIssues.size() > 0 ? mongoIssues : "No MongoDB logs found");
            response.put("connection_issues_found", connectionIssues.size());
            response.put("connection_logs", connectionIssues.size() > 0 ? connectionIssues : "No connection issues found");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get slow request logs
     * GET /api/logs/slow-requests?min_duration_ms=1000
     */
    @GetMapping("/logs/slow-requests")
    public ResponseEntity<Map<String, Object>> getSlowRequests(
            @RequestParam(defaultValue = "1000") int minDurationMs) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("threshold_ms", minDurationMs);

        try {
            File logFile = new File(logFileName);

            if (!logFile.exists()) {
                response.put("status", "ERROR");
                response.put("message", "Log file not found: " + logFileName);
                return ResponseEntity.ok(response);
            }

            List<String> slowRequests = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toUpperCase().contains("SLOW_REQUEST") ||
                            line.toUpperCase().contains("TOOK") && line.contains("ms")) {

                        // Try to extract duration
                        try {
                            if (line.contains("took") || line.contains("TOOK")) {
                                slowRequests.add(line);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            response.put("status", "SUCCESS");
            response.put("slow_requests_found", slowRequests.size());
            response.put("logs", slowRequests.size() > 0 ? slowRequests : "No slow requests found");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
