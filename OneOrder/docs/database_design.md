# Database Design: OneOrder System

## System Overview
The system uses **Supabase (PostgreSQL)** as the backend. It serves two applications:
1.  **OneOrder**: Customer facing (Menu, Cart, Orders).
2.  **OneOrder_SM**: Staff/Manager facing (Order Mgmt, Menu Mgmt, Stats).

## ER Diagram
```mermaid
erDiagram
    PROFILES {
        uuid id PK "References auth.users"
        string role "customer, staff, manager"
        string full_name
        string phone_number
        timestamp created_at
    }

    CATEGORIES {
        int id PK
        string name
        string image_url
        boolean is_active
    }

    MENU_ITEMS {
        int id PK
        int category_id FK
        string name
        string desc
        decimal price
        string image_url
        boolean is_available
    }

    TABLES {
        int id PK
        string table_number
        string qr_code
        string status "free, occupied"
    }

    ORDERS {
        uuid id PK
        uuid user_id FK
        int table_id FK
        decimal total_amount
        string status "pending, confirmed, preparing, served, cancelled, paid"
        string payment_status "unpaid, paid"
        uuid idempotency_key "Unique for pattern"
        timestamp created_at
    }

    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        int menu_item_id FK
        int quantity
        decimal price_at_time
        string note
    }

    IDEMPOTENCY_KEYS {
        uuid key PK
        uuid user_id FK
        jsonb response_payload
        timestamp created_at
    }

    CATEGORIES ||--|{ MENU_ITEMS : contains
    PROFILES ||--|{ ORDERS : places
    TABLES ||--|{ ORDERS : "associated with"
    ORDERS ||--|{ ORDER_ITEMS : contains
    MENU_ITEMS ||--|{ ORDER_ITEMS : "included in"
    PROFILES ||--|{ IDEMPOTENCY_KEYS : generates
```

## Table Descriptions

### Authentication & Profiles
- **profiles**: Extends the default Supabase `auth.users` table. Stores application-specific user data like role (`manager`, `staff`, `customer`).

### Menu System
- **categories**: Groups menu items (e.g., "Drinks", "Main Course").
- **menu_items**: The actual food/drink items. Linked to categories.

### Restaurant Operations
- **tables**: Physical tables in the restaurant. Contains status to help staff manage occupancy.

### Ordering System
- **orders**: The core transaction table.
    - `status`: Tracks the lifecycle (Pending -> ... -> Served).
    - `idempotency_key`: Used to prevent duplicate orders.
- **order_items**: The line items for each order (snapshots price at time of order).

### Reliability
- **idempotency_keys**: Stores keys and responses to handle network retries gracefully.
