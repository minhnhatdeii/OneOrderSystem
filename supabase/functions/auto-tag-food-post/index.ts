import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { GoogleGenerativeAI } from "npm:@google/generative-ai";

// Khởi tạo Supabase Client với Service Role để có quyền Update Database
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const supabase = createClient(supabaseUrl, supabaseKey);

// Khởi tạo Gemini AI (Bạn cần cấu hình GEMINI_API_KEY trong Supabase Secrets)
const genAI = new GoogleGenerativeAI(Deno.env.get("GEMINI_API_KEY")!);

const ALLOWED_TAXONOMY = `
- cuisine: vietnamese, korean, japanese, chinese, western, thai, vegetarian
- flavor: spicy, sweet, sour, salty, bitter, savory, creamy
- protein: beef, pork, chicken, seafood, fish, tofu, egg
- cooking_method: fried, grilled, boiled, steamed, raw, baked, soup
- dish_type: rice, noodle, bread, sandwich, salad, dessert, drink, snack
`;

const SYSTEM_PROMPT = `
You are an expert food classification AI. Your task is to read the food name, description, and ingredients, then classify it strictly using the ALLOWED DICTIONARY below.
YOU MUST STRICTLY FOLLOW THESE RULES:
1. ONLY use tags from the ALLOWED DICTIONARY. Absolutely DO NOT invent new tags.
2. If the food doesn't have information for a category, simply omit that category.
3. OUTPUT FORMAT: You must return ONLY a single string in the exact format: category1:tag1+tag2|category2:tag3.
4. DO NOT use markdown, DO NOT add explanations, DO NOT add extra spaces.

ALLOWED DICTIONARY:
${ALLOWED_TAXONOMY}

Example of correct output:
cuisine:vietnamese|flavor:spicy+savory|protein:beef|cooking_method:soup|dish_type:noodle
`;

serve(async (req) => {
    try {
        const payload = await req.json();
        
        // Supabase Database Webhook (Trigger) sẽ gửi dữ liệu trong biến "record"
        // Nếu bạn gọi API này trực tiếp từ App, nó sẽ lấy thẳng từ body
        const post = payload.record || payload; 
        
        const postId = post.id;
        const postTitle = post.title || post.name; // Đổi lại "title" hay "name" tùy cột tên món ăn của bạn
        const postDesc = post.description || post.content; // Đổi lại tùy cột mô tả món ăn
        
        if (!postId || !postTitle) {
            return new Response("Thiếu dữ liệu bài đăng", { status: 400 });
        }

        console.log(`Bắt đầu phân loại Tag cho Post: ${postTitle}`);

        const model = genAI.getGenerativeModel({ 
            model: "gemini-1.5-flash",
            systemInstruction: SYSTEM_PROMPT
        });
        
        const prompt = `Food Name: ${postTitle}\nDescription: ${postDesc || 'No description'}`;
        
        // Gọi Gemini xử lý
        const result = await model.generateContent(prompt);
        const tags = result.response.text().trim();
        
        console.log(`Generated Tags: ${tags}`);

        // Lưu ngược lại chuỗi tags vào Database
        const { error } = await supabase
            .from('posts') // THAY BẰNG TÊN BẢNG POST CỦA BẠN (VD: menu_items, restaurant_posts...)
            .update({ tags: tags })
            .eq('id', postId);

        if (error) throw error;

        return new Response(JSON.stringify({ success: true, tags: tags }), {
            headers: { "Content-Type": "application/json" }
        });

    } catch (error) {
        console.error("Lỗi:", error);
        return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    }
});
