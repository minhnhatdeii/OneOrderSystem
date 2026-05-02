import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseKey = Deno.env.get("SUPABASE_ANON_KEY")!;

const CAT_W: Record<string, number> = { cuisine: 0.35, flavor: 0.30, protein: 0.20, cooking_method: 0.10, dish_type: 0.05 };
const LAMBDA = 0.82; // MMR: balance relevance vs diversity (higher = more relevance)
const CANDIDATE_POOL = 200; // Number of top-scored candidates for MMR reranking

// Hàm tính khoảng cách địa lý (Haversine)
function distKm(lat1: number, lon1: number, lat2: number, lon2: number) {
    const R = 6371, toR = Math.PI / 180;
    const dLat = (lat2 - lat1) * toR, dLon = (lon2 - lon1) * toR;
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * toR) * Math.cos(lat2 * toR) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ─── Tag Jaccard similarity ─────────────────────────────────────────────
// Jaccard(postA, postB) = |tagsA ∩ tagsB| / |tagsA ∪ tagsB|
function computeTagSet(tagsStr: string): Set<string> {
    const tagSet = new Set<string>();
    if (!tagsStr) return tagSet;
    const parts = tagsStr.split('|');
    for (const part of parts) {
        if (part) tagSet.add(part);
    }
    return tagSet;
}

function jaccard(tagsA: string, tagsB: string): number {
    const setA = computeTagSet(tagsA);
    const setB = computeTagSet(tagsB);
    if (setA.size === 0 || setB.size === 0) return 0;
    let inter = 0;
    for (const t of setA) if (setB.has(t)) inter++;
    const union = setA.size + setB.size - inter;
    return union === 0 ? 0 : inter / union;
}

