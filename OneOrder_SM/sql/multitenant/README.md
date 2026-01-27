# Multi-Tenant SQL Migration Scripts

This directory contains SQL scripts to transform OneOrder_SM into a multi-tenant system.

## Execution Order

Run these scripts in Supabase SQL Editor in the following order:

### 1. `01_create_schema.sql`
- Creates the `tenants` table
- Adds `tenant_id` column to all relevant tables (profiles, categories, menu_items, tables, orders)
- Creates helper functions: `get_user_tenant_id()`, `get_user_role()`, `is_tenant_manager()`

### 2. `02_rls_policies.sql`
- Drops existing RLS policies
- Creates new multi-tenant aware RLS policies
- Ensures data isolation between restaurants

### 3. `03_functions.sql`
- Creates database functions for tenant management:
  - `create_restaurant_account()` - Register new restaurant
  - `get_tenant_info()` - Get current tenant info
  - `update_tenant_info()` - Update restaurant settings
  - `get_tenant_staff()` - List staff for current tenant
  - `deactivate_staff()` / `reactivate_staff()` - Manage staff status
  - `get_order_statistics()` - Order statistics for tenant
  - `get_popular_items()` - Popular items for tenant
  - `get_dashboard_summary()` - Dashboard summary for tenant
- Creates update triggers for `updated_at` columns

### 4. `04_staff_invitations.sql`
- Creates `staff_invitations` table for invitation-based staff onboarding
- Creates functions:
  - `create_staff_invitation()` - Create invitation
  - `accept_staff_invitation()` - Accept invitation (link to tenant)
  - `get_pending_invitations()` - List pending invitations
  - `cancel_invitation()` - Cancel an invitation

## Edge Function

The `supabase/functions/create-staff/` directory contains a Deno Edge Function that:
- Creates auth.users entry using admin API
- Creates profile with tenant association
- Handles authorization (only managers can create staff)

Deploy with:
```bash
supabase functions deploy create-staff
```

## After Running Scripts

1. Test by creating a new user account
2. Call `create_restaurant_account('Restaurant Name', 'Address', 'Phone', 'Email')`
3. Verify tenant was created and profile was updated
4. Create test menu items, categories, tables
5. Create a staff account and verify data isolation

## Rollback

If you need to revert:
1. Drop the new columns and tables
2. Restore old RLS policies from backup
3. Or restore from database backup taken before migration
