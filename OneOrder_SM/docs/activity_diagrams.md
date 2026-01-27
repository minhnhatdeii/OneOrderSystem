# Activity Diagrams

## OneOrder (Customer App)

### 1. Login/Register
```mermaid
graph TD
    Start((Start)) --> CheckLogin{Is Logged In?}
    CheckLogin -- Yes --> ScanQR[Scan Restaurant QR]
    CheckLogin -- No --> LoginPage[Login Page]
    LoginPage --> InputCreds[Input Credentials]
    InputCreds --> Validate{Validate?}
    Validate -- Valid --> ScanQR
    Validate -- Invalid --> Error[Show Error]
    LoginPage --> Register[Register New Account]
    Register --> HomePage
```

### 2. Menu Browsing (via QR)
QR code contains tenant_id to identify the restaurant.
```mermaid
graph TD
    ScanQR[Scan Restaurant QR] --> ExtractTenant[Extract tenant_id from QR]
    ExtractTenant --> FetchMenu[Fetch Menu for Tenant]
    FetchMenu --> DisplayMenu[Display Categories]
    DisplayMenu --> SelectCategory[Select Category]
    SelectCategory --> ShowItems[Show Items]
    ShowItems --> AddToCart[Add to Cart]
```

### 3. Order Placement
```mermaid
graph TD
    ViewCart --> ReviewItems[Review Items]
    ReviewItems --> Checkout[Checkout]
    Checkout --> SendOrder[Send Order with tenant_id]
    SendOrder --> OrderSuccess[Order Placed]
```

---

## OneOrder_SM (Staff/Manager App)

### 1. App Entry Flow (Multi-Tenant)
```mermaid
graph TD
    Start((Start)) --> WelcomeScreen[Welcome Screen]
    WelcomeScreen --> RegisterOption[Register Restaurant]
    WelcomeScreen --> LoginOption[Login as Staff]
    
    RegisterOption --> RegisterPage[Registration Form]
    RegisterPage --> CreateAccount[Create Account]
    CreateAccount --> CreateTenant[Create Restaurant/Tenant]
    CreateTenant --> SetManager[Set User as Manager]
    SetManager --> Dashboard[Dashboard]
    
    LoginOption --> LoginPage[Login Page]
    LoginPage --> Validate{Valid Credentials?}
    Validate -- Yes --> CheckTenant{Has Tenant?}
    CheckTenant -- Yes --> Dashboard
    CheckTenant -- No --> InvitationPrompt[Enter Invitation Code]
    Validate -- No --> Error[Show Error]
```

### 2. Restaurant Registration
```mermaid
graph TD
    RegisterPage --> InputAccount[Input Email/Password/Name]
    InputAccount --> InputRestaurant[Input Restaurant Details]
    InputRestaurant --> Submit[Submit]
    Submit --> CreateAuth[Create Auth User]
    CreateAuth --> CreateProfile[Create Profile]
    CreateProfile --> CallRPC[RPC: create_restaurant_account]
    CallRPC --> CreateTenant[Create Tenant Record]
    CreateTenant --> UpdateProfile[Update Profile: role=manager, tenant_id]
    UpdateProfile --> Success[Registration Complete]
```

### 3. Staff Invitation System
```mermaid
graph TD
    subgraph "Manager Actions"
        StaffMgmt[Staff Management] --> CreateInvite[Create Invitation]
        CreateInvite --> InputInfo[Input Email/Name/Role]
        InputInfo --> SaveInvite[Save to staff_invitations]
        SaveInvite --> ShareToken[Share Invitation Token]
    end
    
    subgraph "Staff Actions"
        NewStaff[New Staff] --> Register[Register Account]
        Register --> EnterToken[Enter Invitation Token]
        EnterToken --> AcceptInvite[Accept Invitation]
        AcceptInvite --> LinkTenant[Link to Tenant]
        LinkTenant --> SetRole[Set Role from Invitation]
        SetRole --> Ready[Ready to Work]
    end
```

### 4. Role-Based Navigation
```mermaid
graph TD
    Login --> CheckRole{User Role?}
    CheckRole -- Manager --> ManagerDash[Full Dashboard]
    CheckRole -- Staff --> StaffDash[Limited Dashboard]
    
    ManagerDash --> Orders[Order Management]
    ManagerDash --> Menu[Menu Management]
    ManagerDash --> Tables[Table Management]
    ManagerDash --> Staff[Staff Management]
    ManagerDash --> Settings[Restaurant Settings]
    
    StaffDash --> Orders
    StaffDash --> Tables
```

### 5. Order Management
```mermaid
graph TD
    OrderView --> ListOrders[List Tenant Orders]
    ListOrders --> SelectOrder[Select Order]
    SelectOrder --> ViewDetails[View Details]
    ViewDetails --> ChangeStatus{Change Status}
    ChangeStatus -- Confirm --> SetConfirmed
    ChangeStatus -- Prepare --> SetPreparing
    ChangeStatus -- Serve --> SetServed
    ChangeStatus -- Cancel --> SetCancelled
```

### 6. Table Management
```mermaid
graph TD
    TableView --> ListTables[List Tenant Tables]
    ListTables --> SelectTable[Select Table]
    SelectTable --> ToggleStatus[Toggle Free/Occupied]
    ListTables --> AddTable[Add New Table]
    AddTable --> InputNumber[Input Table Number]
    InputNumber --> Save[Save]
    SelectTable --> GenerateQR[Generate QR Code]
```

### 7. Menu Management (Manager Only)
```mermaid
graph TD
    MenuMgmt --> Categories[Manage Categories]
    MenuMgmt --> Items[Manage Items]
    Categories --> AddCategory[Add Category]
    Categories --> EditCategory[Edit Category]
    Items --> AddItem[Add Item]
    Items --> EditItem[Edit Item]
    Items --> ToggleAvail[Toggle Availability]
```

### 8. Staff Management (Manager Only)
```mermaid
graph TD
    StaffMgmt --> ViewStaff[View Staff List]
    StaffMgmt --> ViewInvites[View Pending Invitations]
    ViewStaff --> Deactivate[Deactivate Staff]
    ViewStaff --> Reactivate[Reactivate Staff]
    ViewInvites --> CancelInvite[Cancel Invitation]
    StaffMgmt --> CreateInvite[Create New Invitation]
```

---

## Data Flow with Multi-Tenancy

### RLS Enforcement
```mermaid
graph LR
    User --> Query[Query Data]
    Query --> RLS[RLS Policy Check]
    RLS --> GetTenant[get_user_tenant_id]
    GetTenant --> Filter[Filter by tenant_id]
    Filter --> Results[Return Filtered Data]
```

### Cross-Tenant Protection
- All queries automatically filtered by `tenant_id`
- Manager can only see their restaurant's data
- Staff can only access assigned restaurant
- Customers see menu from scanned QR's tenant

