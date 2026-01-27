# Activity Diagrams

## OneOrder (Customer App)

### 1. Login/Register
User enters the app. If not logged in, they are prompted to login or register.

```mermaid
graph TD
    Start((Start)) --> CheckLogin{Is Logged In?}
    CheckLogin -- Yes --> HomePage[Home Page]
    CheckLogin -- No --> LoginPage[Login Page]
    LoginPage --> InputCreds[Input Credentials]
    InputCreds --> Validate{Validate?}
    Validate -- Valid --> HomePage
    Validate -- Invalid --> Error[Show Error]
    Error --> InputCreds
    LoginPage --> Register[Register New Account]
    Register --> InputDetails[Input User Details]
    InputDetails --> SubmitReg[Submit Registration]
    SubmitReg --> RegSuccess{Success?}
    RegSuccess -- Yes --> HomePage
    RegSuccess -- No --> RegError[Show Error]
    RegError --> InputDetails
```

### 2. Home/Menu Browsing
User scans a QR code (simulated or real) to identify table, then browses the menu.

```mermaid
graph TD
    HomePage --> ScanQR[Scan QR / Enter Table ID]
    ScanQR --> FetchMenu[Fetch Menu for Table]
    FetchMenu --> DisplayMenu[Display Menu Categories]
    DisplayMenu --> SelectCategory[Select Category]
    SelectCategory --> ShowItems[Show Items List]
    ShowItems --> ViewDetails[View Item Details]
    ViewDetails --> AddToCart[Add to Cart]
    AddToCart --> ContinueShopping{Continue?}
    ContinueShopping -- Yes --> DisplayMenu
    ContinueShopping -- No --> ViewCart[View Cart]
```

### 3. Cart/Order Placement
User reviews cart and places order.

```mermaid
graph TD
    ViewCart --> ReviewItems[Review Items]
    ReviewItems --> UpdateQty[Update Quantity / Remove]
    UpdateQty --> ReviewItems
    ReviewItems --> Checkout[Proceed to Checkout]
    Checkout --> ConfirmOrder{Confirm Order?}
    ConfirmOrder -- Yes --> SendOrder[Send Order to Backend]
    SendOrder --> OrderPlaced{Success?}
    OrderPlaced -- Yes --> OrderSuccess[Show Success / Track Order]
    OrderPlaced -- No --> OrderFail[Show Error]
    OrderFail --> ReviewItems
```

### 4. Order History/Tracking
User tracks the status of their active order.

```mermaid
graph TD
    OrderSuccess --> ViewStatus[View Order Status]
    ViewStatus --> PollStatus{Check Status}
    PollStatus -- Pending --> Wait[Wait]
    PollStatus -- Confirmed --> ShowConfirmed[Show Confirmed]
    PollStatus -- Preparing --> ShowPrep[Show Preparing]
    PollStatus -- Ready --> ShowReady[Show Served/Ready]
    ShowReady --> FinishEat((Finish))
```

### 5. Profile Management
User views and updates their profile.

```mermaid
graph TD
    HomePage --> UserProfile[User Profile]
    UserProfile --> ViewInfo[View Info]
    ViewInfo --> EditInfo[Edit Info]
    EditInfo --> SaveChanges[Save Changes]
    SaveChanges --> UpdateResult{Success?}
    UpdateResult -- Yes --> ViewInfo
    UpdateResult -- No --> ShowUpdateError[Show Error]
    UserProfile --> Logout[Logout]
    Logout --> LoginPage
```

---

## OneOrder_SM (Staff/Manager App)

### 1. Login
Staff logs in with credentials.

```mermaid
graph TD
    Start((Start)) --> StaffLogin[Staff Login Page]
    StaffLogin --> InputStaffCreds[Input Credentials]
    InputStaffCreds --> VerifyStaff{Verify?}
    VerifyStaff -- Valid --> CheckRole{Check Role}
    CheckRole -- Manager --> Dashboard[Manager Dashboard]
    CheckRole -- Staff --> OrderView[Order Management]
    VerifyStaff -- Invalid --> AuthError[Show Error]
    AuthError --> InputStaffCreds
```

### 2. Dashboard/Stats (Manager)
Manager views usage statistics.

```mermaid
graph TD
    Dashboard --> FetchStats[Fetch Daily Stats]
    FetchStats --> ShowGraphs[Show Graphs/Metrics]
    ShowGraphs --> FilterDate[Filter by Date]
    FilterDate --> UpdateStats[Update View]
```

### 3. Order Management
Staff views and updates order status.

```mermaid
graph TD
    OrderView --> ListOrders[List Active Orders]
    ListOrders --> SelectOrder[Select Order]
    SelectOrder --> ViewOrderDetails[View Details]
    ViewOrderDetails --> ChangeStatus{Change Status}
    ChangeStatus -- Confirm --> UpdateConfirmed[Set Confirmed]
    ChangeStatus -- Cook --> UpdateCooking[Set Cooking]
    ChangeStatus -- Serve --> UpdateServed[Set Served]
    ChangeStatus -- Cancel --> UpdateCancelled[Set Cancelled]
    UpdateConfirmed --> NotifyUser[Notify Customer]
    UpdateCooking --> NotifyUser
    UpdateServed --> NotifyUser
    UpdateCancelled --> NotifyUser
```

### 4. Table Management
Staff manages table availability.

```mermaid
graph TD
    Dashboard --> TableView[Table Management]
    TableView --> ListTables[List Tables]
    ListTables --> SelectTable[Select Table]
    SelectTable --> CheckStatus{Current Status}
    CheckStatus -- Free --> MarkOccupied[Mark Occupied]
    CheckStatus -- Occupied --> MarkFree[Mark Free]
    MarkOccupied --> UpdateDB[Update Database]
    MarkFree --> UpdateDB
```

### 5. Menu Management (Manager)
Manager adds/edits menu items.

```mermaid
graph TD
    Dashboard --> MenuMgmt[Menu Management]
    MenuMgmt --> ListMenuItems[List Items]
    ListMenuItems --> AddItem[Add New Item]
    AddItem --> InputItemDetails[Input Details & Image]
    InputItemDetails --> SaveItem[Save]
    ListMenuItems --> EditItem[Edit Existing Item]
    EditItem --> UpdateItemDetails[Update Details]
    UpdateItemDetails --> SaveUpdates[Save]
    ListMenuItems --> DeleteItem[Delete Item]
```

### 6. Staff Management (Manager)
Manager adds/removes staff.

```mermaid
graph TD
    Dashboard --> StaffMgmt[Staff Management]
    StaffMgmt --> ListStaff[List Staff Members]
    ListStaff --> AddStaff[Add New Staff]
    AddStaff --> InputStaffInfo[Input Info & Role]
    InputStaffInfo --> CreateAccount[Create Account]
    ListStaff --> RemoveStaff[Remove/Deactivate Staff]
    RemoveStaff --> ConfirmRemove[Confirm]
    ConfirmRemove --> Deactivate[Deactivate Account]
```
