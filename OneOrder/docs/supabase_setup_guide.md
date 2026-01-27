# Supabase Setup Guide

This guide will help you set up the Supabase backend for the **OneOrder** system and apply the generated database schema.

## Prerequisites
- A [Supabase Account](https://supabase.com/).
- The [supabase_schema.sql](file:///C:/Users/Acer/.gemini/antigravity/brain/76430431-0852-409a-b71f-3dace623ca2f/supabase_schema.sql) file provided in this project.

## Step 1: Create a New Project
1.  Log in to your Supabase Dashboard.
2.  Click **"New Project"**.
3.  Choose your Organization.
4.  **Name**: `OneOrder` (or similar).
5.  **Database Password**: Generate a strong password and save it securely.
6.  **Region**: Choose a region close to your users (e.g., Singapore for Vietnam).
7.  Click **"Create new project"**.
8.  Wait a few minutes for the project to finish provisioning.

## Step 2: Apply Database Schema
Once the project is active:
1.  Go to the **SQL Editor** (icon on the left sidebar).
2.  Click **"+ New Query"**.
3.  Copy the entire content of `docs/supabase_schema.sql` (or [open it here](file:///C:/Users/Acer/.gemini/antigravity/brain/76430431-0852-409a-b71f-3dace623ca2f/supabase_schema.sql)).
4.  Paste it into the SQL Editor.
5.  Click **"Run"** (bottom right).
6.  Ensure the results show "Success" and no errors.

## Step 3: Configure Storage
The app requires storage buckets for menu images and user avatars.
1.  Go to **Storage** (icon on the left).
2.  Click **"New Bucket"**.
3.  **Name**: `menu_items`.
4.  **Public**: Toggle **ON** (Menu images need to be public).
5.  Click **"Save"**.
6.  Create another bucket named `avatars`.
    - **Public**: Toggle **ON** (User avatars usually public or signed urls, we start with public for simplicity).

## Step 4: Get API Credentials
You need these to connect your Android apps.
1.  Go to **Settings** (Project Settings) > **API**.
2.  Copy the **Project URL**.
3.  Copy the **anon** / **public** key.
    > [!WARNING]
    > Do NOT share the `service_role` key in your mobile app code. Only use the `anon` key.

## Step 5: Verify Setup
1.  Go to the **Table Editor**.
2.  Check that the following tables exist: `profiles`, `categories`, `menu_items`, `tables`, `orders`, `order_items`, `idempotency_keys`.
3.  You are now ready to connect your app!
