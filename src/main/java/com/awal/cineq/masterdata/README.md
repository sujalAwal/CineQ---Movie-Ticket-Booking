# Master Data Module

## 📋 Overview

The **Master Data Module** is a centralized service for providing enum and reference data to the frontend. It acts as a single source of truth for all dropdowns, select fields, and form options.

**Purpose**: Frontend calls this API once on page load to fetch all available enum values (like FormAction codes), then uses these values when submitting forms.

---

## 🎯 Architecture

```
Controller (MasterDataController)
    ↓
Service (MasterDataService)
    ↓
DTOs (FormActionDTO, MasterDataResponse)
    ↓
Enums (FormAction from form module)
```

### Why Separate Module?
- **Centralized**: Single endpoint for all reference data
- **Scalable**: Add new enums without changing existing code
- **Cacheable**: Frontend/CDN can cache responses
- **Decoupled**: Frontend doesn't need to know enum structure

---

## 📁 Module Structure

```
masterdata/
├── controller/
│   └── MasterDataController.java
├── service/
│   ├── MasterDataService.java
│   └── impl/
│       └── MasterDataServiceImpl.java
└── dto/
    ├── FormActionDTO.java
    └── MasterDataResponse.java
```

---

## 🔌 API Endpoints

### 1. Get All Master Data
```
GET /api/v1/master-data
```

**Response**:
```json
{
  "success": true,
  "message": "Master data fetched successfully",
  "data": {
    "formActions": [
      {
        "code": 1,
        "charCode": "C",
        "actionName": "create",
        "description": "Create new document",
        "readOnly": false,
        "writeAction": true,
        "requiresId": false,
        "allCodes": "1 (C)"
      },
      {
        "code": 2,
        "charCode": "R",
        "actionName": "read",
        "description": "View document",
        "readOnly": true,
        "writeAction": false,
        "requiresId": true,
        "allCodes": "2 (R)"
      },
      {
        "code": 3,
        "charCode": "U",
        "actionName": "update",
        "description": "Update existing document",
        "readOnly": false,
        "writeAction": true,
        "requiresId": true,
        "allCodes": "3 (U)"
      },
      {
        "code": 4,
        "charCode": "D",
        "actionName": "delete",
        "description": "Delete document",
        "readOnly": false,
        "writeAction": true,
        "requiresId": true,
        "allCodes": "4 (D)"
      }
    ]
  },
  "timestamp": "2025-01-10T10:30:00"
}
```

### 2. Get Form Actions Only
```
GET /api/v1/master-data/form-actions
```

**Response**: Same as above but only with `formActions` field (more lightweight)

---

## 🎨 Frontend Usage Example

### React/Vue Example:

```javascript
// 1. On component mount, fetch master data
useEffect(() => {
  const fetchMasterData = async () => {
    const response = await fetch('/api/v1/master-data');
    const data = await response.json();
    setMasterData(data.data);  // Store in context/state
  };
  fetchMasterData();
}, []);

// 2. Use in form submission
const handleFormSubmit = (formData, actionCharCode) => {
  const action = masterData.formActions.find(
    a => a.charCode === actionCharCode
  );
  
  const payload = {
    action: action.charCode,        // "C" or use action.code (1)
    actionName: action.actionName,  // "create"
    formData: formData,             // User input
    stepId: "..."
  };
  
  // Send to form submission endpoint
  submitForm(payload);
};

// 3. Render dropdown with actions
<select>
  {masterData.formActions.map(action => (
    <option key={action.code} value={action.charCode}>
      {action.actionName.toUpperCase()} - {action.description}
    </option>
  ))}
</select>
```

---

## 🔄 Adding New Enums

### Example: Adding a new enum (e.g., `FormStatus`)

**Step 1**: Create enum in your module
```java
// In form/enums/FormStatus.java
public enum FormStatus {
    DRAFT("D", "draft", "Not submitted"),
    SUBMITTED("S", "submitted", "Submitted for review"),
    APPROVED("A", "approved", "Approved"),
    REJECTED("R", "rejected", "Rejected");
    // ... same structure as FormAction
}
```

**Step 2**: Create DTO in masterData
```java
// In masterdata/dto/FormStatusDTO.java
@Getter @Setter @Builder
public class FormStatusDTO {
    private String charCode;
    private String statusName;
    private String description;
    // ... other fields
    
    public static FormStatusDTO fromEnum(FormStatus status) {
        // Convert enum to DTO
    }
}
```

**Step 3**: Update MasterDataResponse
```java
@Getter @Setter @Builder
public class MasterDataResponse {
    private List<FormActionDTO> formActions;
    private List<FormStatusDTO> formStatuses;  // NEW
    // private List<AnotherEnumDTO> anotherEnums;
}
```

