// Túi Thần Tài — Presentation for RIKKEI FUTURE LEADER 2026
// Run: node docs/tui-than-tai-slide.js

const pptxgen = require("C:/Users/Admin/AppData/Roaming/npm/node_modules/pptxgenjs");
const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10" x 5.625"
pres.title = "Túi Thần Tài – Rikkei Future Leader 2026";

// ── Palette ─────────────────────────────────────────────────────────────────
const C = {
  darkGreen:  "0A2E1A",   // hero dark bg
  deepGreen:  "1A6E3C",   // primary green
  midGreen:   "2E9E5A",   // medium green
  lightGreen: "E6F4EC",   // card bg
  gold:       "C9A84C",   // gold accent
  goldLight:  "F0D080",   // light gold
  white:      "FFFFFF",
  offWhite:   "F7FBF8",
  gray:       "64748B",
  darkText:   "1A2E24",
};

const makeShadow = () => ({
  type: "outer", color: "000000", opacity: 0.12, blur: 8, offset: 3, angle: 135
});

// ── Helpers ──────────────────────────────────────────────────────────────────
function card(slide, x, y, w, h, opts = {}) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h,
    fill: { color: opts.fill || C.white },
    line: { color: opts.border || "E2EAE6", width: 1 },
    shadow: makeShadow(),
  });
}

