package com.awal.cineq.action.seeder;

import com.awal.cineq.action.model.Action;
import com.awal.cineq.action.repository.ActionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Action Seeder - Initializes standard actions on application startup
 * Creates predefined actions: VIEW, ADD, EDIT, DELETE, EXPORT, APPROVE, REJECT
 * 
 * Runs once on application startup via @PostConstruct
 */
//@Component
//@Slf4j
@RequiredArgsConstructor
public class ActionSeeder {

    private final ActionRepository actionRepository;
    // Logging disabled - uncomment @Slf4j when @Component is enabled

    /**
     * Seed standard actions if they don't exist
     * This ensures consistent actions across all environments
     */
//    @PostConstruct
    public void seedActions() {
        System.out.println("seedActions STARTED");
        try {
            List<ActionData> actionDataList = List.of(
                    new ActionData("VIEW", "View", "View and read access to resources"),
                    new ActionData("ADD", "Add", "Create new resources"),
                    new ActionData("EDIT", "Edit", "Modify existing resources"),
                    new ActionData("DELETE", "Delete", "Remove resources (soft delete)"),
                    new ActionData("EXPORT", "Export", "Export data to external formats"),
                    new ActionData("APPROVE", "Approve", "Approve pending requests or submissions"),
                    new ActionData("REJECT", "Reject", "Reject pending requests or submissions")
            );

            int createdCount = 0;
            for (ActionData data : actionDataList) {
                Optional<Action> existingAction = actionRepository.findByCodeAndDeletedAtIsNull(data.code);
                if (existingAction.isEmpty()) {
                    Action action = new Action();
                    action.setCode(data.code);
                    action.setName(data.name);
                    action.setDescription(data.description);
                    action.setIsEnabled(true);
                    actionRepository.save(action);
                    System.out.println("seedActions: created action code=" + data.code + ", name=" + data.name);
                    createdCount++;
                } else {
                    System.out.println("seedActions: action already exists code=" + data.code);
                }
            }

            System.out.println("seedActions END: created " + createdCount + " new actions, total seeded=" + actionDataList.size());

        } catch (Exception e) {
            System.err.println("seedActions ERROR: Failed to seed actions: " + e.getMessage());
        }
    }

    /**
     * Inner class to hold action seed data
     */
    private static class ActionData {
        final String code;
        final String name;
        final String description;

        ActionData(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
    }
}
