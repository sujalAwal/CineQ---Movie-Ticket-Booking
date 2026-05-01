package com.awal.cineq.monitor;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * DIAGNOSTIC API - For debugging production issues
 *
 * Endpoints:
 * - GET /api/diagnostics/health-detailed
 * - GET /api/diagnostics/mongodb
 * - GET /api/diagnostics/system
 *
 * TODO: Remove this controller in production after debugging is complete
 */
@Slf4j
@RestController
public class DiagnosticsController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private MongoProperties mongoProperties;

    @Value("${spring.data.mongodb.uri:}")
    private String mongodbUri;

    /**
     * Detailed health check with MongoDB connectivity test
     * GET /api/diagnostics/health-detailed
     */
    @GetMapping("/diagnostics/health-detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("service_status", "UP");

        // Test MongoDB connectivity
        Map<String, Object> mongoStatus = testMongoDBConnectivity();
        response.put("mongodb", mongoStatus);

        // JVM Status
        response.put("jvm", getJVMStatus());

        // System Status
        response.put("system", getSystemStatus());

        long duration = System.currentTimeMillis() - startTime;
        response.put("check_duration_ms", duration);

        if (duration > 5000) {
            response.put("⚠️_warning", "Health check took " + duration + "ms (> 5 seconds)");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * MongoDB specific diagnostics
     * GET /api/diagnostics/mongodb
     */
    @GetMapping("/diagnostics/mongodb")
    public ResponseEntity<Map<String, Object>> getMongoDBDiagnostics() {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        // Current Configuration
        Map<String, Object> config = new HashMap<>();
        config.put("host", mongoProperties.getHost());
        config.put("port", mongoProperties.getPort());
        config.put("database", mongoProperties.getDatabase());
        config.put("uri_masked", maskUri(mongodbUri));
        config.put("connection_string_info", extractConnectionInfo());
        response.put("configuration", config);

        // Connectivity Test
        response.put("connectivity", testMongoDBConnectivity());

        // Connection Pool Status
        response.put("connection_pool", getConnectionPoolStatus());

        // Database Operations Test
        response.put("database_operations", testDatabaseOperations());

        long duration = System.currentTimeMillis() - startTime;
        response.put("total_check_duration_ms", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * System and JVM diagnostics
     * GET /api/diagnostics/system
     */
    @GetMapping("/diagnostics/system")
    public ResponseEntity<Map<String, Object>> getSystemDiagnostics() {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        // JVM
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("heap_used_mb", memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
        jvm.put("heap_max_mb", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
        jvm.put("heap_percent", String.format("%.2f%%",
            (double) memoryBean.getHeapMemoryUsage().getUsed() /
            memoryBean.getHeapMemoryUsage().getMax() * 100));
        jvm.put("non_heap_used_mb", memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
        response.put("jvm", jvm);

        // Threads
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new HashMap<>();
        threads.put("active_count", threadBean.getThreadCount());
        threads.put("peak_count", threadBean.getPeakThreadCount());
        threads.put("daemon_count", threadBean.getDaemonThreadCount());
        response.put("threads", threads);

        // System
        response.put("system", getSystemStatus());

        // Runtime
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> runtimeInfo = new HashMap<>();
        runtimeInfo.put("available_processors", runtime.availableProcessors());
        runtimeInfo.put("max_memory_mb", runtime.maxMemory() / 1024 / 1024);
        runtimeInfo.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
        runtimeInfo.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
        response.put("runtime", runtimeInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * Test MongoDB connectivity with timing
     */
    private Map<String, Object> testMongoDBConnectivity() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Test 1: Ping via Mongo client
            long pingStart = System.currentTimeMillis();
            mongoClient.getDatabase("admin").runCommand(
                new org.bson.Document("ping", 1)
            );
            long pingDuration = System.currentTimeMillis() - pingStart;

            result.put("status", "✓ CONNECTED");
            result.put("ping_time_ms", pingDuration);

            if (pingDuration > 2000) {
                result.put("⚠️_warning", "Ping took " + pingDuration + "ms (slow)");
            }

            return result;
        } catch (Exception e) {
            result.put("status", "✗ FAILED");
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
            long duration = System.currentTimeMillis() - startTime;
            result.put("timeout_duration_ms", duration);
            return result;
        }
    }

    /**
     * Get connection pool status
     */
    private Map<String, Object> getConnectionPoolStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get cluster description
            var clusterDescription = mongoClient.getClusterDescription();
            result.put("cluster_type", clusterDescription.getType());
            result.put("server_count", clusterDescription.getServerDescriptions().size());
            result.put("all_servers_ok", clusterDescription.getServerDescriptions().stream()
                .allMatch(sd -> sd.isOk()));

            result.put("status", "✓ ACTIVE");
            return result;
        } catch (Exception e) {
            result.put("status", "✗ ERROR");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Test actual database operations
     */
    private Map<String, Object> testDatabaseOperations() {
        Map<String, Object> result = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();

            // Try to list collections
            var collections = mongoTemplate.getDb().listCollectionNames();
            int collectionCount = 0;
            for (var col : collections) {
                collectionCount++;
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("status", "✓ SUCCESS");
            result.put("collections_found", collectionCount);
            result.put("operation_time_ms", duration);
            return result;
        } catch (Exception e) {
            result.put("status", "✗ FAILED");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Get JVM status
     */
    private Map<String, Object> getJVMStatus() {
        Map<String, Object> jvm = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapPercent = (double) heapUsed / heapMax * 100;

        jvm.put("heap_used_mb", heapUsed / 1024 / 1024);
        jvm.put("heap_max_mb", heapMax / 1024 / 1024);
        jvm.put("heap_percent", String.format("%.1f%%", heapPercent));
        jvm.put("status", heapPercent > 85 ? "⚠️ WARNING" : "✓ OK");

        return jvm;
    }

    /**
     * Get system status
     */
    private Map<String, Object> getSystemStatus() {
        Map<String, Object> system = new HashMap<>();
        try {
            system.put("hostname", InetAddress.getLocalHost().getHostName());
            system.put("ip_address", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            system.put("error", "Could not resolve hostname");
        }

        system.put("os_name", System.getProperty("os.name"));
        system.put("java_version", System.getProperty("java.version"));
        system.put("available_processors", Runtime.getRuntime().availableProcessors());

        return system;
    }

    /**
     * Extract connection info from URI
     */
    private Map<String, Object> extractConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            if (mongodbUri != null && !mongodbUri.isEmpty()) {
                if (mongodbUri.contains("@")) {
                    String[] parts = mongodbUri.split("@");
                    String hostPart = parts[1];
                    if (hostPart.contains("/")) {
                        hostPart = hostPart.split("/")[0];
                    }
                    info.put("host", hostPart);
                    info.put("has_credentials", true);
                } else {
                    info.put("error", "No credentials in URI");
                }
            }
        } catch (Exception e) {
            info.put("error", "Could not parse URI");
        }
        return info;
    }

    /**
     * Mask sensitive credentials in URI
     */
    private String maskUri(String uri) {
        if (uri == null || uri.isEmpty()) return "NOT SET";
        try {
            if (uri.contains("@")) {
                String[] parts = uri.split("@");
                return "mongodb+srv://***:***@" + parts[1];
            }
            return "mongodb+srv://...";
        } catch (Exception e) {
            return "ERROR: Could not parse URI";
        }
    }
}