function tag(slide, x, y, label, color) {
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x, y, w: 1.5, h: 0.28,
    fill: { color: color || C.midGreen },
    line: { width: 0 },
    rectRadius: 0.05,
  });
  slide.addText(label, {
    x, y, w: 1.5, h: 0.28,
    fontSize: 9, bold: true, color: C.white,
    align: "center", valign: "middle", margin: 0,
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 1 — Title
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.darkGreen };

  // Left gold stripe
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.15, h: 5.625,
    fill: { color: C.gold }, line: { width: 0 },
  });

  // Decorative gold circle — top right
  sl.addShape(pres.shapes.OVAL, {
    x: 7.8, y: -0.9, w: 3.2, h: 3.2,
    fill: { color: C.gold, transparency: 82 },
    line: { width: 0 },
  });
  sl.addShape(pres.shapes.OVAL, {
    x: 8.4, y: -0.4, w: 2.2, h: 2.2,
    fill: { color: C.gold, transparency: 70 },
    line: { width: 0 },
  });

  // Decorative circle bottom left
  sl.addShape(pres.shapes.OVAL, {
    x: -0.5, y: 4.2, w: 2.0, h: 2.0,
    fill: { color: C.midGreen, transparency: 60 },
    line: { width: 0 },
  });

  // App name
  sl.addText("TÚI THẦN TÀI", {
    x: 0.5, y: 1.05, w: 9, h: 1.0,
    fontSize: 52, bold: true, color: C.gold,
    fontFace: "Georgia", charSpacing: 4, align: "left", margin: 0,
  });

  // Lucky Wallet
  sl.addText("Lucky Wallet  ·  Android App", {
    x: 0.5, y: 2.1, w: 7, h: 0.45,
    fontSize: 16, color: C.goldLight, italic: true,
    fontFace: "Calibri", align: "left", margin: 0,
  });

  // Tagline
  sl.addText("Quản lý chi tiêu thông minh bằng AI —\nchụp ảnh hoá đơn, app tự ghi chép.", {
    x: 0.5, y: 2.7, w: 6.5, h: 1.0,
    fontSize: 18, color: C.white, fontFace: "Calibri",
    align: "left", margin: 0,
  });

  // Tech pills
  const pills = ["Kotlin", "Jetpack Compose", "Gemini AI", "MLKit", "Google Drive"];
  let px = 0.5;
  pills.forEach(p => {
    const pw = p.length * 0.095 + 0.4;
    sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x: px, y: 3.9, w: pw, h: 0.3,
      fill: { color: C.deepGreen }, line: { color: C.midGreen, width: 1 }, rectRadius: 0.06,
    });
    sl.addText(p, {
      x: px, y: 3.9, w: pw, h: 0.3,
      fontSize: 10, color: C.white, align: "center", valign: "middle", margin: 0,
    });
    px += pw + 0.15;
  });

  // GitHub
  sl.addText("github.com/dt-vinh/tui-than-tai", {
    x: 0.5, y: 4.45, w: 5, h: 0.3,
    fontSize: 11, color: C.gold, align: "left", margin: 0,
    hyperlink: { url: "https://github.com/dt-vinh/tui-than-tai" },
  });

  // Rikkei badge
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 7.0, y: 4.95, w: 2.8, h: 0.5,
    fill: { color: C.gold }, line: { width: 0 },
  });
  sl.addText("RIKKEI FUTURE LEADER 2026", {
    x: 7.0, y: 4.95, w: 2.8, h: 0.5,
    fontSize: 9, bold: true, color: C.darkGreen,
    align: "center", valign: "middle", margin: 0,
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 2 — Problem & Solution
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.offWhite };

  sl.addText("Vấn đề & Giải pháp", {
    x: 0.5, y: 0.3, w: 9, h: 0.6,
    fontSize: 28, bold: true, color: C.darkGreen, fontFace: "Georgia", margin: 0,
  });

  // LEFT — Problem card
  card(sl, 0.4, 1.1, 4.2, 3.9, { fill: "FEF2F2", border: "FECACA" });
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 0.4, y: 1.1, w: 4.2, h: 0.45,
    fill: { color: "DC2626" }, line: { width: 0 },
  });
  sl.addText("❌  Vấn đề", {
    x: 0.4, y: 1.1, w: 4.2, h: 0.45,
    fontSize: 14, bold: true, color: C.white,
    align: "center", valign: "middle", margin: 0,
  });

  const problems = [
    "Ghi chép tay mất thời gian & dễ quên",
    "Hóa đơn, tờ tiền, screenshot mỗi loại một app",
    "Khó nhớ đã tiêu bao nhiêu, tiêu vào đâu",
    "Data dễ mất nếu điện thoại hỏng",
  ];
  problems.forEach((p, i) => {
    sl.addText(`${i + 1}.  ${p}`, {
      x: 0.6, y: 1.75 + i * 0.68, w: 3.8, h: 0.55,
      fontSize: 13, color: "7F1D1D", fontFace: "Calibri",
      align: "left", valign: "middle", margin: 0,
    });
  });

  // Arrow
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 4.75, y: 2.8, w: 0.5, h: 0.08,
    fill: { color: C.gold }, line: { width: 0 },
  });
  sl.addText("→", {
    x: 4.6, y: 2.5, w: 0.8, h: 0.6,
    fontSize: 28, color: C.gold, align: "center", bold: true, margin: 0,
  });

  // RIGHT — Solution card
  card(sl, 5.4, 1.1, 4.2, 3.9, { fill: "F0FAF4", border: "86EFAC" });
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 5.4, y: 1.1, w: 4.2, h: 0.45,
    fill: { color: C.deepGreen }, line: { width: 0 },
  });
  sl.addText("✅  Túi Thần Tài", {
    x: 5.4, y: 1.1, w: 4.2, h: 0.45,
    fontSize: 14, bold: true, color: C.white,
    align: "center", valign: "middle", margin: 0,
  });

  const solutions = [
    "Chụp ảnh → AI tự nhận diện & ghi chép",
    "Hỗ trợ: hoá đơn, tờ tiền, Shopee, MoMo…",
    "Thống kê chi tiêu theo danh mục, tài khoản",
    "Tự động backup lên Google Drive mỗi tối",
  ];
  solutions.forEach((s, i) => {
    sl.addText(`${i + 1}.  ${s}`, {
      x: 5.6, y: 1.75 + i * 0.68, w: 3.8, h: 0.55,
      fontSize: 13, color: "14532D", fontFace: "Calibri",
      align: "left", valign: "middle", margin: 0,
    });
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 3 — Key Features (2×2 grid)
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.offWhite };

  sl.addText("Tính Năng Chính", {
    x: 0.5, y: 0.25, w: 9, h: 0.6,
    fontSize: 28, bold: true, color: C.darkGreen, fontFace: "Georgia", margin: 0,
  });

  const features = [
    {
      icon: "📷", title: "OCR 2 Tầng",
      lines: ["Gemini Vision AI (primary)", "MLKit + SmartTotalResolver (offline)", "Hỗ trợ: VI · EN · TH · JP · KR"],
      color: C.deepGreen,
    },
    {
      icon: "☁️", title: "Google Drive Backup",
      lines: ["Tự động sao lưu lúc 8h tối", "Đăng nhập Google, bảo mật OAuth2", "Khôi phục 1 chạm"],
      color: "1565C0",
    },
    {
      icon: "📊", title: "Thống Kê & Lịch Sử",
      lines: ["Biểu đồ chi tiêu theo danh mục", "Lịch sử giao dịch có thể lọc", "Quản lý nhiều tài khoản"],
      color: "7B1FA2",
    },
    {
      icon: "🤖", title: "AI Nhận Diện",
      lines: ["Hoá đơn, tờ tiền, screenshot", "Shopee · MoMo · ZaloPay · Banking", "Đa ngôn ngữ, mọi định dạng"],
      color: "C62828",
    },
  ];

  const positions = [
    { x: 0.4, y: 0.95 }, { x: 5.2, y: 0.95 },
    { x: 0.4, y: 3.1  }, { x: 5.2, y: 3.1  },
  ];

  features.forEach((f, i) => {
    const { x, y } = positions[i];
    card(sl, x, y, 4.5, 2.0, { fill: C.white });
    // Color accent left bar
    sl.addShape(pres.shapes.RECTANGLE, {
      x, y, w: 0.12, h: 2.0,
      fill: { color: f.color }, line: { width: 0 },
    });
    // Icon circle
    sl.addShape(pres.shapes.OVAL, {
      x: x + 0.22, y: y + 0.18, w: 0.65, h: 0.65,
      fill: { color: f.color, transparency: 88 }, line: { width: 0 },
    });
    sl.addText(f.icon, {
      x: x + 0.22, y: y + 0.18, w: 0.65, h: 0.65,
      fontSize: 22, align: "center", valign: "middle", margin: 0,
    });
    // Title
    sl.addText(f.title, {
      x: x + 1.0, y: y + 0.18, w: 3.3, h: 0.4,
      fontSize: 14, bold: true, color: f.color, margin: 0,
    });
    // Lines
    f.lines.forEach((line, li) => {
      sl.addText("· " + line, {
        x: x + 1.0, y: y + 0.62 + li * 0.38, w: 3.3, h: 0.38,
        fontSize: 11.5, color: C.gray, margin: 0,
      });
    });
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 4 — OCR Pipeline
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.darkGreen };

  sl.addText("OCR Pipeline — Kiến Trúc 2 Tầng", {
    x: 0.5, y: 0.25, w: 9, h: 0.6,
    fontSize: 26, bold: true, color: C.gold, fontFace: "Georgia", margin: 0,
  });
  sl.addText("Đa ngôn ngữ · Hoạt động offline · Tự động chọn tầng tối ưu", {
    x: 0.5, y: 0.85, w: 9, h: 0.35,
    fontSize: 12, color: C.goldLight, italic: true, margin: 0,
  });

  // Input box
  card(sl, 0.3, 1.35, 1.6, 1.4, { fill: "1E3A28", border: C.midGreen });
  sl.addText("📸\nUser chụp ảnh", {
    x: 0.3, y: 1.35, w: 1.6, h: 1.4,
    fontSize: 12, color: C.white, align: "center", valign: "middle", margin: 0,
  });

  // Arrow 1
  sl.addText("→", { x: 2.0, y: 1.8, w: 0.5, h: 0.5, fontSize: 22, color: C.gold, align: "center", margin: 0 });

  // Stage 1 — Gemini
  card(sl, 2.55, 1.15, 2.8, 1.9, { fill: "0F3320", border: C.gold });
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 2.55, y: 1.15, w: 2.8, h: 0.38,
    fill: { color: C.gold }, line: { width: 0 },
  });
  sl.addText("STAGE 1  ·  PRIMARY", {
    x: 2.55, y: 1.15, w: 2.8, h: 0.38,
    fontSize: 9, bold: true, color: C.darkGreen, align: "center", valign: "middle", margin: 0,
  });
  sl.addText([
    { text: "Gemini Vision AI\n", options: { bold: true, fontSize: 14, color: C.gold } },
    { text: "gemini-2.0-flash-lite\n", options: { fontSize: 10, color: C.goldLight } },
    { text: "Hiểu ngữ cảnh đa ngôn ngữ\nVI · EN · TH · JP · KR · ZH", options: { fontSize: 10, color: C.white } },
  ], { x: 2.65, y: 1.6, w: 2.6, h: 1.35, valign: "top", margin: 0 });

  // Condition fork
  sl.addText("✅ OK", { x: 5.45, y: 1.45, w: 1.1, h: 0.38, fontSize: 11, color: "4ADE80", bold: true, align: "center", margin: 0 });
  sl.addText("→", { x: 5.5, y: 1.85, w: 0.6, h: 0.4, fontSize: 20, color: "4ADE80", align: "center", margin: 0 });
  sl.addText("❌ Fail\n(no network)", { x: 5.35, y: 2.35, w: 1.3, h: 0.55, fontSize: 10, color: "FCA5A5", align: "center", margin: 0 });
  sl.addText("↓", { x: 5.6, y: 2.9, w: 0.5, h: 0.3, fontSize: 18, color: "FCA5A5", align: "center", margin: 0 });

  // Stage 2 — MLKit
  card(sl, 2.55, 3.1, 2.8, 2.1, { fill: "0F3320", border: C.midGreen });
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 2.55, y: 3.1, w: 2.8, h: 0.38,
    fill: { color: C.midGreen }, line: { width: 0 },
  });
  sl.addText("STAGE 2  ·  FALLBACK", {
    x: 2.55, y: 3.1, w: 2.8, h: 0.38,
    fontSize: 9, bold: true, color: C.white, align: "center", valign: "middle", margin: 0,
  });
  sl.addText([
    { text: "MLKit OCR\n", options: { bold: true, fontSize: 13, color: "86EFAC" } },
    { text: "+ SmartTotalResolver\n", options: { bold: true, fontSize: 13, color: "86EFAC" } },
    { text: "Currency symbols: đ ₫ $ € £ ¥ ₩\n", options: { fontSize: 10, color: C.white } },
    { text: "Positional + Frequency scoring", options: { fontSize: 10, color: C.white } },
  ], { x: 2.65, y: 3.55, w: 2.6, h: 1.55, valign: "top", margin: 0 });

  // Output box
  card(sl, 6.2, 1.35, 2.0, 1.4, { fill: "1E3A28", border: C.gold });
  sl.addText("💰\nExpenseSuggestion\n+ confidence", {
    x: 6.2, y: 1.35, w: 2.0, h: 1.4,
    fontSize: 11, color: C.white, align: "center", valign: "middle", margin: 0,
  });
  sl.addText("→", { x: 5.6, y: 1.8, w: 0.55, h: 0.5, fontSize: 22, color: "4ADE80", align: "center", margin: 0 });

  // Confidence threshold note
  card(sl, 6.2, 3.1, 2.0, 2.1, { fill: "1E3A28", border: C.midGreen });
  sl.addText([
    { text: "Confidence\n", options: { bold: true, color: C.gold, fontSize: 12 } },
    { text: "≥ 0.65 → Done\n", options: { color: "4ADE80", fontSize: 11 } },
    { text: "< 0.65 →\nneedsReview", options: { color: "FCA5A5", fontSize: 11 } },
  ], { x: 6.2, y: 3.1, w: 2.0, h: 2.1, align: "center", valign: "middle", margin: 0 });
  sl.addText("→", { x: 5.6, y: 3.95, w: 0.55, h: 0.5, fontSize: 22, color: "86EFAC", align: "center", margin: 0 });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 5 — Tech Stack
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.offWhite };

  sl.addText("Tech Stack", {
    x: 0.5, y: 0.25, w: 9, h: 0.6,
    fontSize: 28, bold: true, color: C.darkGreen, fontFace: "Georgia", margin: 0,
  });

  const layers = [
    {
      label: "UI Layer", color: C.deepGreen,
      items: ["Kotlin", "Jetpack Compose", "Material 3", "CameraX"],
    },
    {
      label: "AI / OCR Layer", color: "7B1FA2",
      items: ["Gemini Vision API", "Google MLKit", "SmartTotalResolver", "Gemini 2.0 Flash Lite"],
    },
    {
      label: "Data Layer", color: "1565C0",
      items: ["Room Database", "WorkManager", "Google Drive API", "SharedPreferences"],
    },
    {
      label: "Auth / Cloud", color: C.gold,
      items: ["Google Sign-In", "OAuth 2.0", "Drive AppData Folder", "Auto Backup 8PM"],
    },
  ];

  layers.forEach((layer, i) => {
    const x = 0.35 + (i % 2) * 4.8;
    const y = 1.05 + Math.floor(i / 2) * 2.2;
    card(sl, x, y, 4.3, 1.9, { fill: C.white });
    sl.addShape(pres.shapes.RECTANGLE, {
      x, y, w: 4.3, h: 0.4,
      fill: { color: layer.color }, line: { width: 0 },
    });
    sl.addText(layer.label, {
      x, y, w: 4.3, h: 0.4,
      fontSize: 13, bold: true, color: C.white,
      align: "center", valign: "middle", margin: 0,
    });
    layer.items.forEach((item, j) => {
      const col = j < 2 ? 0 : 1;
      const row = j % 2;
      sl.addShape(pres.shapes.OVAL, {
        x: x + 0.2 + col * 2.1, y: y + 0.55 + row * 0.58, w: 0.18, h: 0.18,
        fill: { color: layer.color }, line: { width: 0 },
      });
      sl.addText(item, {
        x: x + 0.45 + col * 2.1, y: y + 0.5 + row * 0.58, w: 1.8, h: 0.3,
        fontSize: 11, color: C.darkText, margin: 0,
      });
    });
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 6 — Engineering Challenges
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.offWhite };

  sl.addText("Thách Thức Kỹ Thuật & Giải Pháp", {
    x: 0.5, y: 0.25, w: 9, h: 0.6,
    fontSize: 26, bold: true, color: C.darkGreen, fontFace: "Georgia", margin: 0,
  });

  const challenges = [
    {
      num: "01",
      challenge: "MLKit đọc 'đ' thành ASCII 'd'",
      detail: "Ký tự Unicode U+0111 bị normalize → OCR miss toàn bộ số tiền có 'đ'",
      solution: "Regex [đĐdkK] + lookahead (?![A-Za-z0-9]) để match cả 2 dạng encoding",
      tag: "Regex · Unicode",
    },
    {
      num: "02",
      challenge: "OCR chọn nhầm số serial thay vì tiền",
      detail: "Số tài khoản (13+ chữ số) và số serial trên tờ tiền bị chọn là 'tổng tiền'",
      solution: "SmartTotalResolver: >10 digits filter + positional score + frequency penalty cho số lặp",
      tag: "Algorithm · Scoring",
    },
    {
      num: "03",
      challenge: "OAuth token hết hạn → Backup thất bại",
      detail: "Token lưu SharedPreferences hết hạn sau ~1h, gây lỗi 401 khi backup",
      solution: "getFreshDriveToken(): invalidate token cũ + re-fetch trước mỗi lần backup/restore",
      tag: "OAuth 2.0 · Google API",
    },
  ];

  challenges.forEach((c, i) => {
    const y = 1.0 + i * 1.48;
    card(sl, 0.35, y, 9.3, 1.32, { fill: C.white });

    // Number badge
    sl.addShape(pres.shapes.OVAL, {
      x: 0.45, y: y + 0.18, w: 0.75, h: 0.75,
      fill: { color: C.deepGreen }, line: { width: 0 },
    });
    sl.addText(c.num, {
      x: 0.45, y: y + 0.18, w: 0.75, h: 0.75,
      fontSize: 16, bold: true, color: C.white,
      align: "center", valign: "middle", margin: 0,
    });

    // Challenge title
    sl.addText(c.challenge, {
      x: 1.35, y: y + 0.1, w: 4.8, h: 0.38,
      fontSize: 14, bold: true, color: C.darkText, margin: 0,
    });
    sl.addText(c.detail, {
      x: 1.35, y: y + 0.5, w: 4.8, h: 0.35,
      fontSize: 10.5, color: C.gray, italic: true, margin: 0,
    });

    // Solution
    sl.addShape(pres.shapes.RECTANGLE, {
      x: 6.25, y: y + 0.08, w: 0.04, h: 1.1,
      fill: { color: C.midGreen }, line: { width: 0 },
    });
    sl.addText("✅  " + c.solution, {
      x: 6.35, y: y + 0.08, w: 3.1, h: 1.1,
      fontSize: 10.5, color: C.deepGreen, valign: "middle", margin: 0,
    });

    tag(sl, 1.35, y + 0.92, c.tag, C.deepGreen);
  });
}