serve(async (req) => {
    try {
        if (req.method === 'OPTIONS') {
            return new Response('ok', {
                headers: { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type' }
            });
        }

        let userId: string;
        let lat: number, lng: number;
        let page: number, limit: number, refresh: boolean;

        try {
            const authHeader = req.headers.get('Authorization');
            if (!authHeader) throw new Error("Missing Authorization header");

            const supabase = createClient(supabaseUrl, supabaseKey, {
                global: { headers: { Authorization: authHeader } }
            });
            const supabaseAdmin = createClient(supabaseUrl, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

            const { data: { user }, error: authError } = await supabase.auth.getUser();
            if (authError || !user) throw new Error("Unauthorized - Token invalid");
            userId = user.id;

            const body = await req.json();
            lat = body.lat || 0.0;
            lng = body.lng || 0.0;
            page = body.page || 1;
            limit = body.limit || 10;
            refresh = body.refresh || false;

            // #region agent debug log
            console.log(`[get-food-feed] DEBUG: lat=${lat}, lng=${lng}, page=${page}, limit=${limit}, refresh=${refresh}`);
            // #endregion

            // ── 1. Cache Check ────────────────────────────────────────────
            // #region agent debug log
            console.log(`[get-food-feed] DEBUG: Checking cache for user=${userId}, lat=${lat}, lng=${lng}`);
            // #endregion
            const { data: cache } = await supabaseAdmin.from('feed_cache').select('*').eq('user_id', userId).single();

            if (cache && new Date(cache.expires_at) > new Date()) {
                // #region agent debug log
                console.log(`[get-food-feed] DEBUG: Cache hit, expires_at=${cache.expires_at}`);
                // #endregion
                const firstItem = cache.recommended_post_ids?.[0];
                if (firstItem && typeof firstItem === 'object') {
                    if (refresh) {
                        // Recalculate distanceKm for all shuffled posts
                        const shuffled = [...(cache.recommended_post_ids as any[])].sort(() => 0.5 - Math.random());
                        const shuffledWithDistance = shuffled.map((post: any) => {
                            if (lat && lng && post.restaurantLat && post.restaurantLng && post.restaurantLat !== 0 && post.restaurantLng !== 0) {
                                const dist = distKm(lat, lng, post.restaurantLat, post.restaurantLng);
                                return { ...post, distanceKm: dist };
                            }
                            return post;
                        });
                        supabaseAdmin.from('feed_cache').update({ recommended_post_ids: shuffledWithDistance }).eq('user_id', userId).then();
                        return new Response(JSON.stringify({ data: shuffledWithDistance.slice(0, limit), source: 'cache_shuffled' }),
                            { headers: { 'Content-Type': 'application/json' } });
                    } else if (page >= 1) {
                        const startIndex = (page - 1) * limit;
                        const pagePosts = (cache.recommended_post_ids as any[]).slice(startIndex, startIndex + limit);
                        // #region agent debug log
                        console.log(`[get-food-feed] DEBUG: Cache pagination - startIndex=${startIndex}, posts=${pagePosts.length}`);
                        // #endregion
                        if (pagePosts.length > 0) {
                            // FIX: Recalculate distanceKm for each cached post
                            // Cache cũ có thể được tạo khi chưa có GPS (lat=0, lng=0)
                            // Cần tính lại khoảng cách nếu user bây giờ đã có GPS
                            const postsWithDistance = pagePosts.map((post: any) => {
                                if (lat && lng && post.restaurantLat && post.restaurantLng && post.restaurantLat !== 0 && post.restaurantLng !== 0) {
                                    const dist = distKm(lat, lng, post.restaurantLat, post.restaurantLng);
                                    // #region agent debug log
                                    console.log(`[get-food-feed] DEBUG: Recalculating dist for ${post.id}: oldDist=${post.distanceKm}, newDist=${dist.toFixed(2)}km`);
                                    // #endregion
                                    return { ...post, distanceKm: dist };
                                }
                                return post;
                            });
                            // FIX: Cập nhật lại cache với distance đã recalculate
                            // Để lần paginate tiếp theo (page 2, 3...) cũng có distance đúng
                            const updatedCache = (cache.recommended_post_ids as any[]).map((originalPost: any) => {
                                const updated = postsWithDistance.find((p: any) => p.id === originalPost.id);
                                return updated || originalPost;
                            });
                            supabaseAdmin.from('feed_cache').update({
                                recommended_post_ids: updatedCache
                            }).eq('user_id', userId).then();
                            return new Response(JSON.stringify({ data: postsWithDistance, source: 'cache_pagination' }),
                                { headers: { 'Content-Type': 'application/json' } });
                        }
                    }
                }
            } else {
                // #region agent debug log
                console.log(`[get-food-feed] DEBUG: Cache miss or expired`);
                // #endregion
            }

            console.log(`[get-food-feed] Computing new feed for user: ${userId}`);

            // ── 2. Load User Profile ─────────────────────────────────────
            const { data: profile } = await supabase.from('user_profiles').select('*').eq('user_id', userId).single();
            const tagProfile = profile?.tag_profile || {};

            const catMax: Record<string, number> = {};
            for (const [cat, sub] of Object.entries(tagProfile)) {
                const vals = Object.values(sub as Record<string, number>);
                catMax[cat] = Math.max(...vals, 1);
            }

            // ── 3. Fetch Candidates ──────────────────────────────────────
            // #region agent debug log
            console.log(`[get-food-feed] DEBUG: Calling RPC get_recommended_feed_posts with p_user_id=${userId}, p_user_lat=${lat}, p_user_lng=${lng}, p_limit=300, p_offset=0`);
            // #endregion
            const { data: candidates, error: candidateErr } = await supabaseAdmin.rpc('get_recommended_feed_posts', {
                p_user_id: userId,
                p_user_lat: lat,
                p_user_lng: lng,
                p_limit: 300,
                p_offset: 0
            });

            // #region agent debug log
            console.log(`[get-food-feed] DEBUG: RPC returned candidates count=${candidates?.length || 0}, error=${candidateErr?.message || 'none'}`);
            if (candidates && candidates.length > 0) {
                const first = candidates[0];
                console.log(`[get-food-feed] DEBUG: First candidate - distanceKm=${first.distanceKm}, restaurantLat=${first.restaurantLat}, restaurantLng=${first.restaurantLng}`);
            }
            // #endregion

            if (candidateErr) console.error("RPC Error:", candidateErr);
            if (!candidates || !Array.isArray(candidates) || candidates.length === 0) {
                return new Response(JSON.stringify({ error: "No posts found" }), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
                });
            }

            // ── 4. Compute Global Tag IDF ─────────────────────────────────
            const tagCounts: Record<string, number> = {};
            for (const post of candidates as any[]) {
                if (!post.tags) continue;
                const parts = post.tags.split('|');
                for (const part of parts) {
                    if (part) tagCounts[part] = (tagCounts[part] || 0) + 1;
                }
            }

            const totalPosts = (candidates as any[]).length;
            const maxIDF = Math.log((totalPosts + 1) / 2);
            const idfMap: Record<string, number> = {};
            for (const [tag, count] of Object.entries(tagCounts)) {
                idfMap[tag] = Math.log((totalPosts + 1) / (count + 1)) / maxIDF;
            }

            // ── 5. Score All Candidates ───────────────────────────────────
            const REQUEST_TIME = Date.now();

            const scoredPosts = (candidates as any[]).map(post => {
                let contentRaw = 0, catTotal = 0;
                if (post.tags) {
                    const parts = post.tags.split('|');
                    for (const part of parts) {
                        const colonIdx = part.indexOf(':');
                        if (colonIdx === -1) continue;
                        const cat = part.substring(0, colonIdx);
                        const valsStr = part.substring(colonIdx + 1);
                        const w = CAT_W[cat] || 0.05;
                        catTotal += w;

                        if (!tagProfile[cat]) continue;

                        const vals = valsStr.split('+');
                        let catScore = 0;
                        for (const v of vals) {
                            const key = `${cat}:${v}`;
                            const rawScore = ((tagProfile[cat] as Record<string, number>)?.[v] || 0) / (catMax[cat] || 1);
                            const idfFactor = idfMap[key] ?? 1.0;
                            catScore += rawScore * idfFactor;
                        }
                        contentRaw += (catScore / vals.length) * w;
                    }
                }
                const contentScore = (catTotal > 0 ? contentRaw / catTotal : 0) * 0.55;

                let locationScore = 0;
                // #region agent debug log
                if (lat && lng && post.restaurantLat && post.restaurantLng) {
                    const dist = distKm(lat, lng, post.restaurantLat, post.restaurantLng);
                    post.distanceKm = dist;
                    locationScore = (1 - Math.min(dist / 20.0, 1)) * 0.15;
                    console.log(`[get-food-feed] DEBUG: Post ${post.id} - lat=${lat}, lng=${lng}, rLat=${post.restaurantLat}, rLng=${post.restaurantLng}, dist=${dist.toFixed(2)}km, locScore=${locationScore.toFixed(4)}`);
                } else {
                    console.log(`[get-food-feed] DEBUG: Post ${post.id} - no location data (lat=${lat}, lng=${lng}, rLat=${post.restaurantLat}, rLng=${post.restaurantLng})`);
                }
                // #endregion
                if (lat && lng && post.restaurantLat && post.restaurantLng) {
                    const dist = distKm(lat, lng, post.restaurantLat, post.restaurantLng);
                    post.distanceKm = dist;
                    locationScore = (1 - Math.min(dist / 20.0, 1)) * 0.15;
                }

                const trendingScore = Math.min(Math.log(1 + (post.like_count || 0)) / Math.log(200), 1.0) * 0.08;

                const ageMs = REQUEST_TIME - new Date(post.created_at).getTime();
                const ageDays = Math.max(0, ageMs / 86400000);
                const freshnessScore = (1 - Math.min(ageDays / 90, 1)) * 0.02;

                const randomNoise = (Math.random() * 0.06) - 0.03;

                const finalScore = contentScore + locationScore + trendingScore + freshnessScore + randomNoise;

                return {
                    post_id: post.id,
                    score: finalScore,
                    post_details: post,
                    tags: post.tags || '',
                };
            });

            scoredPosts.sort((a, b) => b.score - a.score);

            // ── 6. MMR Reranking ─────────────────────────────────────────
            const topCandidates = scoredPosts.slice(0, CANDIDATE_POOL).filter(p => p.score > -900);
            const selected: typeof topCandidates = [];
            const remaining = [...topCandidates];
            const selectedCuisines = new Set<string>();
            const selectedDishTypes = new Set<string>();

            while (selected.length < limit && remaining.length > 0) {
                if (selected.length === 0) {
                    const first = remaining.shift()!;
                    selected.push(first);
                    const cuisines = first.post_details?.cuisine_tags as string[] | undefined;
                    const dishTypes = first.post_details?.dish_type_tags as string[] | undefined;
                    cuisines?.forEach(c => selectedCuisines.add(c));
                    dishTypes?.forEach(d => selectedDishTypes.add(d));
                    continue;
                }

                let bestIdx = 0, bestMMR = -Infinity;

                for (let i = 0; i < remaining.length; i++) {
                    const item = remaining[i];
                    const relevance = item.score;
                    let divPenalty = 0;

                    const cuisine = item.post_details?.cuisine_tags as string[] | undefined;
                    if (cuisine) {
                        for (const c of cuisine) {
                            if (selectedCuisines.has(c)) divPenalty += 0.08;
                        }
                    }

                    const dishType = item.post_details?.dish_type_tags as string[] | undefined;
                    if (dishType) {
                        for (const d of dishType) {
                            if (selectedDishTypes.has(d)) divPenalty += 0.06;
                        }
                    }

                    let maxSim = 0;
                    for (const sel of selected) {
                        const sim = jaccard(item.tags, sel.tags);
                        if (sim > maxSim) maxSim = sim;
                    }
                    divPenalty += maxSim * 0.12;

                    const mmr = LAMBDA * relevance - (1 - LAMBDA) * divPenalty;
                    if (mmr > bestMMR) {
                        bestMMR = mmr;
                        bestIdx = i;
                    }
                }

                const chosen = remaining.splice(bestIdx, 1)[0];
                selected.push(chosen);
                const cuisines = chosen.post_details?.cuisine_tags as string[] | undefined;
                const dishTypes = chosen.post_details?.dish_type_tags as string[] | undefined;
                cuisines?.forEach(c => selectedCuisines.add(c));
                dishTypes?.forEach(d => selectedDishTypes.add(d));
            }

            // ── 7. Save to Cache ─────────────────────────────────────────
            const top100 = scoredPosts.slice(0, 100).map(p => p.post_details);
            const expiresAt = new Date();
            expiresAt.setHours(expiresAt.getHours() + 4);

            // Safety net: ensure every post has distanceKm before caching.
            // Handles cases where RPC returned 0 due to guard logic or NULL restaurant coords.
            const top100WithDistance = top100.map((post: any) => {
                const needsRecalc = post && !post.distanceKm && post.restaurantLat && post.restaurantLng
                    && post.restaurantLat !== 0 && post.restaurantLng !== 0 && lat && lng;
                if (needsRecalc) {
                    return { ...post, distanceKm: distKm(lat, lng, post.restaurantLat, post.restaurantLng) };
                }
                return post;
            });

            supabaseAdmin.from('feed_cache').upsert({
                user_id: userId,
                recommended_post_ids: top100WithDistance,
                expires_at: expiresAt.toISOString()
            }).then();

            // ── 8. Return Response ───────────────────────────────────────
            const responseData = selected.map(p => p.post_details);

            return new Response(JSON.stringify({ data: responseData, source: 'computed' }), {
                headers: { "Content-Type": "application/json", 'Access-Control-Allow-Origin': '*' }
            });

        } catch (innerError: any) {
            console.error('[get-food-feed] Inner error:', innerError?.message || innerError);
            return new Response(JSON.stringify({ error: innerError?.message || 'Unknown error' }), {
                status: 200, // Return 200 so the app doesn't crash, it will handle the error gracefully
                headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
            });
        }

    } catch (outerError: any) {
        console.error('[get-food-feed] Outer error:', outerError?.message || outerError);
        return new Response(JSON.stringify({ error: outerError?.message || 'Service error' }), {
            status: 200,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
        });
    }
});
