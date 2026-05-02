-- Insert Demo Data for Food Recommendations

-- 1. Get some existing tenant IDs and menu_item IDs
DO $$
DECLARE
    tenant1 UUID;
    tenant2 UUID;
    menu1 BIGINT;
    menu2 BIGINT;
    menu3 BIGINT;
    post1 UUID := gen_random_uuid();
    post2 UUID := gen_random_uuid();
    post3 UUID := gen_random_uuid();
BEGIN
    -- Assume we have at least one tenant and some menu items.
    -- To avoid failure if tenants table doesn't have data, we just insert into food_posts 
    -- using whatever menu items are available in the public.menu_items table.
    
    SELECT id INTO menu1 FROM menu_items LIMIT 1 OFFSET 0;
    SELECT id INTO menu2 FROM menu_items LIMIT 1 OFFSET 1;
    SELECT id INTO menu3 FROM menu_items LIMIT 1 OFFSET 2;
    
    -- Insert Mock Food Posts
    IF menu1 IS NOT NULL THEN
        INSERT INTO food_posts (id, tenant_id, menu_item_id, images, caption, category_tags, like_count, created_at)
        VALUES (
            post1, 
            NULL, -- We'll assign NULL tenant_id for demo if we don't know it
            menu1, 
            '[{"url": "https://images.unsplash.com/photo-1555126634-323283e090fa?w=800&q=80", "layout": "PORTRAIT"}]'::jsonb,
            'Phở bò đặc biệt thơm ngon!',
            ARRAY['vietnamese', 'noodle', 'beef', 'hot', 'soup'],
            15,
            NOW() - INTERVAL '2 days'
        );
    END IF;

    IF menu2 IS NOT NULL THEN
        INSERT INTO food_posts (id, tenant_id, menu_item_id, images, caption, category_tags, like_count, created_at)
        VALUES (
            post2, 
            NULL, 
            menu2, 
            '[{"url": "https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=800&q=80", "layout": "SQUARE"}]'::jsonb,
            'Bún chả Hà Nội nướng than hoa hấp dẫn.',
            ARRAY['vietnamese', 'pork', 'noodle', 'grilled'],
            45,
            NOW() - INTERVAL '5 hours'
        );
    END IF;

    IF menu3 IS NOT NULL THEN
        INSERT INTO food_posts (id, tenant_id, menu_item_id, images, caption, category_tags, like_count, created_at)
        VALUES (
            post3, 
            NULL, 
            menu3, 
            '[{"url": "https://images.unsplash.com/photo-1611143669185-af224c5e3252?w=800&q=80", "layout": "LANDSCAPE"}]'::jsonb,
            'Gỏi cuốn mùa hè tươi mát.',
            ARRAY['vietnamese', 'fresh', 'shrimp', 'vegetables', 'healthy'],
            120,
            NOW() - INTERVAL '10 minutes'
        );
    END IF;
    
    RAISE NOTICE 'Demo food posts inserted.';
END $$;
