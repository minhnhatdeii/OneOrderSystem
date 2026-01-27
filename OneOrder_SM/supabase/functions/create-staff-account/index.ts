// Supabase Edge Function: create-staff-account
// This function uses Admin API to create user and send invitation email

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Create Supabase client with Service Role key
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '', // Admin key
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false
        }
      }
    )

    // Get request body
    const { email, fullName, phone, role, tenantId, createdBy } = await req.json()

    // Validate inputs
    if (!email || !fullName || !tenantId || !createdBy) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Create user with Admin API and default password
    // Password: 123456 (staff can login immediately)
    // Email is auto-confirmed for mobile app (no web verification needed)
    const { data: userData, error: userError } = await supabaseAdmin.auth.admin.createUser({
      email: email,
      password: '123456',  // Default password - staff can change in Profile
      email_confirm: true,  // Auto-confirm email so staff can login immediately
      user_metadata: {
        full_name: fullName,
        phone_number: phone,
      }
    })

    if (userError) {
      console.error('Error creating user:', userError)
      return new Response(
        JSON.stringify({ error: userError.message }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Create or update profile in public.profiles table
    // Note: Database trigger on_auth_user_created may have already created a basic profile
    // So we use upsert to either insert or update with tenant information
    const { error: profileError } = await supabaseAdmin
      .from('profiles')
      .upsert({
        id: userData.user.id,
        tenant_id: tenantId,
        full_name: fullName,
        phone_number: phone,
        role: role,
        is_active: true,
        created_by: createdBy,
      }, {
        onConflict: 'id'  // Update if profile with this id exists
      })

    if (profileError) {
      console.error('Error creating/updating profile:', profileError)
      // Cleanup: delete the created user
      await supabaseAdmin.auth.admin.deleteUser(userData.user.id)

      return new Response(
        JSON.stringify({ error: `Profile creation failed: ${profileError.message}` }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Return success
    return new Response(
      JSON.stringify({
        success: true,
        message: `Staff account created. Default password: 123456. Staff can change password in Profile.`,
        userId: userData.user.id
      }),
      {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )

  } catch (error) {
    console.error('Unexpected error:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
