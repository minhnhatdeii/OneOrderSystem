-- ============================================================================
-- SUPABASE EDGE FUNCTION: Create Staff Account
-- Deploy with: supabase functions deploy create-staff
-- ============================================================================

-- This file is for documentation. The actual Edge Function is in TypeScript.
-- File: supabase/functions/create-staff/index.ts

-- The Edge Function is needed because:
-- 1. Creating auth.users requires service_role key (admin privileges)
-- 2. We need to create both auth.users entry AND profiles entry atomically
-- 3. Regular users cannot call auth.admin.createUser()

-- ============================================================================
-- ALTERNATIVE: Use Supabase Auth Admin API directly
-- If you prefer not to use Edge Functions, you can:
-- 1. Use the Supabase Dashboard to manually create staff accounts
-- 2. Use the supabase-js admin client in a trusted server environment
-- ============================================================================

-- ============================================================================
-- SQL-ONLY APPROACH (Limited)
-- This function can be called by managers to "prepare" a staff invitation
-- The actual user creation still needs Edge Function or Dashboard
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.staff_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    full_name TEXT NOT NULL,
    phone_number TEXT,
    role TEXT NOT NULL DEFAULT 'staff' CHECK (role IN ('staff', 'manager')),
    invitation_token UUID DEFAULT gen_random_uuid(),
    expires_at TIMESTAMPTZ DEFAULT now() + INTERVAL '7 days',
    accepted_at TIMESTAMPTZ,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Index for fast token lookup
CREATE INDEX IF NOT EXISTS idx_staff_invitations_token ON public.staff_invitations(invitation_token);
CREATE INDEX IF NOT EXISTS idx_staff_invitations_tenant ON public.staff_invitations(tenant_id);

-- Enable RLS
ALTER TABLE public.staff_invitations ENABLE ROW LEVEL SECURITY;

-- Managers can create invitations for their tenant
CREATE POLICY "Managers can create invitations"
ON public.staff_invitations FOR INSERT
WITH CHECK (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- Managers can view invitations for their tenant
CREATE POLICY "Managers can view tenant invitations"
ON public.staff_invitations FOR SELECT
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- ============================================================================
-- FUNCTION: Create Staff Invitation
-- Manager creates an invitation, staff can sign up using the invitation token
-- ============================================================================

CREATE OR REPLACE FUNCTION public.create_staff_invitation(
    p_email TEXT,
    p_full_name TEXT,
    p_phone TEXT DEFAULT NULL,
    p_role TEXT DEFAULT 'staff'
) RETURNS UUID AS $$
DECLARE
    v_tenant_id UUID;
    v_user_role TEXT;
    v_invitation_id UUID;
BEGIN
    -- Get caller's tenant and role
    SELECT tenant_id, role INTO v_tenant_id, v_user_role
    FROM public.profiles
    WHERE id = auth.uid();
    
    -- Verify caller is manager
    IF v_user_role != 'manager' THEN
        RAISE EXCEPTION 'Only managers can create staff invitations';
    END IF;
    
    -- Validate role
    IF p_role NOT IN ('staff', 'manager') THEN
        RAISE EXCEPTION 'Invalid role. Must be staff or manager';
    END IF;
    
    -- Check if invitation already exists for this email
    IF EXISTS (
        SELECT 1 FROM public.staff_invitations
        WHERE tenant_id = v_tenant_id
        AND email = p_email
        AND accepted_at IS NULL
        AND expires_at > now()
    ) THEN
        RAISE EXCEPTION 'Pending invitation already exists for this email';
    END IF;
    
    -- Create invitation
    INSERT INTO public.staff_invitations (
        tenant_id,
        email,
        full_name,
        phone_number,
        role,
        created_by
    )
    VALUES (
        v_tenant_id,
        p_email,
        p_full_name,
        p_phone,
        p_role,
        auth.uid()
    )
    RETURNING id INTO v_invitation_id;
    
    RETURN v_invitation_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.create_staff_invitation(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- FUNCTION: Accept Staff Invitation
-- Called after user signs up, to link them to the tenant
-- ============================================================================

CREATE OR REPLACE FUNCTION public.accept_staff_invitation(
    p_invitation_token UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_invitation RECORD;
    v_user_id UUID;
BEGIN
    v_user_id := auth.uid();
    
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    
    -- Get invitation
    SELECT * INTO v_invitation
    FROM public.staff_invitations
    WHERE invitation_token = p_invitation_token
    AND accepted_at IS NULL
    AND expires_at > now();
    
    IF v_invitation IS NULL THEN
        RAISE EXCEPTION 'Invalid or expired invitation';
    END IF;
    
    -- Check if user already belongs to a tenant
    IF EXISTS (SELECT 1 FROM public.profiles WHERE id = v_user_id AND tenant_id IS NOT NULL) THEN
        RAISE EXCEPTION 'User already belongs to a restaurant';
    END IF;
    
    -- Update user profile
    UPDATE public.profiles
    SET 
        tenant_id = v_invitation.tenant_id,
        role = v_invitation.role,
        full_name = v_invitation.full_name,
        phone_number = v_invitation.phone_number,
        created_by = v_invitation.created_by,
        updated_at = now()
    WHERE id = v_user_id;
    
    -- Mark invitation as accepted
    UPDATE public.staff_invitations
    SET accepted_at = now()
    WHERE id = v_invitation.id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.accept_staff_invitation(UUID) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Pending Invitations
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_pending_invitations()
RETURNS TABLE (
    id UUID,
    email TEXT,
    full_name TEXT,
    phone_number TEXT,
    role TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ
) AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    RETURN QUERY
    SELECT 
        si.id,
        si.email,
        si.full_name,
        si.phone_number,
        si.role,
        si.expires_at,
        si.created_at
    FROM public.staff_invitations si
    WHERE si.tenant_id = v_tenant_id
    AND si.accepted_at IS NULL
    AND si.expires_at > now()
    ORDER BY si.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_pending_invitations() TO authenticated;

-- ============================================================================
-- FUNCTION: Cancel Invitation
-- ============================================================================

CREATE OR REPLACE FUNCTION public.cancel_invitation(p_invitation_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_tenant_id UUID;
    v_invitation_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    -- Get invitation's tenant
    SELECT tenant_id INTO v_invitation_tenant_id
    FROM public.staff_invitations
    WHERE id = p_invitation_id;
    
    -- Verify invitation belongs to caller's tenant
    IF v_invitation_tenant_id != v_tenant_id THEN
        RAISE EXCEPTION 'Invitation does not belong to your restaurant';
    END IF;
    
    -- Delete invitation
    DELETE FROM public.staff_invitations
    WHERE id = p_invitation_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.cancel_invitation(UUID) TO authenticated;
