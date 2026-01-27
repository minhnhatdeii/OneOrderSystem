-- Function to create staff account directly (no invitation needed)
-- Manager calls this to create immediate account for staff with temp password
CREATE OR REPLACE FUNCTION create_staff_account(
    p_email TEXT,
    p_password TEXT,
    p_full_name TEXT,
    p_phone TEXT DEFAULT NULL,
    p_role TEXT DEFAULT 'staff'
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_manager_id UUID;
    v_tenant_id UUID;
    v_new_user_id UUID;
BEGIN
    -- Get current user (must be manager)
    v_manager_id := auth.uid();
    IF v_manager_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    
    -- Get manager's tenant_id
    SELECT tenant_id INTO v_tenant_id
    FROM profiles
    WHERE id = v_manager_id;
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'Manager has no tenant';
    END IF;
    
    -- Check if manager has permission (must be manager role)
    IF NOT EXISTS (
        SELECT 1 FROM profiles
        WHERE id = v_manager_id 
        AND role = 'manager'
        AND is_active = true
    ) THEN
        RAISE EXCEPTION 'Only active managers can create staff accounts';
    END IF;
    
    -- Create user in auth.users (requires service_role or admin API)
    -- This uses Supabase admin function
    v_new_user_id := extensions.uuid_generate_v4();
    
    INSERT INTO auth.users (
        id,
        instance_id,
        email,
        encrypted_password,
        email_confirmed_at,
        raw_app_meta_data,
        raw_user_meta_data,
        created_at,
        updated_at,
        confirmation_token,
        email_change,
        email_change_token_new,
        recovery_token
    ) VALUES (
        v_new_user_id,
        '00000000-0000-0000-0000-000000000000',
        p_email,
        crypt(p_password, gen_salt('bf')),
        NOW(),
        '{"provider": "email", "providers": ["email"]}',
        jsonb_build_object('full_name', p_full_name),
        NOW(),
        NOW(),
        '',
        '',
        '',
        ''
    );
    
    -- Create profile for the new user
    INSERT INTO profiles (
        id,
        tenant_id,
        full_name,
        phone_number,
        role,
        is_active,
        created_by
    ) VALUES (
        v_new_user_id,
        v_tenant_id,
        p_full_name,
        p_phone,
        p_role,
        true,
        v_manager_id
    );
    
END;
$$;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION create_staff_account TO authenticated;
