package com.awal.cineq.form.interceptor;

import com.awal.cineq.common.util.ConditionEvaluator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executor service for running interceptor handlers.
 *
 * Uses reflection to discover and invoke methods on UniversalInterceptor.
 * Method name in UniversalInterceptor = handler name in JSON config.
 *
 * Execution flow:
 * 1. executeBefore() - before main action (throws exception to stop)
 * 2. [main action executes]
 * 3. executeAfter() - always runs (like finally)
 * 4. executeAfterReturning() - only on success
 * OR executeAfterThrowing() - only on failure
 *
 * Interceptors are completely optional. If not defined, methods return immediately.
 */
@Slf4j
@Service
public class InterceptorExecutor {

    private final UniversalInterceptor universalInterceptor;

    /**
     * Cache of handler methods: methodName -> Method object
     */
    private final Map<String, Method> methodCache = new HashMap<>();

    public InterceptorExecutor(UniversalInterceptor universalInterceptor) {
        this.universalInterceptor = universalInterceptor;
    }

    /**
     * Discover and cache all handler methods from UniversalInterceptor on startup.
     */
    @PostConstruct
    public void discoverHandlerMethods() {
        log.info("Discovering handler methods in UniversalInterceptor...");

        for (Method method : UniversalInterceptor.class.getDeclaredMethods()) {
            // Only cache public methods with single InterceptorContext parameter
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) &&
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0] == InterceptorContext.class) {

                methodCache.put(method.getName(), method);
                log.debug("Registered handler: {}", method.getName());
            }
        }

        log.info("Total handlers registered: {} - {}", methodCache.size(), methodCache.keySet());
    }

    /**
     * Execute 'before' interceptors.
     * If any handler throws an exception, the action is stopped.
     *
     * @param context interceptor context
     * @throws RuntimeException if any handler fails (stops the action)
     */
    public void executeBefore(InterceptorContext context) {
        executePhase(context, "before", true);
    }

    /**
     * Execute 'after' interceptors (always runs, like finally).
     * Exceptions are caught and logged but don't affect the response.
     *
     * @param context interceptor context
     */
    public void executeAfter(InterceptorContext context) {
        try {
            executePhase(context, "after", false);
        } catch (Exception e) {
            log.error("Error in 'after' interceptors (continuing): {}", e.getMessage(), e);
        }
    }

    /**
     * Execute 'afterReturning' interceptors (only on success).
     * Exceptions are caught and logged but don't affect the response.
     *
     * @param context interceptor context with result set
     */
    public void executeAfterReturning(InterceptorContext context) {
        try {
            executePhase(context, "afterReturning", false);
        } catch (Exception e) {
            log.error("Error in 'afterReturning' interceptors (continuing): {}", e.getMessage(), e);
        }
    }

    /**
     * Execute 'afterThrowing' interceptors (only on failure).
     * Exceptions are caught and logged but don't affect the response.
     *
     * @param context interceptor context with exception set in result
     */
    public void executeAfterThrowing(InterceptorContext context) {
        try {
            executePhase(context, "afterThrowing", false);
        } catch (Exception e) {
            log.error("Error in 'afterThrowing' interceptors (continuing): {}", e.getMessage(), e);
        }
    }

    /**
     * Execute interceptors for a specific phase.
     *
     * @param context interceptor context
     * @param phase before, after, afterReturning, or afterThrowing
     * @param throwOnError whether to throw exceptions (true for 'before' phase)
     */
    @SuppressWarnings("unchecked")
    private void executePhase(InterceptorContext context, String phase, boolean throwOnError) {
        // Get interceptors config from workflowRules.actions[action].interceptors
        Map<String, Object> interceptorsConfig = getInterceptorsConfig(context);

        if (interceptorsConfig == null) {
            log.debug("No interceptors configured, skipping {} phase", phase);
            return;
        }

        // Get handlers for this phase
        Object phaseHandlers = interceptorsConfig.get(phase);

        if (phaseHandlers == null) {
            log.debug("No '{}' interceptors configured, skipping", phase);
            return;
        }

        if (!(phaseHandlers instanceof List)) {
            log.warn("Invalid '{}' interceptors config - expected List, got {}",
                    phase, phaseHandlers.getClass().getSimpleName());
            return;
        }

        List<Map<String, Object>> handlerConfigs = (List<Map<String, Object>>) phaseHandlers;

        log.debug("Executing {} '{}' interceptors", handlerConfigs.size(), phase);

        for (Map<String, Object> handlerConfig : handlerConfigs) {
            executeHandler(context, handlerConfig, phase, throwOnError);
        }
    }

    /**
     * Execute a single handler based on config.
     *
     * @param context interceptor context
     * @param handlerConfig handler configuration: {handler, args, when}
     * @param phase current phase for logging
     * @param throwOnError whether to throw exceptions
     */
    @SuppressWarnings("unchecked")
    private void executeHandler(InterceptorContext context,
                                Map<String, Object> handlerConfig,
                                String phase,
                                boolean throwOnError) {
        String handlerName = (String) handlerConfig.get("handler");

        if (handlerName == null || handlerName.isEmpty()) {
            log.warn("Handler config missing 'handler' name in {} phase, skipping", phase);
            return;
        }

        // Check if method exists in cache
        Method method = methodCache.get(handlerName);
        if (method == null) {
            log.warn("Handler '{}' not found in UniversalInterceptor, skipping. Available: {}",
                    handlerName, methodCache.keySet());
            return;
        }

        // Check 'when' condition
        Object whenCondition = handlerConfig.get("when");
        if (whenCondition != null && whenCondition instanceof Map) {
            Map<String, Object> condition = (Map<String, Object>) whenCondition;

            if (!ConditionEvaluator.evaluate(condition, context.getFormData())) {
                log.debug("Handler '{}' skipped - 'when' condition not met", handlerName);
                return;
            }
        }

        // Set handler args in context
        Object args = handlerConfig.get("args");
        if (args instanceof Map) {
            context.setArgs((Map<String, Object>) args);
        } else {
            context.setArgs(Map.of());
        }

        // Execute handler via reflection
        log.debug("Executing handler '{}' in {} phase", handlerName, phase);

        try {
            method.invoke(universalInterceptor, context);
            log.debug("Handler '{}' completed successfully", handlerName);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Handler '{}' failed: {}", handlerName, cause.getMessage());

            if (throwOnError) {
                throw new RuntimeException("Interceptor failed: " + handlerName, cause);
            }
        }
    }

    /**
     * Extract interceptors config from workflowRules.actions[action].interceptors
     *
     * @param context interceptor context
     * @return interceptors config map or null if not defined
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getInterceptorsConfig(InterceptorContext context) {
        Map<String, Object> workflowRules = context.getWorkflowRules();

        if (workflowRules == null) {
            return null;
        }

        Object actionsObj = workflowRules.get("actions");
        if (!(actionsObj instanceof Map)) {
            return null;
        }

        Map<String, Object> actions = (Map<String, Object>) actionsObj;
        String action = context.getAction() != null ? context.getAction().toLowerCase() : null;

        if (action == null) {
            return null;
        }

        Object actionConfigObj = actions.get(action);
        if (!(actionConfigObj instanceof Map)) {
            return null;
        }

        Map<String, Object> actionConfig = (Map<String, Object>) actionConfigObj;
        Object interceptorsObj = actionConfig.get("interceptors");

        if (!(interceptorsObj instanceof Map)) {
            return null;
        }

        return (Map<String, Object>) interceptorsObj;
    }

    /**
     * Check if interceptors are configured for a given action.
     *
     * @param workflowRules workflow rules map
     * @param action action name (create, update, delete)
     * @return true if interceptors are configured
     */
    @SuppressWarnings("unchecked")
    public boolean hasInterceptors(Map<String, Object> workflowRules, String action) {
        if (workflowRules == null || action == null) {
            return false;
        }

        Object actionsObj = workflowRules.get("actions");
        if (!(actionsObj instanceof Map)) {
            return false;
        }

        Map<String, Object> actions = (Map<String, Object>) actionsObj;
        Object actionConfigObj = actions.get(action.toLowerCase());

        if (!(actionConfigObj instanceof Map)) {
            return false;
        }

        Map<String, Object> actionConfig = (Map<String, Object>) actionConfigObj;
        Object interceptorsObj = actionConfig.get("interceptors");

        return interceptorsObj instanceof Map && !((Map<?, ?>) interceptorsObj).isEmpty();
    }

    /**
     * Check if a handler method exists.
     *
     * @param handlerName handler method name
     * @return true if handler exists
     */
    public boolean hasHandler(String handlerName) {
        return methodCache.containsKey(handlerName);
    }

    /**
     * Get all registered handler names.
     *
     * @return set of handler names
     */
    public java.util.Set<String> getRegisteredHandlers() {
        return methodCache.keySet();
    }
}
