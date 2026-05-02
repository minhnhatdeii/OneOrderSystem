-- Add missing policies for food_posts table so that restaurants can insert/update/delete their posts

-- 1. Policy for INSERT
DROP POLICY IF EXISTS "Staff and managers can insert food posts" ON food_posts;
CREATE POLICY "Staff and managers can insert food posts" 
ON food_posts FOR INSERT 
WITH CHECK (
    auth.uid() IN (
        SELECT id FROM profiles WHERE tenant_id = food_posts.tenant_id
    )
);

-- 2. Policy for UPDATE
DROP POLICY IF EXISTS "Staff and managers can update their food posts" ON food_posts;
CREATE POLICY "Staff and managers can update their food posts" 
ON food_posts FOR UPDATE 
USING (
    auth.uid() IN (
        SELECT id FROM profiles WHERE tenant_id = food_posts.tenant_id
    )
);

-- 3. Policy for DELETE
DROP POLICY IF EXISTS "Staff and managers can delete their food posts" ON food_posts;
CREATE POLICY "Staff and managers can delete their food posts" 
ON food_posts FOR DELETE 
USING (
    auth.uid() IN (
        SELECT id FROM profiles WHERE tenant_id = food_posts.tenant_id
    )
);
