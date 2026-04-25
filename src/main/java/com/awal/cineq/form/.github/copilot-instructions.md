# NOTE (Project-Wide Instructions)

For repository-wide guidance, use the canonical file at:
`/.github/copilot-instructions.md`

This current file remains a deeper Universal Form System reference.

# 🚀 Universal Form System - Complete Copilot Guide

**Version**: 2.0 | **Updated**: February 8, 2026 | **Branch**: develop-mongodb

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [FormManager Structure](#formmanager-structure)
4. [FormStep Structure](#formstep-structure)
5. [Validation Rules - Complete Reference](#validation-rules---complete-reference)
6. [Field Types](#field-types)
7. [Workflow Rules](#workflow-rules)
8. [Serialization Configuration](#serialization-configuration)
9. [Field Transformers](#field-transformers)
10. [Permissions System](#permissions-system)
11. [API Endpoints](#api-endpoints)
12. [Complete FormManager Template](#complete-formmanager-template)
13. [Examples for Common Modules](#examples-for-common-modules)
14. [Copilot Generation Rules](#copilot-generation-rules)

---

## Overview

The **Universal Form System** is CineQ's primary pattern for handling ALL CRUD operations without writing traditional Controller → Service → Repository boilerplate. Instead of creating Java code for each entity, you configure a **FormManager** with **FormSteps** that define:

- **What fields to accept** (formSchema)
- **How to validate them** (validationRules)
- **Where to store data** (workflowRules.targetCollection)
- **Who can perform actions** (workflowRules.permissions)
- **How to serialize responses** (validationRules.*.serialization)

### Why Use This System?

| Traditional CRUD | Universal Form System |
|------------------|----------------------|
| Create Controller, Service, Repository, DTO classes | Configure JSON in FormManager |
| Write validation logic in Java | Define validation rules in JSON |
| Hardcode permissions in annotations | Configure permissions in workflowRules |
| Multiple files per entity | Single FormManager document |
| Code changes require recompile | Configuration changes are instant |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     API Layer                                   │
│  UniversalFormController (/v1/submit, /v1/view, /v1/list)     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Service Layer                                 │
│  UniversalFormServiceImpl                                       │
│    ├── FormConfigCacheService (cached FormManager lookup)       │
│    ├── PermissionService (authorization check)                  │
│    ├── JavaAnnotationValidator (validation engine)              │
│    ├── FieldTransformer (data transformation)                   │
│    ├── FieldSerializer (response serialization)                 │
│    └── InterceptorExecutor (lifecycle hooks)                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Data Layer                                    │
│  MongoTemplate → Dynamic targetCollection                       │
│  (e.g., "banners", "roles", "movies", "form_submissions")       │
└─────────────────────────────────────────────────────────────────┘
```

### Key Collections

| Collection | Purpose |
|------------|---------|
| `form_managers` | Stores FormManager definitions |
| `form_steps` | Stores FormStep configurations (referenced by FormManager) |
| `{targetCollection}` | Dynamic collection defined in workflowRules (e.g., "banners", "roles") |

---

## FormManager Structure

```json
{
  "_id": "ObjectId (auto-generated)",
  "title": "Human-readable title",
  "slug": "url-friendly-identifier (unique)",
  "description": "Optional description",
  "modelName": "Entity name (e.g., Banner, Role, Movie)",
  "isActive": true,
  "moduleCode": null,
  "formSteps": [/* Array of FormStep references */],
  "createdAt": "2026-02-08T00:00:00Z",
  "updatedAt": "2026-02-08T00:00:00Z",
  "deletedAt": null
}
```

### FormManager Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | ✅ | Display title |
| `slug` | String | ✅ | URL-friendly unique identifier (lowercase, hyphens allowed) |
| `description` | String | ❌ | Optional description |
| `modelName` | String | ❌ | Entity name for reference |
| `isActive` | Boolean | ❌ | Default: `true`. If `false`, form cannot be submitted |
| `moduleCode` | Integer | ❌ | Links to Module for RBAC integration |
| `formSteps` | Array | ✅ | At least one FormStep required |

---

## FormStep Structure

```json
{
  "_id": "ObjectId (auto-generated)",
  "formManagerId": "Reference to parent FormManager",
  "stepTitle": "Step display title",
  "stepSlug": "step-identifier (e.g., v1, details, basic-info)",
  "stepOrder": 1,
  "isActive": true,
  "validationRules": {/* Field validation definitions */},
  "formSchema": {/* UI form schema */},
  "uiSchema": {/* UI rendering hints */},
  "workflowRules": {/* Permissions, targetCollection, fieldMapping */},
  "metadata": {/* Optional metadata */},
  "createdAt": "2026-02-08T00:00:00Z",
  "updatedAt": "2026-02-08T00:00:00Z",
  "deletedAt": null
}
```

### FormStep Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `stepTitle` | String | ✅ | Display title for the step |
| `stepSlug` | String | ✅ | Identifier used in API calls (e.g., `v1`, `details`) |
| `stepOrder` | Integer | ✅ | Order in multi-step forms (start from 1) |
| `isActive` | Boolean | ❌ | Default: `true` |
| `validationRules` | Object | ✅ | **CRITICAL**: Defines all field validations |
| `formSchema` | Object | ❌ | JSON Schema for UI rendering |
| `uiSchema` | Object | ❌ | UI widget hints |
| `workflowRules` | Object | ✅ | **CRITICAL**: Defines targetCollection, permissions |
| `metadata` | Object | ❌ | Any additional metadata |

---

## Validation Rules - Complete Reference

### Structure of validationRules

```json
{
  "validationRules": {
    "fieldName": {
      "type": "STRING|INTEGER|BOOLEAN|OBJECT|ARRAY|DATE|DATETIME|DECIMAL|LONG|DOUBLE|FLOAT",
      "collectionField": "actual_field_name_in_mongodb",
      "fieldTransformer": "uppercase|lowercase|camelCase|PascalCase|snake_case|CONSTANT_CASE|kebab-case|null",
      "baseValidation": ["@NotBlank", "@Size(max=100)"],
      "actionRules": {
        "create": ["@NotBlank", "@Unique(collection='...', field='...')"],
        "update": ["@Exists(collection='...', field='_id')"],
        "delete": ["@Exists(collection='...', field='_id')"]
      },
      "conditionalRules": [
        {
          "condition": {"field": "status", "operator": "===", "value": "active"},
          "validation": ["@NotBlank"]
        }
      ],
      "dependencyRules": {
        "requiredIf": {"field": "hasDiscount", "operator": "===", "value": true, "message": "Required when discount is enabled"},
        "requiredUnless": {"field": "isGuest", "operator": "===", "value": true}
      },
      "serialization": {
        "select": true,
        "outputField": "displayFieldName",
        "format": "iso8601"
      }
    }
  }
}
```

### Validation Priority

```
actionRules (highest) > conditionalRules > dependencyRules > baseValidation (lowest)
```

If `actionRules[action]` is defined for the current action, it **completely overrides** `baseValidation`.

---

### All Supported Validators

#### Basic Validators

| Validator | Description | Example |
|-----------|-------------|---------|
| `@NotNull` | Value cannot be null | `@NotNull(message='Field is required')` |
| `@NotBlank` | String cannot be null/empty/whitespace | `@NotBlank(message='Name is required')` |
| `@NotEmpty` | Collection/String cannot be empty | `@NotEmpty(message='List cannot be empty')` |
| `@Size` | String/Collection length limits | `@Size(min=2, max=100, message='Must be 2-100 chars')` |
| `@Min` | Minimum numeric value | `@Min(value=1, message='Must be at least 1')` |
| `@Max` | Maximum numeric value | `@Max(value=1000, message='Cannot exceed 1000')` |
| `@Email` | Valid email format | `@Email(message='Invalid email format')` |
| `@Pattern` | Regex pattern matching | `@Pattern(regexp='^[A-Z]+$', message='Uppercase only')` |

#### Date Validators

| Validator | Description | Example |
|-----------|-------------|---------|
| `@DateFormat` | Validate date format | `@DateFormat(format='yyyy-MM-dd')` |
| `@FutureDate` / `@Future` | Date must be in future | `@FutureDate(message='Must be a future date')` |
| `@PastDate` / `@Past` | Date must be in past | `@PastDate(message='Must be a past date')` |
| `@DateAfter` | Date must be after another field | `@DateAfter(field='startDate', message='End must be after start')` |
| `@DateBefore` | Date must be before another field | `@DateBefore(field='endDate', message='Start must be before end')` |

#### Database Validators (MongoDB)

| Validator | Description | Example |
|-----------|-------------|---------|
| `@Unique` | Value must be unique in collection | `@Unique(collection='roles', field='name', message='Already exists')` |
| `@Exists` | Value must exist in collection | `@Exists(collection='roles', field='_id', message='Not found')` |
| `@UniqueExcludingSelf` | Unique but allow same value on UPDATE | `@UniqueExcludingSelf(collection='roles', field='name', idField='_id', message='Already exists')` |
| `@MustMatchExisting` | Value cannot be changed (immutable) | `@MustMatchExisting(collection='roles', field='code', idField='_id', message='Cannot change this field')` |

---

### Action-Aware Validation Pattern

**CRITICAL**: Always define `actionRules` for the `id` field to handle UPDATE/DELETE:

```json
{
  "id": {
    "type": "STRING",
    "collectionField": "_id",
    "baseValidation": [],
    "actionRules": {
      "create": [],
      "update": [
        "@NotBlank(message='ID is required for update')",
        "@Exists(collection='target_collection', field='_id', message='Document not found')"
      ],
      "delete": [
        "@NotBlank(message='ID is required for delete')",
        "@Exists(collection='target_collection', field='_id', message='Document not found')"
      ]
    },
    "serialization": {
      "select": true,
      "outputField": "id",
      "format": "string"
    }
  }
}
```

---

### Conditional Rules

Execute validation only when a condition is met:

```json
{
  "conditionalRules": [
    {
      "condition": {
        "field": "bannerType",
        "operator": "===",
        "value": "promo"
      },
      "validation": ["@NotBlank", "@Pattern(regexp='^https://.*')"]
    },
    {
      "condition": {
        "field": "userType",
        "operator": "in",
        "value": ["premium", "vip"]
      },
      "validation": ["@Size(min=10)"]
    }
  ]
}
```

#### Supported Condition Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `===` / `==` | Equals | `{"field": "status", "operator": "===", "value": "active"}` |
| `!==` / `!=` | Not equals | `{"field": "type", "operator": "!==", "value": "guest"}` |
| `in` | Value in list | `{"field": "role", "operator": "in", "value": ["ADMIN", "MANAGER"]}` |
| `notIn` | Value not in list | `{"field": "status", "operator": "notIn", "value": ["deleted", "banned"]}` |
| `isEmpty` | Value is null/empty | `{"field": "email", "operator": "isEmpty"}` |
| `isNotEmpty` | Value exists | `{"field": "phone", "operator": "isNotEmpty"}` |
| `>`, `>=`, `<`, `<=` | Numeric comparison | `{"field": "age", "operator": ">=", "value": 18}` |
| `contains` | String contains | `{"field": "tags", "operator": "contains", "value": "featured"}` |
| `startsWith` | String starts with | `{"field": "code", "operator": "startsWith", "value": "PRO"}` |
| `endsWith` | String ends with | `{"field": "email", "operator": "endsWith", "value": "@company.com"}` |

---

### Dependency Rules

Make fields conditionally required based on other fields:

```json
{
  "startDate": {
    "type": "DATETIME",
    "dependencyRules": {
      "requiredIf": {
        "field": "isActive",
        "operator": "===",
        "value": true,
        "message": "Start date required when banner is active"
      }
    }
  },
  "discountCode": {
    "type": "STRING",
    "dependencyRules": {
      "requiredUnless": {
        "field": "isGuest",
        "operator": "===",
        "value": true,
        "message": "Discount code required for registered users"
      }
    }
  }
}
```

---

## Field Types

| Type | Java Class | MongoDB Type | Example Value |
|------|------------|--------------|---------------|
| `STRING` | String | string | `"hello world"` |
| `INTEGER` | Integer | int32 | `42` |
| `LONG` | Long | int64 | `9223372036854775807` |
| `DOUBLE` | Double | double | `3.14159` |
| `FLOAT` | Float | double | `3.14` |
| `BOOLEAN` | Boolean | bool | `true` / `false` |
| `DECIMAL` | BigDecimal | string/decimal128 | `"99.99"` |
| `OBJECT` | Map | object | `{"key": "value"}` |
| `ARRAY` | Collection/List | array | `[1, 2, 3]` |
| `DATE` | LocalDate | date | `"2026-02-08"` |
| `DATETIME` | LocalDateTime | date | `"2026-02-08T10:30:00Z"` |

---

## Workflow Rules

```json
{
  "workflowRules": {
    "targetCollection": "banners",
    "persistToCollection": true,
    
    "fieldMapping": {
      "formFieldName": "mongoFieldName",
      "bannerTitle": "title",
      "bannerType": "type"
    },
    
    "fieldTransformers": {
      "slug": "lowercase",
      "code": "uppercase"
    },
    
    "permissions": {
      "create": ["ADMIN", "MANAGER"],
      "read": ["USER", "ADMIN", "MANAGER"],
      "update": ["ADMIN", "MANAGER"],
      "delete": ["ADMIN"],
      "export": ["ADMIN"]
    },
    
    "actions": {
      "create": {
        "enabled": true,
        "autoApprove": true,
        "autoFields": {
          "createdAt": "{{now}}",
          "updatedAt": "{{now}}",
          "deletedAt": null
        }
      },
      "update": {
        "enabled": true,
        "autoFields": {
          "updatedAt": "{{now}}"
        }
      },
      "delete": {
        "enabled": true,
        "softDelete": true,
        "autoFields": {
          "deletedAt": "{{now}}"
        }
      }
    }
  }
}
```

### Key workflowRules Fields

| Field | Required | Description |
|-------|----------|-------------|
| `targetCollection` | ✅ | MongoDB collection name to store documents |
| `permissions` | ✅ | Role-based permissions for each action |
| `fieldMapping` | ❌ | Map form field names to MongoDB field names |
| `fieldTransformers` | ❌ | Apply transformations before saving |
| `actions` | ❌ | Configure behavior for each action type |

---

## Serialization Configuration

Control how fields are returned in API responses:

```json
{
  "validationRules": {
    "id": {
      "type": "STRING",
      "collectionField": "_id",
      "serialization": {
        "select": true,
        "outputField": "id",
        "format": "string"
      }
    },
    "createdAt": {
      "type": "DATETIME",
      "collectionField": "createdAt",
      "serialization": {
        "select": true,
        "outputField": "createdAt",
        "format": "iso8601"
      }
    },
    "internalNotes": {
      "type": "STRING",
      "collectionField": "internalNotes",
      "serialization": {
        "select": false
      }
    }
  }
}
```

### Serialization Options

| Option | Type | Description |
|--------|------|-------------|
| `select` | Boolean | `true` = include in response, `false` = exclude |
| `outputField` | String | Rename field in response (e.g., `_id` → `id`) |
| `format` | String | `"iso8601"` for dates, `"string"` for ObjectIds |

---

## Field Transformers

Apply transformations to field values before saving:

| Transformer | Example Input | Example Output |
|-------------|---------------|----------------|
| `uppercase` | `"hello world"` | `"HELLO WORLD"` |
| `lowercase` | `"Hello World"` | `"hello world"` |
| `camelCase` | `"hello_world"` | `"helloWorld"` |
| `PascalCase` | `"hello_world"` | `"HelloWorld"` |
| `snake_case` | `"helloWorld"` | `"hello_world"` |
| `CONSTANT_CASE` | `"hello-world"` | `"HELLO_WORLD"` |
| `kebab-case` | `"helloWorld"` | `"hello-world"` |

Usage in validationRules:
```json
{
  "slug": {
    "type": "STRING",
    "collectionField": "slug",
    "fieldTransformer": "lowercase"
  }
}
```

---

## Permissions System

### Permission Structure

```json
{
  "permissions": {
    "create": ["ADMIN", "MANAGER"],
    "read": ["USER", "ADMIN", "MANAGER"],
    "update": ["ADMIN", "MANAGER"],
    "delete": ["ADMIN"],
    "export": ["ADMIN"]
  }
}
```

### Prominent Role (Superuser Bypass)

Users with the **prominent role** (configured via `app.security.prominent-role`) bypass ALL permission checks.

```properties
# application.properties
app.security.prominent-role=SUPER_ADMIN
```

### Authorization Providers

Configure via `app.security.authorization-provider`:

1. **workflow** (default): Uses `workflowRules.permissions`
2. **RBAC**: Uses `RBACPermissionService` with module codes

---

## API Endpoints

### Universal Form API (`/v1`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/submit/{formSlug}` | CREATE/UPDATE/DELETE any entity |
| `GET` | `/v1/view/{formSlug}/{id}` | Get single document |
| `GET` | `/v1/list/{formSlug}?page=1&size=15` | List documents (paginated) |
| `GET` | `/v1/list/user/{username}` | List by user |
| `PATCH` | `/v1/update-status` | Bulk update isActive |
| `DELETE` | `/v1/delete` | Bulk soft-delete |

### FormManager API (`/form-manager`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/form-manager` | List all form managers |
| `GET` | `/form-manager/{id}` | Get by ID |
| `GET` | `/form-manager/slug/{slug}` | Get by slug |
| `POST` | `/form-manager` | Create new form manager |
| `PUT` | `/form-manager/{id}` | Update form manager |
| `DELETE` | `/form-manager/{id}` | Soft-delete |

### Request Body for Submit

```json
{
  "stepSlug": "v1",
  "action": "CREATE",
  "formData": {
    "name": "Sample Name",
    "description": "Sample description",
    "isActive": true
  }
}
```

#### Action Values

| Action | Code | Char | Description |
|--------|------|------|-------------|
| `CREATE` | 1 | C | Create new document |
| `READ` | 2 | R | View document |
| `UPDATE` | 3 | U | Update existing (requires `id` in formData) |
| `DELETE` | 4 | D | Soft-delete (requires `id` in formData) |

---

## Complete FormManager Template

Use this template when generating FormManager configurations:

```json
{
  "title": "{{MODULE_TITLE}} Management",
  "slug": "{{module-slug}}",
  "description": "Manage {{module description}}",
  "modelName": "{{ModelName}}",
  "isActive": true,
  "formSteps": [
    {
      "stepTitle": "{{Module}} Form",
      "stepSlug": "v1",
      "stepOrder": 1,
      "isActive": true,
      
      "validationRules": {
        "id": {
          "type": "STRING",
          "collectionField": "_id",
          "baseValidation": [],
          "actionRules": {
            "create": [],
            "update": [
              "@NotBlank(message='ID is required for update')",
              "@Exists(collection='{{collection_name}}', field='_id', message='{{ModelName}} not found')"
            ],
            "delete": [
              "@NotBlank(message='ID is required for delete')",
              "@Exists(collection='{{collection_name}}', field='_id', message='{{ModelName}} not found')"
            ]
          },
          "serialization": {
            "select": true,
            "outputField": "id",
            "format": "string"
          }
        },
        "name": {
          "type": "STRING",
          "collectionField": "name",
          "baseValidation": [
            "@NotBlank(message='Name is required')",
            "@Size(min=2, max=100, message='Name must be 2-100 characters')"
          ],
          "actionRules": {
            "create": [
              "@NotBlank(message='Name is required')",
              "@Size(min=2, max=100)",
              "@Unique(collection='{{collection_name}}', field='name', message='Name already exists')"
            ],
            "update": [
              "@Size(min=2, max=100)",
              "@UniqueExcludingSelf(collection='{{collection_name}}', field='name', idField='_id', message='Name already exists')"
            ],
            "delete": []
          },
          "serialization": {
            "select": true,
            "outputField": "name"
          }
        },
        "isActive": {
          "type": "BOOLEAN",
          "collectionField": "isActive",
          "baseValidation": [],
          "actionRules": {},
          "serialization": {
            "select": true,
            "outputField": "isActive"
          }
        },
        "createdAt": {
          "type": "DATETIME",
          "collectionField": "createdAt",
          "baseValidation": [],
          "actionRules": {},
          "serialization": {
            "select": true,
            "outputField": "createdAt",
            "format": "iso8601"
          }
        },
        "updatedAt": {
          "type": "DATETIME",
          "collectionField": "updatedAt",
          "baseValidation": [],
          "actionRules": {},
          "serialization": {
            "select": true,
            "outputField": "updatedAt",
            "format": "iso8601"
          }
        }
      },
      
      "formSchema": {
        "type": "object",
        "title": "{{Module}} Form",
        "properties": {
          "name": {
            "type": "STRING",
            "title": "Name",
            "placeholder": "Enter name",
            "required": true
          },
          "isActive": {
            "type": "BOOLEAN",
            "title": "Active",
            "default": true
          }
        },
        "required": ["name"]
      },
      
      "uiSchema": {
        "name": {
          "ui:widget": "text",
          "ui:autofocus": true
        },
        "isActive": {
          "ui:widget": "checkbox"
        }
      },
      
      "workflowRules": {
        "targetCollection": "{{collection_name}}",
        "permissions": {
          "create": ["ADMIN", "MANAGER"],
          "read": ["USER", "ADMIN", "MANAGER"],
          "update": ["ADMIN", "MANAGER"],
          "delete": ["ADMIN"]
        }
      },
      
      "metadata": {
        "version": "1.0",
        "category": "{{category}}"
      }
    }
  ]
}
```

---

## Examples for Common Modules

### Example 1: Categories Module

```json
{
  "title": "Category Management",
  "slug": "categories",
  "description": "Manage content categories",
  "modelName": "Category",
  "isActive": true,
  "formSteps": [{
    "stepTitle": "Category Form",
    "stepSlug": "v1",
    "stepOrder": 1,
    "isActive": true,
    "validationRules": {
      "id": {
        "type": "STRING",
        "collectionField": "_id",
        "actionRules": {
          "update": ["@NotBlank", "@Exists(collection='categories', field='_id')"],
          "delete": ["@NotBlank", "@Exists(collection='categories', field='_id')"]
        },
        "serialization": {"select": true, "outputField": "id"}
      },
      "name": {
        "type": "STRING",
        "collectionField": "name",
        "baseValidation": ["@NotBlank", "@Size(max=100)"],
        "actionRules": {
          "create": ["@NotBlank", "@Unique(collection='categories', field='name')"],
          "update": ["@UniqueExcludingSelf(collection='categories', field='name', idField='_id')"]
        },
        "serialization": {"select": true, "outputField": "name"}
      },
      "slug": {
        "type": "STRING",
        "collectionField": "slug",
        "fieldTransformer": "lowercase",
        "baseValidation": ["@NotBlank", "@Pattern(regexp='^[a-z0-9-]+$')"],
        "actionRules": {
          "create": ["@NotBlank", "@Unique(collection='categories', field='slug')"],
          "update": ["@UniqueExcludingSelf(collection='categories', field='slug', idField='_id')"]
        },
        "serialization": {"select": true, "outputField": "slug"}
      },
      "parentId": {
        "type": "STRING",
        "collectionField": "parentId",
        "baseValidation": [],
        "actionRules": {
          "create": ["@Exists(collection='categories', field='_id', message='Parent category not found')"],
          "update": ["@Exists(collection='categories', field='_id', message='Parent category not found')"]
        },
        "serialization": {"select": true, "outputField": "parentId"}
      },
      "isActive": {
        "type": "BOOLEAN",
        "collectionField": "isActive",
        "serialization": {"select": true, "outputField": "isActive"}
      }
    },
    "workflowRules": {
      "targetCollection": "categories",
      "permissions": {
        "create": ["ADMIN"],
        "read": ["USER", "ADMIN"],
        "update": ["ADMIN"],
        "delete": ["ADMIN"]
      }
    }
  }]
}
```

### Example 2: Settings/Configuration Module

```json
{
  "title": "Application Settings",
  "slug": "settings",
  "description": "Manage application configuration",
  "modelName": "Setting",
  "isActive": true,
  "formSteps": [{
    "stepTitle": "Setting Form",
    "stepSlug": "v1",
    "stepOrder": 1,
    "isActive": true,
    "validationRules": {
      "id": {
        "type": "STRING",
        "collectionField": "_id",
        "actionRules": {
          "update": ["@NotBlank", "@Exists(collection='settings', field='_id')"],
          "delete": ["@NotBlank", "@Exists(collection='settings', field='_id')"]
        },
        "serialization": {"select": true, "outputField": "id"}
      },
      "key": {
        "type": "STRING",
        "collectionField": "key",
        "fieldTransformer": "CONSTANT_CASE",
        "baseValidation": ["@NotBlank", "@Size(max=100)", "@Pattern(regexp='^[A-Z][A-Z0-9_]*$')"],
        "actionRules": {
          "create": ["@NotBlank", "@Unique(collection='settings', field='key')"],
          "update": ["@MustMatchExisting(collection='settings', field='key', idField='_id', message='Key cannot be changed')"]
        },
        "serialization": {"select": true, "outputField": "key"}
      },
      "value": {
        "type": "STRING",
        "collectionField": "value",
        "baseValidation": ["@NotBlank"],
        "serialization": {"select": true, "outputField": "value"}
      },
      "description": {
        "type": "STRING",
        "collectionField": "description",
        "baseValidation": ["@Size(max=500)"],
        "serialization": {"select": true, "outputField": "description"}
      },
      "group": {
        "type": "STRING",
        "collectionField": "group",
        "baseValidation": ["@NotBlank"],
        "serialization": {"select": true, "outputField": "group"}
      }
    },
    "workflowRules": {
      "targetCollection": "settings",
      "permissions": {
        "create": ["ADMIN"],
        "read": ["ADMIN"],
        "update": ["ADMIN"],
        "delete": ["ADMIN"]
      }
    }
  }]
}
```

---

## Copilot Generation Rules

When asked to generate a FormManager for a new module, follow these rules:

### 1. Required Structure
- Always include `id` field with proper `actionRules` for UPDATE/DELETE
- Always include `createdAt`, `updatedAt` with `iso8601` format serialization
- Use `v1` as default `stepSlug`

### 2. Naming Conventions
- `slug`: lowercase, hyphen-separated (e.g., `user-profiles`)
- `targetCollection`: lowercase, plural (e.g., `user_profiles` or `userProfiles`)
- `collectionField`: match MongoDB field names exactly

### 3. Validation Best Practices
- Always use `@NotBlank` before `@Unique` or `@Exists`
- Use `@UniqueExcludingSelf` for UPDATE to allow keeping same value
- Use `@Exists` on `id` field for UPDATE/DELETE actions
- Use `@MustMatchExisting` for immutable fields

### 4. Default Permissions
```json
{
  "create": ["ADMIN", "MANAGER"],
  "read": ["USER", "ADMIN", "MANAGER"],
  "update": ["ADMIN", "MANAGER"],
  "delete": ["ADMIN"]
}
```

### 5. Common Field Patterns

#### Slug Field
```json
{
  "slug": {
    "type": "STRING",
    "collectionField": "slug",
    "fieldTransformer": "lowercase",
    "baseValidation": ["@NotBlank", "@Size(max=100)", "@Pattern(regexp='^[a-z0-9-]+$')"],
    "actionRules": {
      "create": ["@NotBlank", "@Unique(collection='{{collection}}', field='slug')"],
      "update": ["@UniqueExcludingSelf(collection='{{collection}}', field='slug', idField='_id')"]
    }
  }
}
```

#### Email Field
```json
{
  "email": {
    "type": "STRING",
    "collectionField": "email",
    "fieldTransformer": "lowercase",
    "baseValidation": ["@NotBlank", "@Email", "@Size(max=255)"],
    "actionRules": {
      "create": ["@NotBlank", "@Email", "@Unique(collection='{{collection}}', field='email')"],
      "update": ["@Email", "@UniqueExcludingSelf(collection='{{collection}}', field='email', idField='_id')"]
    }
  }
}
```

#### Code/Identifier Field (Immutable)
```json
{
  "code": {
    "type": "STRING",
    "collectionField": "code",
    "fieldTransformer": "uppercase",
    "baseValidation": ["@NotBlank", "@Pattern(regexp='^[A-Z][A-Z0-9_]*$')"],
    "actionRules": {
      "create": ["@NotBlank", "@Unique(collection='{{collection}}', field='code')"],
      "update": ["@MustMatchExisting(collection='{{collection}}', field='code', idField='_id', message='Code cannot be changed')"]
    }
  }
}
```

#### Foreign Key Reference
```json
{
  "categoryId": {
    "type": "STRING",
    "collectionField": "categoryId",
    "baseValidation": ["@NotBlank"],
    "actionRules": {
      "create": ["@NotBlank", "@Exists(collection='categories', field='_id', message='Category not found')"],
      "update": ["@Exists(collection='categories', field='_id', message='Category not found')"]
    }
  }
}
```

#### Price/Amount Field
```json
{
  "price": {
    "type": "DECIMAL",
    "collectionField": "price",
    "baseValidation": ["@NotNull", "@Min(value=0)", "@Max(value=999999.99)"]
  }
}
```

#### Date Range Fields
```json
{
  "startDate": {
    "type": "DATETIME",
    "collectionField": "startDate",
    "baseValidation": ["@NotNull", "@DateFormat(format='yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\'')"],
    "actionRules": {
      "create": ["@NotNull", "@FutureDate(message='Start date must be in future')"]
    }
  },
  "endDate": {
    "type": "DATETIME",
    "collectionField": "endDate",
    "baseValidation": ["@DateFormat(format='yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\'')", "@DateAfter(field='startDate')"]
  }
}
```

---

## Quick Reference Card

### Validator Cheat Sheet

| Use Case | Validator |
|----------|-----------|
| Required string | `@NotBlank` |
| Required any type | `@NotNull` |
| Length limits | `@Size(min=X, max=Y)` |
| Number range | `@Min(value=X)`, `@Max(value=Y)` |
| Email format | `@Email` |
| Pattern matching | `@Pattern(regexp='...')` |
| Unique on CREATE | `@Unique(collection='...', field='...')` |
| Unique on UPDATE | `@UniqueExcludingSelf(collection='...', field='...', idField='_id')` |
| Must exist | `@Exists(collection='...', field='...')` |
| Cannot change | `@MustMatchExisting(collection='...', field='...', idField='_id')` |
| Future date | `@FutureDate` |
| Past date | `@PastDate` |
| After another field | `@DateAfter(field='...')` |

### Serialization Quick Reference

```json
{
  "serialization": {
    "select": true,
    "outputField": "fieldName",
    "format": "iso8601"
  }
}
```

| Format | Use For |
|--------|---------|
| `iso8601` | DateTime fields → `"2026-02-08T10:30:00Z"` |
| `string` | ObjectId → String conversion |
| (none) | Keep original type |

---

**End of Document**

*This guide is the authoritative reference for generating FormManager configurations. When asked to create a new module, use this document as your blueprint.*