**Step 4**: Update Service
```java
@Override
public MasterDataResponse getAllMasterData() {
    return MasterDataResponse.builder()
            .formActions(getFormActionsData())
            .formStatuses(getFormStatusesData())  // NEW
            .build();
}

private List<FormStatusDTO> getFormStatusesData() {
    return Arrays.stream(FormStatus.values())
            .map(FormStatusDTO::fromEnum)
            .collect(Collectors.toList());
}
```

**That's it!** The API automatically includes the new enum without breaking existing code.

---

## 🔐 Security Considerations

### Public Access
- Master data endpoints are **public** (no authentication needed)
- Just returns enum values (no sensitive data)
- Safe to cache at CDN level

### To Require Authentication (Optional)
If you want to require authentication:

```java
@GetMapping
@PreAuthorize("isAuthenticated()")  // Add Spring Security annotation
public ResponseEntity<ApiResponse<MasterDataResponse>> getAllMasterData() {
    // ...
}
```

---

## 🚀 Performance Tips

### 1. Frontend Caching
```javascript
const getMasterData = async () => {
  // Cache in localStorage
  const cached = localStorage.getItem('masterData');
  if (cached) return JSON.parse(cached);
  
  const response = await fetch('/api/v1/master-data');
  const data = await response.json();
  localStorage.setItem('masterData', JSON.stringify(data.data));
  return data.data;
};
```

### 2. Backend Caching
```java
@Cacheable(value = "masterData", key = "'all'")
public MasterDataResponse getAllMasterData() {
    // Caches result in memory
    // Invalidate with @CacheEvict when adding new enums
}
```

### 3. Conditional Endpoints
- Use `GET /api/v1/master-data/form-actions` if you only need FormAction
- Reduces payload size
- Better for specific forms

---

## 📊 Data Flow

### Form Submission Flow:

1. **Frontend loads page**
   ```
   GET /api/v1/master-data
   ↓
   Frontend stores formActions in state
   ```

2. **User submits form**
   ```
   Frontend sends:
   {
     "action": "C",  // or code: 1
     "formData": { ... },
     "stepId": "..."
   }
   ```

3. **Backend receives**
   ```
   FormAction.fromCharCode("C") → CREATE
   Use this for business logic
   ```

---

## 🧪 Testing

### Unit Test Example:

```java
@SpringBootTest
class MasterDataServiceTest {
    
    @Autowired
    private MasterDataService masterDataService;
    
    @Test
    void testGetAllMasterData() {
        MasterDataResponse response = masterDataService.getAllMasterData();
        
        assertNotNull(response.getFormActions());
        assertEquals(4, response.getFormActions().size());
        
        // Check FormAction conversion
        FormActionDTO create = response.getFormActions().stream()
            .filter(a -> a.getCode() == 1)
            .findFirst()
            .orElseThrow();
        
        assertEquals("C", create.getCharCode());
        assertEquals("create", create.getActionName());
    }
}
```

### Integration Test:

```java
@SpringBootTest
class MasterDataControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetAllMasterDataEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/master-data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.formActions").isArray())
            .andExpect(jsonPath("$.data.formActions.length()").value(4));
    }
}
```

---

## 📝 Best Practices

### ✅ DO
- Call this API once on app initialization
- Cache the response in frontend state/localStorage
- Use enum values from this API for form submissions
- Add new enums here instead of hardcoding values

### ❌ DON'T
- Call this API on every form render (cache it!)
- Hardcode enum values in frontend
- Store enum data in database (it's configuration, not data)
- Add sensitive information to response

---

## 🔗 Related Modules

- **Form Module**: Uses FormAction enum
- **User Module**: Can add UserRole enum here
- **Movie Module**: Can add MovieGenre, MovieStatus enums here
- **Booking Module**: Can add BookingStatus enum here

---

## 📚 Quick Reference

| Action | Code | CharCode | Description |
|--------|------|----------|-------------|
| CREATE | 1 | C | Create new document |
| READ | 2 | R | View document |
| UPDATE | 3 | U | Update existing document |
| DELETE | 4 | D | Delete document |

---

## 🎯 Next Steps

1. **Frontend Integration**: Update React/Vue forms to use this API
2. **Add More Enums**: Follow the pattern to add FormStatus, UserRole, etc.
3. **Caching**: Implement Redis/Spring Cache for performance
4. **Documentation**: Add to API docs (Swagger/OpenAPI)

---

**Created**: January 10, 2026  
**Module**: masterData  
**Architecture**: Spring Boot 3.x with MongoDB