// ════════════════════════════════════════════════════════════════════════════
// SLIDE 7 — About & Why Rikkei (Closing — dark)
// ════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  sl.background = { color: C.darkGreen };

  // Gold strip top
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 10, h: 0.12,
    fill: { color: C.gold }, line: { width: 0 },
  });

  sl.addText("Tại Sao Rikkei Future Leader?", {
    x: 0.5, y: 0.28, w: 9, h: 0.65,
    fontSize: 28, bold: true, color: C.gold, fontFace: "Georgia", margin: 0,
  });

  // Left: skills demonstrated
  card(sl, 0.4, 1.1, 4.2, 4.0, { fill: "0F3320", border: C.midGreen });
  sl.addText("Kỹ Năng Thể Hiện Qua Dự Án", {
    x: 0.5, y: 1.2, w: 4.0, h: 0.4,
    fontSize: 13, bold: true, color: C.gold, margin: 0,
  });
  const skills = [
    "Android / Kotlin từ đầu đến sản phẩm",
    "Tích hợp AI API (Gemini Vision)",
    "OAuth 2.0 + Google APIs",
    "Thiết kế pipeline OCR đa ngôn ngữ",
    "Debug production issues từ log",
    "Clean Architecture & modular code",
  ];
  skills.forEach((s, i) => {
    sl.addText("▸  " + s, {
      x: 0.55, y: 1.68 + i * 0.5, w: 3.9, h: 0.45,
      fontSize: 12, color: C.white, margin: 0,
    });
  });

  // Right: why Rikkei
  card(sl, 5.0, 1.1, 4.5, 4.0, { fill: "0F3320", border: C.gold });
  sl.addText("Mục Tiêu Tham Gia", {
    x: 5.1, y: 1.2, w: 4.2, h: 0.4,
    fontSize: 13, bold: true, color: C.gold, margin: 0,
  });
  const goals = [
    "Học leadership trong môi trường IT thực chiến",
    "Phát triển tư duy product + engineering cùng lúc",
    "Network với top engineers tại Rikkei",
    "Đưa Túi Thần Tài lên production thực sự",
  ];
  goals.forEach((g, i) => {
    sl.addText("▸  " + g, {
      x: 5.1, y: 1.68 + i * 0.6, w: 4.2, h: 0.55,
      fontSize: 12, color: C.white, margin: 0,
    });
  });

  // Bottom bar
  sl.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 5.17, w: 10, h: 0.455,
    fill: { color: C.gold }, line: { width: 0 },
  });
  sl.addText("github.com/dt-vinh/tui-than-tai  ·  Túi Thần Tài  ·  RIKKEI FUTURE LEADER 2026", {
    x: 0, y: 5.17, w: 10, h: 0.455,
    fontSize: 11, bold: true, color: C.darkGreen,
    align: "center", valign: "middle", margin: 0,
  });
}

// ── Write file ────────────────────────────────────────────────────────────
pres.writeFile({ fileName: "docs/TuiThanTai-RikkeiFutureLeader2026.pptx" })
  .then(() => console.log("✅  Saved: docs/TuiThanTai-RikkeiFutureLeader2026.pptx"))
  .catch(e => console.error("❌", e));
