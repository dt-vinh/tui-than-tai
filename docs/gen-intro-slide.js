/**
 * Túi Thần Tài — Slide giới thiệu app (màu tươi sáng — purple/indigo)
 * Run: node docs/gen-intro-slide.js
 */
const pptxgen = require("C:/Users/Admin/AppData/Roaming/npm/node_modules/pptxgenjs");
const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10" × 5.625"

const IMG_DIR = "C:/Users/Admin/Documents/vinh/tui-than-tai/docs/store-imgs";

// ── Palette tươi sáng — deep indigo + bright purple + cyan ──────────────────
const C = {
  bg:     "0F0B2E",   // deep indigo (đậm nhưng sáng hơn đen)
  bgMid:  "2D1B69",   // mid purple
  purple: "7C3AED",   // vivid purple (glow)
  purpleL:"A78BFA",   // light purple
  cyan:   "22D3EE",   // bright cyan accent
  yellow: "FBBF24",   // bright amber/yellow (headline accent)
  yellowL:"FDE68A",   // light yellow
  white:  "FFFFFF",
  muted:  "A0A0C8",   // lavender-muted
  dark:   "1E1650",   // dark card
};

// ── Slide 1: Title ─────────────────────────────────────────────────────────
{
  const sl = pres.addSlide();
  sl.background = { color: C.bg };

  // Glow blobs
  sl.addShape(pres.shapes.OVAL, {
    x:6.5, y:-2.5, w:7, h:7,
    fill:{ color: C.purple, transparency:85 }, line:{width:0},
  });
  sl.addShape(pres.shapes.OVAL, {
    x:-2.5, y:2.5, w:5.5, h:5.5,
    fill:{ color: C.cyan, transparency:90 }, line:{width:0},
  });
  sl.addShape(pres.shapes.OVAL, {
    x:3.5, y:1.5, w:3, h:3,
    fill:{ color: C.purpleL, transparency:88 }, line:{width:0},
  });

  // App name big
  sl.addText("Túi Thần Tài", {
    x:0.6, y:1.00, w:8.8, h:1.70,
    fontSize:82, bold:true, color:C.yellowL,
    fontFace:"Georgia", align:"center", valign:"middle", margin:0,
  });

  // Tagline
  sl.addText("Quản lý chi tiêu thông minh — AI · Google Drive · Đa ngôn ngữ", {
    x:0.6, y:2.82, w:8.8, h:0.55,
    fontSize:20, color:C.muted,
    fontFace:"Segoe UI", align:"center", margin:0, charSpacing:1,
  });

  // Divider (cyan)
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:4.0, y:3.52, w:2.0, h:0.05,
    fill:{color: C.cyan}, line:{width:0}, rectRadius:0.02,
  });

  // Sub-info
  sl.addText("Android  ·  Kotlin + Jetpack Compose  ·  Material 3", {
    x:0.6, y:3.72, w:8.8, h:0.42,
    fontSize:16, color:"5A5A8A",
    align:"center", margin:0, charSpacing:1,
  });

  // Slide number tag
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fill:{color:C.dark}, line:{width:0}, rectRadius:0.06,
  });
  sl.addText("01", {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fontSize:18, bold:true, color:C.purpleL,
    fontFace:"Georgia", align:"center", valign:"middle", margin:0,
  });
}

// ── Helper: content slide ────────────────────────────────────────────────────
function contentSlide(imgFile, num, headline1, headline2, bullets) {
  const sl = pres.addSlide();
  sl.background = { color: C.bg };

  // Glow top-right
  sl.addShape(pres.shapes.OVAL, {
    x:6.5, y:-2.0, w:6, h:6,
    fill:{color:C.purple, transparency:86}, line:{width:0},
  });
  // Glow bottom-left (cyan tint)
  sl.addShape(pres.shapes.OVAL, {
    x:-2.0, y:3.0, w:4.5, h:4.5,
    fill:{color:C.cyan, transparency:91}, line:{width:0},
  });

  // ── Phone image (right side) ──
  const IH = 5.25;
  const IW = IH * (1080 / 1920);   // ≈ 2.953"
  const IX = 10 - IW - 0.18;
  const IY = (5.625 - IH) / 2;

  sl.addImage({ path: imgFile, x: IX, y: IY, w: IW, h: IH });

  // ── Text block (left) ──
  const lx = 0.45, lw = IX - lx - 0.35;

  // Headline 1 — white
  sl.addText(headline1, {
    x:lx, y:0.72, w:lw, h:0.88,
    fontSize:41, bold:true, color:C.white,
    fontFace:"Segoe UI", align:"left", valign:"bottom", margin:0,
  });

  // Headline 2 — bright yellow
  sl.addText(headline2, {
    x:lx, y:1.62, w:lw, h:0.88,
    fontSize:41, bold:true, color:C.yellowL,
    fontFace:"Segoe UI", align:"left", valign:"top", margin:0,
  });

  // Cyan divider
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:lx, y:2.60, w:1.6, h:0.05,
    fill:{color:C.cyan}, line:{width:0}, rectRadius:0.02,
  });

  // Bullets
  bullets.forEach((b, i) => {
    const by = 2.78 + i * 0.57;
    // Purple dot
    sl.addShape(pres.shapes.OVAL, {
      x:lx, y:by+0.15, w:0.13, h:0.13,
      fill:{color:C.purpleL}, line:{width:0},
    });
    sl.addText(b, {
      x:lx+0.24, y:by, w:lw-0.24, h:0.46,
      fontSize:15, color:C.white,
      fontFace:"Segoe UI", align:"left", valign:"middle", margin:0,
    });
  });

  // App name bottom-left
  sl.addText("Túi Thần Tài", {
    x:lx, y:5.12, w:2.5, h:0.36,
    fontSize:14, bold:true, color:C.yellow,
    fontFace:"Segoe UI", align:"left", margin:0,
  });

  // Slide number
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fill:{color:C.dark}, line:{width:0}, rectRadius:0.06,
  });
  sl.addText(num, {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fontSize:18, bold:true, color:C.purpleL,
    fontFace:"Georgia", align:"center", valign:"middle", margin:0,
  });
}

