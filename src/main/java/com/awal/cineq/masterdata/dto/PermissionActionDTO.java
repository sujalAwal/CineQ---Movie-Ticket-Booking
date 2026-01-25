package com.awal.cineq.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.awal.cineq.form.enums.FormAction;

/**
 * DTO for FormAction enum
 * Used to send action metadata to frontend for form submissions
 *
 * Example:
 * {
 *   "code": 1,
 *   "charCode": "C",
 *   "actionName": "create",
 *   "description": "Create new document",
 *   "readOnly": false,
 *   "writeAction": true,
 *   "requiresId": false
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionActionDTO {
    private int code;                   // 1, 2, 3, 4
    private String actionName;          // create, read, update, delete
    private boolean readOnly;           // Is this a read-only action?
    private boolean writeAction;        // Is this a write action?
    private boolean requiresId;         // Does this action require a document ID?

    /**
     * Convert FormAction enum to DTO
     */
    public static PermissionActionDTO fromEnum(FormAction action) {
        return PermissionActionDTO.builder()
                .code(action.getCode())
                .actionName(action.getActionName())
                .readOnly(action.isReadOnly())
                .writeAction(action.isWriteAction())
                .requiresId(action.requiresId())
                .build();
    }
}

