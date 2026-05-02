import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// Khởi tạo Supabase client với Service Role Key để có quyền thao tác trên toàn bộ DB
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const supabase = createClient(supabaseUrl, supabaseKey);

// Hàm hỗ trợ tính toán Cosine Similarity
function getCosineSimilarity(ratings1: Record<string, number>, ratings2: Record<string, number>) {
    let dotProduct = 0, norm1 = 0, norm2 = 0;
    for (const [pid, w1] of Object.entries(ratings1)) {
        norm1 += w1 * w1;
        if (ratings2[pid]) dotProduct += w1 * ratings2[pid];
    }
    for (const w2 of Object.values(ratings2)) norm2 += w2 * w2;
    if (norm1 === 0 || norm2 === 0) return 0;
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}

serve(async (req) => {
    try {
        console.log("Bắt đầu chạy Cronjob cập nhật User Profiles...");

        // 1. Lấy dữ liệu tương tác trong 30 ngày gần nhất
        // Giả sử bảng posts lưu cột tags dưới dạng text "cuisine:vietnamese|flavor:spicy"
        const { data: logs, error: logsError } = await supabase
            .from("interactions_log")
            .select("user_id, post_id, action_weight");

        if (logsError) throw logsError;

        // Lấy thông tin tags của các post đã được tương tác
        const postIds = [...new Set(logs.map(l => l.post_id))];
        const { data: postsData } = await supabase
            .from("posts") // THAY TÊN BẢNG POST CỦA BẠN VÀO ĐÂY
            .select("id, tags")
            .in("id", postIds);

        const postMap = new Map(postsData?.map(p => [p.id, p]));

        // 2. Tính Tag Profile & Rating Vector cho từng User
        const userRatings: Record<string, Record<string, number>> = {};
        const userTagProfiles: Record<string, any> = {};

        for (const log of logs) {
            const uid = log.user_id;
            const pid = log.post_id;
            const weight = log.action_weight;

            // Xây dựng ma trận User-Item cho thuật toán KNN
            if (!userRatings[uid]) userRatings[uid] = {};
            userRatings[uid][pid] = (userRatings[uid][pid] || 0) + weight;

            // Xây dựng Tag Profile cho Content Score
            if (!userTagProfiles[uid]) userTagProfiles[uid] = {};
            const post = postMap.get(pid);
            if (post && post.tags) {
                // Parse chuỗi "cuisine:vietnamese|flavor:spicy"
                const parts = post.tags.split('|');
                for (const part of parts) {
                    const [cat, vals] = part.split(':');
                    if (!cat || !vals) continue;
                    if (!userTagProfiles[uid][cat]) userTagProfiles[uid][cat] = {};
                    
                    const tagsArr = vals.split('+');
                    for (const v of tagsArr) {
                        userTagProfiles[uid][cat][v] = (userTagProfiles[uid][cat][v] || 0) + weight;
                    }
                }
            }
        }

        // 3. Tính User-User Cosine Similarity & Lưu vào DB
        const allUsers = Object.keys(userRatings);
        const updates = [];

        for (const uid of allUsers) {
            const similarities = [];
            for (const otherUid of allUsers) {
                if (uid === otherUid) continue;
                const sim = getCosineSimilarity(userRatings[uid], userRatings[otherUid]);
                if (sim > 0) similarities.push({ uid: otherUid, sim });
            }
            // Sắp xếp giảm dần và lấy Top 30
            similarities.sort((a, b) => b.sim - a.sim);
            const topSimilar = similarities.slice(0, 30);

            updates.push({
                user_id: uid,
                tag_profile: userTagProfiles[uid],
                similar_users: topSimilar,
                last_updated: new Date().toISOString()
            });
        }

        // 4. Upsert (Cập nhật hoặc Thêm mới) vào Supabase
        const { error: upsertError } = await supabase
            .from("user_profiles")
            .upsert(updates, { onConflict: "user_id" });

        if (upsertError) throw upsertError;

        return new Response(JSON.stringify({ success: true, message: `Đã cập nhật ${updates.length} profiles.` }), {
            headers: { "Content-Type": "application/json" },
        });

    } catch (error) {
        console.error("Cronjob Lỗi:", error);
        return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    }
});