// ── Slides 2–5 ───────────────────────────────────────────────────────────────
contentSlide(
  `${IMG_DIR}/01-home.png`, "02",
  "Quản lý tiền,",
  "trong tầm tay.",
  [
    "📊  Thống kê thu chi theo danh mục & tháng",
    "💳  Quản lý nhiều tài khoản cùng lúc",
    "📅  Lịch sử giao dịch đầy đủ, dễ tra cứu",
  ]
);

contentSlide(
  `${IMG_DIR}/02-ai-scan.png`, "03",
  "Chụp ảnh.",
  "AI tự ghi chép.",
  [
    "🤖  Gemini Vision nhận diện hoá đơn tức thì",
    "💵  Hỗ trợ tờ tiền, screenshot Shopee/MoMo",
    "🌐  Đa ngôn ngữ: VI · EN · TH · JP · KR",
  ]
);

contentSlide(
  `${IMG_DIR}/03-report.png`, "04",
  "Biết rõ tiền",
  "đi đâu, vì sao.",
  [
    "📈  Biểu đồ thu/chi theo thời gian",
    "🗂️  Phân tích theo từng danh mục chi tiêu",
    "🔍  So sánh ngân sách vs thực chi",
  ]
);

contentSlide(
  `${IMG_DIR}/04-backup.png`, "05",
  "Dữ liệu của bạn,",
  "luôn an toàn.",
  [
    "☁️  Auto backup Google Drive lúc 8:00 tối",
    "🔐  Xác thực OAuth 2.0 — token tự làm mới",
    "🔄  Khôi phục dữ liệu chỉ một chạm",
  ]
);

// ── Slide 6: Closing ─────────────────────────────────────────────────────────
{
  const sl = pres.addSlide();
  sl.background = { color: C.bg };

  // Big glow centre
  sl.addShape(pres.shapes.OVAL, {
    x:2.5, y:-1.5, w:9, h:9,
    fill:{color:C.purple, transparency:86}, line:{width:0},
  });
  sl.addShape(pres.shapes.OVAL, {
    x:0, y:3, w:4, h:4,
    fill:{color:C.cyan, transparency:91}, line:{width:0},
  });

  sl.addText("Cảm ơn bạn đã xem!", {
    x:0.5, y:1.15, w:9, h:0.92,
    fontSize:50, bold:true, color:C.white,
    fontFace:"Segoe UI", align:"center", margin:0,
  });

  sl.addText("Túi Thần Tài", {
    x:0.5, y:2.20, w:9, h:0.78,
    fontSize:38, bold:true, color:C.yellowL,
    fontFace:"Georgia", align:"center", margin:0,
  });

  // Cyan divider
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:3.8, y:3.12, w:2.4, h:0.05,
    fill:{color:C.cyan}, line:{width:0}, rectRadius:0.02,
  });

  sl.addText("github.com/dt-vinh/tui-than-tai", {
    x:0.5, y:3.32, w:9, h:0.44,
    fontSize:18, color:C.muted,
    fontFace:"Segoe UI", align:"center", margin:0, charSpacing:0.5,
  });

  sl.addText("Android  ·  Kotlin  ·  Gemini Vision  ·  Google Drive", {
    x:0.5, y:3.88, w:9, h:0.38,
    fontSize:15, color:"4A4A7A",
    fontFace:"Segoe UI", align:"center", margin:0, charSpacing:1,
  });

  // Slide number
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fill:{color:C.dark}, line:{width:0}, rectRadius:0.06,
  });
  sl.addText("06", {
    x:9.28, y:5.13, w:0.58, h:0.36,
    fontSize:18, bold:true, color:C.purpleL,
    fontFace:"Georgia", align:"center", valign:"middle", margin:0,
  });
}

// ── Write ─────────────────────────────────────────────────────────────────────
const OUT = "C:/Users/Admin/Documents/vinh/tui-than-tai/docs/TuiThanTai-Intro-v2.pptx";
pres.writeFile({ fileName: OUT })
  .then(() => console.log("✅  Saved:", OUT))
  .catch(e => console.error("❌", e));
