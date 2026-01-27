# Edge Function Deployment Guide

## Bước 1: Cài đặt Supabase CLI

```bash
# Windows (PowerShell)
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase

# Mac
brew install supabase/tap/supabase

# Hoặc dùng npm
npm install -g supabase
```

## Bước 2: Login vào Supabase

```bash
supabase login
```

## Bước 3: Link với project

```bash
cd c:\MyCode\DuAnCongNghe\OneOrder_SM
supabase link --project-ref YOUR_PROJECT_REF
```

## Bước 4: Deploy Edge Function

```bash
supabase functions deploy create-staff-account
```

## Bước 5: Set secrets (Environment variables)

Function cần Service Role Key:

```bash
# Get Service Role Key từ Supabase Dashboard:
# Settings > API > service_role (secret)

supabase secrets set SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here
```

## Bước 6: Test function

```bash
curl -L -X POST 'https://YOUR_PROJECT_REF.supabase.co/functions/v1/create-staff-account' \
  -H 'Authorization: Bearer YOUR_ANON_KEY' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "test@example.com",
    "fullName": "Test Staff",
    "phone": "0123456789",
    "role": "staff",
    "tenantId": "your-tenant-id",
    "createdBy": "manager-user-id"
  }'
```

## Bước 7: Config Email (Quan trọng!)

Vào Supabase Dashboard:
1. **Authentication** > **Email Templates**
2. **Confirm signup** template - Customize email template
3. **SMTP Settings** (nếu muốn dùng email riêng):
   - Settings > Auth > SMTP Settings
   - Hoặc dùng Supabase default email

## URL của Edge Function

```
https://YOUR_PROJECT_REF.supabase.co/functions/v1/create-staff-account
```

Thêm URL này vào `SupabaseConfig` trong app.
