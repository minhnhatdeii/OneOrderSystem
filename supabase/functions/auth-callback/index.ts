/**
 * Supabase Auth Callback Edge Function
 * 
 * Supabase sends tokens in URL hash (#access_token=xxx) which browsers don't send to servers.
 * This function returns HTML that reads the hash client-side and redirects to the Android app.
 * 
 * Deploy: supabase functions deploy auth-callback
 */

// Handle both old and new Deno stdlib import paths
let _serve;
try {
  const m = await import("https://deno.land/std@0.224.0/http/server.ts");
  _serve = m.serve;
} catch {
  try {
    const m = await import("https://deno.land/std/http/server.ts");
    _serve = m.serve;
  } catch (e) {
    console.error("Failed to import serve:", e);
  }
}

const APP_SCHEME = "oneorder";

const HTML_TEMPLATE = `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#667eea">
  <title>OneOrder - Đang xử lý</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      display: flex; justify-content: center; align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    .container {
      background: white; padding: 48px; border-radius: 24px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
      text-align: center; max-width: 440px; width: 90%;
    }
    .icon {
      width: 80px; height: 80px; border-radius: 50%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 24px;
    }
    .icon svg { width: 40px; height: 40px; fill: white; }
    h1 { color: #333; font-size: 22px; margin-bottom: 12px; }
    p { color: #666; font-size: 15px; line-height: 1.6; margin-bottom: 20px; }
    .spinner {
      width: 40px; height: 40px; margin: 0 auto 20px;
      border: 4px solid #f3f3f3; border-top: 4px solid #667eea;
      border-radius: 50%; animation: spin 1s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .btn {
      display: inline-block; padding: 14px 28px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white !important; border-radius: 12px;
      font-weight: 600; font-size: 15px; text-decoration: none; margin-top: 8px;
    }
    .btn:hover { opacity: 0.9; text-decoration: none; }
    .error-msg {
      color: #dc3545; background: #f8d7da;
      border-radius: 8px; padding: 14px; font-size: 14px;
      text-align: left; margin-top: 12px;
    }
  </style>
</head>
<body>
  <div class="container" id="container">
    <div class="icon">
      <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
    </div>
    <div class="spinner" id="spinner"></div>
    <h1 id="title">Đang xử lý...</h1>
    <p id="message">Vui lòng đợi trong giây lát.</p>
    <a href="#" class="btn" id="btn">Mở ứng dụng OneOrder</a>
    <div class="error-msg" id="errorBox" style="display:none"></div>
  </div>

  <script>
    var APP_SCHEME = "${APP_SCHEME}";
    
    (function() {
      var url = new URL(window.location.href);
      var hashParams = new URLSearchParams(window.location.hash.substring(1));
      var queryParams = new URLSearchParams(url.search);
      
      var accessToken = hashParams.get("access_token");
      var refreshToken = hashParams.get("refresh_token") || "";
      var type = hashParams.get("type");
      var email = hashParams.get("email") || "";
      var code = queryParams.get("code");
      
      var titleEl = document.getElementById("title");
      var messageEl = document.getElementById("message");
      var btnEl = document.getElementById("btn");
      var spinnerEl = document.getElementById("spinner");
      var errorBox = document.getElementById("errorBox");
      var container = document.getElementById("container");
      
      function showError(msg) {
        spinnerEl.style.display = "none";
        btnEl.style.display = "none";
        errorBox.style.display = "block";
        errorBox.textContent = msg;
        titleEl.textContent = "Lỗi";
        messageEl.textContent = "";
      }
      
      function redirectToApp(token, emailVal, fallbackTitle, fallbackMsg) {
        var deepLink = APP_SCHEME + "://password-reset?token=" + encodeURIComponent(token) + "&email=" + encodeURIComponent(emailVal);
        
        titleEl.textContent = fallbackTitle;
        messageEl.textContent = fallbackMsg;
        btnEl.href = deepLink;
        
        // Try to open the app
        window.location.href = deepLink;
        
        // After 2 seconds, if still visible, hide spinner
        setTimeout(function() {
          if (document.visibilityState === "visible") {
            spinnerEl.style.display = "none";
          }
        }, 2000);
        
        // If user switches away (opened app), update UI
        document.addEventListener("visibilitychange", function() {
          if (document.visibilityState === "hidden") {
            spinnerEl.style.display = "none";
            titleEl.textContent = "Thành công!";
            messageEl.textContent = "Đang mở ứng dụng OneOrder...";
          }
        });
      }
      
      if (accessToken && type === "recovery") {
        // Password reset flow
        redirectToApp(accessToken, email, "Đặt lại mật khẩu", "Đang chuyển hướng đến ứng dụng OneOrder...");
      } else if (accessToken && type === "signup") {
        // Email confirmation flow
        var deepLink = APP_SCHEME + "://confirm-email?token=" + encodeURIComponent(accessToken) + "&email=" + encodeURIComponent(email);
        titleEl.textContent = "Xác nhận email";
        messageEl.textContent = "Đang xác nhận email...";
        btnEl.href = deepLink;
        btnEl.textContent = "Mở ứng dụng OneOrder";
        window.location.href = deepLink;
      } else if (code) {
        // PKCE or other OAuth flow — can't handle in this page
        showError("Liên kết này không thể xử lý tự động. Vui lòng mở ứng dụng OneOrder và thử lại.");
      } else {
        // Completely invalid link
        showError("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu gửi lại email mới.");
      }
    })();
  </script>
</body>
</html>`;

// If serve is available, start the server
if (typeof _serve !== "undefined") {
  _serve(async (req) => {
    const url = new URL(req.url);
    console.log("Auth callback URL:", req.url);
    console.log("Hash:", url.hash); // Will be empty — hash not sent to server

    return new Response(HTML_TEMPLATE, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-cache, no-store, must-revalidate",
      },
    });
  });
}
