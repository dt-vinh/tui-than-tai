/**
 * Túi Thần Tài — App Store Promotional Screenshots (4 slides)
 * Style: dark bg + phone mockup + big headline (like CapMoney)
 * Run: node docs/store-screenshots.js
 */
const pptxgen = require("C:/Users/Admin/AppData/Roaming/npm/node_modules/pptxgenjs");
const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10" × 5.625"

// ── Palette ──────────────────────────────────────────────────────────────────
const C = {
  bg:      "071510",
  green:   "0F8F5F",
  greenL:  "16C174",
  greenD:  "0A3320",
  gold:    "C9A84C",
  goldL:   "E8C96A",
  white:   "FFFFFF",
  muted:   "607870",
  muted2:  "96B8A8",
  red:     "F87171",
  blue:    "60A5FA",
  purple:  "A78BFA",
  orange:  "FB923C",
  phoneFr: "182C20",
  phoneSc: "0B1A11",
  card:    "0F2219",
  cardBdr: "1C3D2A",
};

// ── Phone dimensions (used on every slide) ────────────────────────────────────
const PX = 5.88, PY = 0.30, PW = 2.14, PH = 4.90;
// Screen (inset inside frame)
const SX = PX + 0.09,  SY = PY + 0.14;
const SW = PW - 0.18,  SH = PH - 0.28;
// SX=5.97  SY=0.44  SW=1.96  SH=4.62
// Screen right edge: SX+SW = 7.93
// Screen bottom:     SY+SH = 5.06  ← safely within slide height 5.625

// ── Helpers ───────────────────────────────────────────────────────────────────

// Slide background + decorative glows
function addBg(sl) {
  sl.background = { color: C.bg };
  sl.addShape(pres.shapes.OVAL, {
    x: 7.2, y: -2.5, w: 5.5, h: 5.5,
    fill: { color: C.green, transparency: 91 }, line: { width: 0 },
  });
  sl.addShape(pres.shapes.OVAL, {
    x: -2.5, y: 3.2, w: 5.0, h: 5.0,
    fill: { color: C.green, transparency: 93 }, line: { width: 0 },
  });
}

// Phone frame + screen background (draw once per slide)
function addPhone(sl) {
  // Drop shadow
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: PX + 0.07, y: PY + 0.12, w: PW, h: PH,
    fill: { color: "000000", transparency: 62 },
    line: { width: 0 }, rectRadius: 0.22,
  });
  // Outer frame
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: PX, y: PY, w: PW, h: PH,
    fill: { color: C.phoneFr },
    line: { color: "243C2E", width: 2 },
    rectRadius: 0.22,
  });
  // Screen fill
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: SX, y: SY, w: SW, h: SH,
    fill: { color: C.phoneSc },
    line: { width: 0 }, rectRadius: 0.17,
  });
  // Dynamic island
  sl.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: PX + PW / 2 - 0.20, y: PY + 0.10, w: 0.40, h: 0.10,
    fill: { color: C.bg }, line: { width: 0 }, rectRadius: 0.05,
  });
}

// Screen clip: clamp element to stay inside screen bounds
// (just a reminder — caller must ensure x,y,w,h are within SX..SX+SW, SY..SY+SH)

// ─ small text inside screen
function st(sl, x, y, w, h, txt, opts = {}) {
  sl.addText(txt, {
    x, y, w, h,
    fontSize:  opts.fs    || 7,
    color:     opts.color || C.white,
    bold:      opts.bold  || false,
    italic:    opts.italic || false,
    align:     opts.align || "left",
    valign:    opts.valign || "middle",
    fontFace:  "Calibri",
    margin: 0,
  });
}

// ─ small shape inside screen
function ss(sl, shape, x, y, w, h, fill, opts = {}) {
  sl.addShape(shape, {
    x, y, w, h,
    fill: { color: fill },
    line: opts.line || { width: 0 },
    rectRadius: opts.r || undefined,
  });
}

// Status bar (common to all screens)
function statusBar(sl) {
  st(sl, SX + 0.06, SY + 0.02, 0.35, 0.14, "9:41",
     { fs: 7.5, bold: true });
  // Battery stub
  ss(sl, pres.shapes.ROUNDED_RECTANGLE,
     SX + SW - 0.26, SY + 0.04, 0.22, 0.08, C.greenL, { r: 0.02 });
  ss(sl, pres.shapes.RECTANGLE,
     SX + SW - 0.06, SY + 0.05, 0.03, 0.06, C.greenL);
}

// Left-column text block (headline + subtitle + bullets + app name)
// Positions chosen so NO element overlaps any other or the phone (starts at PX=5.88)
function leftBlock(sl, tag, h1, h2, sub, bullets, num) {
  const lx = 0.35, lw = 5.20; // right edge = 5.55  (gap of 0.33 before phone at 5.88)

  // ① Category tag  y=0.30..0.56
  sl.addText(tag, {
    x: lx, y: 0.30, w: lw, h: 0.26,
    fontSize: 8.5, color: C.muted, charSpacing: 2.5,
    align: "left", margin: 0,
  });

  // ② Headline line 1 (white)  y=0.62..1.38
  sl.addText(h1, {
    x: lx, y: 0.62, w: lw, h: 0.76,
    fontSize: 40, bold: true, color: C.white,
    fontFace: "Georgia", align: "left", margin: 0,
  });

  // ③ Headline line 2 (gold)  y=1.40..2.16
  sl.addText(h2, {
    x: lx, y: 1.40, w: lw, h: 0.76,
    fontSize: 40, bold: true, color: C.goldL,
    fontFace: "Georgia", align: "left", margin: 0,
  });

  // ④ Subtitle  y=2.28..2.88  (gap 0.12 after headline)
  sl.addText(sub, {
    x: lx, y: 2.28, w: 4.85, h: 0.60,
    fontSize: 12.5, color: C.muted2,
    align: "left", valign: "top", margin: 0,
  });

  // ⑤ Bullets  y=3.05..4.45  (3 bullets × 0.47h, gap 0.17 after subtitle)
  bullets.forEach((b, i) => {
    const by = 3.05 + i * 0.47;
    ss(sl, pres.shapes.OVAL, lx, by + 0.13, 0.13, 0.13, C.green);
    sl.addText(b, {
      x: lx + 0.23, y: by, w: 4.85, h: 0.38,
      fontSize: 12.5, color: C.white,
      align: "left", valign: "middle", margin: 0,
    });
  });
  // ⑥ Divider  y=4.88..4.91
  ss(sl, pres.shapes.RECTANGLE, lx, 4.90, 1.60, 0.03, C.greenD);

  // ⑦ App name  y=4.98..5.30
  sl.addText("Túi Thần Tài", {
    x: lx, y: 4.98, w: 2.60, h: 0.32,
    fontSize: 13, bold: true, color: C.gold,
    align: "left", margin: 0,
  });

  // ⑧ Slide number badge  bottom-right of slide
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, 9.30, 5.15, 0.55, 0.35, C.greenD, { r: 0.06 });
  sl.addText(num, {
    x: 9.30, y: 5.15, w: 0.55, h: 0.35,
    fontSize: 18, bold: true, color: C.green,
    align: "center", valign: "middle", fontFace: "Georgia", margin: 0,
  });
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE 1 — Home screen
// ══════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  addBg(sl);
  leftBlock(sl,
    "SMART MONEY  ·  PERSONAL FINANCE",
    "Quản lý tiền",
    "thông minh.",
    "Theo dõi thu chi, ngân sách và thống kê — tất cả trong một app.",
    [
      "📊  Thống kê theo danh mục & tháng",
      "💳  Quản lý nhiều tài khoản",
      "📅  Lịch sử giao dịch đầy đủ",
    ],
    "01"
  );
  addPhone(sl);

  // ── Screen content: Home ───────────────────────────────────────────────────
  statusBar(sl);

  // Header strip
  ss(sl, pres.shapes.RECTANGLE, SX, SY + 0.18, SW, 0.38, "0F2A1C");
  st(sl, SX + 0.06, SY + 0.18, SW - 0.12, 0.38, "Tháng 4 · 2026",
     { fs: 8, bold: true, align: "center" });

  // Balance card
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 0.62, SW - 0.12, 1.02, C.card,
     { r: 0.08, line: { color: C.cardBdr, width: 1 } });
  st(sl, SX + 0.06, SY + 0.70, SW - 0.12, 0.22, "Chi tiêu tháng này",
     { fs: 6.5, color: C.muted2, align: "center" });
  st(sl, SX + 0.06, SY + 0.92, SW - 0.12, 0.36, "-967.000đ",
     { fs: 16, bold: true, color: C.red, align: "center" });
  // Income/expense mini stats
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 1.34, (SW - 0.20) / 2, 0.24, "0D2A1A", { r: 0.04 });
  st(sl, SX + 0.06, SY + 1.34, (SW - 0.20) / 2, 0.24, "↑ Thu: 1.234k",
     { fs: 6, color: C.greenL, align: "center" });
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + (SW - 0.20) / 2 + 0.14, SY + 1.34, (SW - 0.20) / 2, 0.24, "2A0D0D", { r: 0.04 });
  st(sl, SX + (SW - 0.20) / 2 + 0.14, SY + 1.34, (SW - 0.20) / 2, 0.24, "↓ Chi: 967k",
     { fs: 6, color: C.red, align: "center" });

  // Section header
  st(sl, SX + 0.06, SY + 1.72, SW - 0.12, 0.22, "Giao dịch gần đây",
     { fs: 7, bold: true, color: C.muted2 });

  // Transaction rows
  const txs = [
    { icon: "🍜", name: "Ăn uống", amt: "-65.000đ", color: C.orange, dt: "Hôm nay" },
    { icon: "🛵", name: "Di chuyển", amt: "-25.000đ", color: C.blue,   dt: "Hôm nay" },
    { icon: "🛒", name: "Mua sắm",  amt: "-86.250đ", color: C.purple, dt: "Hôm qua" },
  ];
  txs.forEach((tx, i) => {
    const ty = SY + 2.00 + i * 0.58;
    // Dot
    ss(sl, pres.shapes.OVAL, SX + 0.08, ty + 0.10, 0.28, 0.28, tx.color);
    st(sl, SX + 0.08, ty + 0.10, 0.28, 0.28, tx.icon, { fs: 9, align: "center" });
    // Name + date
    st(sl, SX + 0.42, ty + 0.04, 0.85, 0.20, tx.name, { fs: 7.5, bold: true });
    st(sl, SX + 0.42, ty + 0.24, 0.85, 0.16, tx.dt, { fs: 6, color: C.muted2 });
    // Amount
    st(sl, SX + SW - 0.68, ty + 0.10, 0.60, 0.22, tx.amt,
       { fs: 7.5, bold: true, color: C.red, align: "right" });
    // Separator line (except last)
    if (i < 2) {
      ss(sl, pres.shapes.RECTANGLE, SX + 0.06, ty + 0.50, SW - 0.12, 0.01, "1A3A28");
    }
  });

  // Bottom nav bar
  ss(sl, pres.shapes.RECTANGLE, SX, SY + SH - 0.40, SW, 0.40, "0F2219");
  const navIcons = ["🏠", "📊", "📋", "⚙️"];
  navIcons.forEach((ic, i) => {
    const nx = SX + 0.10 + i * (SW - 0.20) / 3;
    st(sl, nx, SY + SH - 0.35, 0.38, 0.30, ic,
       { fs: 11, align: "center", valign: "middle" });
  });
  // Active indicator under first icon
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.16, SY + SH - 0.08, 0.26, 0.04, C.green, { r: 0.02 });
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE 2 — OCR Camera
// ══════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  addBg(sl);
  leftBlock(sl,
    "AI  ·  OCR  ·  COMPUTER VISION",
    "Chụp ảnh.",
    "AI lo phần còn lại.",
    "Gemini Vision tự nhận diện số tiền từ hoá đơn, tờ tiền, screenshot.",
    [
      "🧾  Hoá đơn POS, e-invoice",
      "💵  Tờ tiền, phiếu thu",
      "📱  Shopee · MoMo · ZaloPay",
    ],
    "02"
  );
  addPhone(sl);
  statusBar(sl);

  // Header
  ss(sl, pres.shapes.RECTANGLE, SX, SY + 0.18, SW, 0.38, "0F2A1C");
  st(sl, SX + 0.06, SY + 0.18, SW - 0.12, 0.38, "Nhận diện hoá đơn",
     { fs: 8, bold: true, align: "center" });

  // Camera viewfinder area
  const vx = SX + 0.08, vy = SY + 0.62, vw = SW - 0.16, vh = 2.20;
  ss(sl, pres.shapes.RECTANGLE, vx, vy, vw, vh, "090F0C");
  // Scan corner brackets
  const blen = 0.20, bthk = 0.04;
  const corners = [
    [vx, vy], [vx + vw - blen, vy],
    [vx, vy + vh - blen], [vx + vw - blen, vy + vh - blen],
  ];
  corners.forEach(([cx, cy]) => {
    const isRight = cx > vx + vw / 2;
    const isBottom = cy > vy + vh / 2;
    // Horizontal arm
    ss(sl, pres.shapes.RECTANGLE,
       isRight ? cx : cx, isBottom ? cy + blen - bthk : cy,
       blen, bthk, C.greenL);
    // Vertical arm
    ss(sl, pres.shapes.RECTANGLE,
       isRight ? cx + blen - bthk : cx, isBottom ? cy : cy,
       bthk, blen, C.greenL);
  });
  // Scan line
  ss(sl, pres.shapes.RECTANGLE, vx + 0.04, vy + 1.05, vw - 0.08, 0.025, C.greenL);

  // Receipt thumbnail inside viewfinder
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, vx + 0.30, vy + 0.30, vw - 0.60, 1.50, "141F18", { r: 0.04 });
  st(sl, vx + 0.30, vy + 0.38, vw - 0.60, 0.20, "GUMPA SHOP",
     { fs: 7.5, bold: true, align: "center", color: C.muted2 });
  st(sl, vx + 0.30, vy + 0.60, vw - 0.60, 0.18, "Áo sơ mi nam",
     { fs: 6.5, align: "center", color: C.muted2 });
  ss(sl, pres.shapes.RECTANGLE, vx + 0.30, vy + 0.82, vw - 0.60, 0.01, "1C3A28");
  st(sl, vx + 0.30, vy + 0.88, vw - 0.60, 0.28, "86.250đ",
     { fs: 13, bold: true, align: "center", color: C.white });
  st(sl, vx + 0.30, vy + 1.18, vw - 0.60, 0.18, "Tổng số tiền",
     { fs: 6, align: "center", color: C.greenL });

  // AI scanning label
  st(sl, vx, vy + vh + 0.05, vw, 0.22, "✦  AI đang nhận diện...",
     { fs: 7, color: C.greenL, align: "center", italic: true });

  // Result card
  const ry = SY + 3.10;
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, ry, SW - 0.12, 0.98, C.card,
     { r: 0.08, line: { color: C.green, width: 1 } });
  // Green check badge
  ss(sl, pres.shapes.OVAL, SX + 0.16, ry + 0.10, 0.26, 0.26, C.green);
  st(sl, SX + 0.16, ry + 0.10, 0.26, 0.26, "✓",
     { fs: 10, bold: true, align: "center", valign: "middle" });
  st(sl, SX + 0.50, ry + 0.10, SW - 0.65, 0.22, "Đã nhận diện",
     { fs: 6.5, color: C.greenL });
  st(sl, SX + 0.50, ry + 0.32, SW - 0.65, 0.30, "86.250đ",
     { fs: 15, bold: true, color: C.white });
  st(sl, SX + 0.50, ry + 0.64, SW - 0.65, 0.18, "Chi tiêu · Mua sắm",
     { fs: 6.5, color: C.muted2 });

  // Bottom nav
  ss(sl, pres.shapes.RECTANGLE, SX, SY + SH - 0.40, SW, 0.40, "0F2219");
  ["🏠","📷","📊","⚙️"].forEach((ic, i) => {
    st(sl, SX + 0.10 + i * (SW - 0.20) / 3, SY + SH - 0.35, 0.38, 0.30, ic,
       { fs: 11, align: "center" });
  });
  ss(sl, pres.shapes.ROUNDED_RECTANGLE,
     SX + 0.10 + 1 * (SW - 0.20) / 3 + 0.06, SY + SH - 0.08,
     0.26, 0.04, C.green, { r: 0.02 });
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE 3 — Statistics
// ══════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  addBg(sl);
  leftBlock(sl,
    "INSIGHTS  ·  SEE THE BIG PICTURE",
    "Biết rõ tiền",
    "đi đâu, vì sao.",
    "Biểu đồ chi tiêu theo danh mục, tháng, tài khoản. Rõ từng đồng.",
    [
      "🍩  Biểu đồ donut theo danh mục",
      "📈  So sánh thu nhập vs chi tiêu",
      "💰  Phân tích xu hướng theo tháng",
    ],
    "03"
  );
  addPhone(sl);
  statusBar(sl);

  // Tab bar
  ss(sl, pres.shapes.RECTANGLE, SX, SY + 0.18, SW, 0.32, "0F2A1C");
  const tabs = ["Tháng", "Quý", "Năm"];
  tabs.forEach((t, i) => {
    const tx = SX + 0.06 + i * (SW - 0.12) / 3;
    const tw = (SW - 0.12) / 3;
    if (i === 0) {
      ss(sl, pres.shapes.ROUNDED_RECTANGLE, tx, SY + 0.20, tw, 0.26, C.greenD, { r: 0.04 });
    }
    st(sl, tx, SY + 0.20, tw, 0.26, t,
       { fs: 7, bold: i === 0, color: i === 0 ? C.white : C.muted, align: "center" });
  });

  // Month label
  st(sl, SX + 0.06, SY + 0.56, SW - 0.12, 0.22, "Tháng 4 · 2026",
     { fs: 7.5, bold: true, align: "center", color: C.muted2 });

  // Donut chart (concentric arcs simulated with ovals)
  const cx = SX + SW / 2 - 0.02, cy = SY + 1.75, cr = 0.58;
  // Outer ring slices (use ovals with clip/transparency to fake pie chart)
  const slices = [
    { color: C.orange,  r: cr },
    { color: C.blue,    r: cr - 0.02 },
    { color: C.purple,  r: cr - 0.04 },
    { color: C.green,   r: cr - 0.06 },
  ];
  // Draw outer ring (full circle = background ring)
  ss(sl, pres.shapes.OVAL, cx - cr, cy - cr, cr * 2, cr * 2, "1A3028");
  // Draw inner cutout (donut hole)
  const ir = cr * 0.58;
  ss(sl, pres.shapes.OVAL, cx - ir, cy - ir, ir * 2, ir * 2, C.phoneSc);
  // Center total
  st(sl, cx - 0.50, cy - 0.24, 1.00, 0.22, "Tổng chi",
     { fs: 6, color: C.muted2, align: "center" });
  st(sl, cx - 0.50, cy - 0.04, 1.00, 0.30, "967k",
     { fs: 13, bold: true, color: C.white, align: "center" });

  // Color arc indicators (small arcs around donut)
  const arcColors = [C.orange, C.blue, C.purple, C.green];
  arcColors.forEach((ac, i) => {
    const angle = i * 90 * Math.PI / 180;
    const ax = cx + cr * 0.80 * Math.cos(angle) - 0.07;
    const ay = cy + cr * 0.80 * Math.sin(angle) - 0.07;
    ss(sl, pres.shapes.OVAL, ax, ay, 0.14, 0.14, ac);
  });

  // Category legend
  const cats = [
    { name: "Ăn uống",    pct: "38%", color: C.orange },
    { name: "Mua sắm",    pct: "27%", color: C.blue   },
    { name: "Di chuyển",  pct: "19%", color: C.purple },
    { name: "Khác",       pct: "16%", color: C.green  },
  ];
  cats.forEach((cat, i) => {
    const lx2 = SX + 0.06, ly = SY + 2.75 + i * 0.37;
    ss(sl, pres.shapes.OVAL, lx2, ly + 0.06, 0.12, 0.12, cat.color);
    st(sl, lx2 + 0.18, ly, SW * 0.55, 0.26, cat.name,
       { fs: 7, color: C.white });
    // Bar
    const barW = SW - 0.12 - 0.52;
    const barX = SX + SW - 0.06 - barW;
    ss(sl, pres.shapes.ROUNDED_RECTANGLE, barX, ly + 0.08, barW * 0.85, 0.10, "1A3028", { r: 0.03 });
    ss(sl, pres.shapes.ROUNDED_RECTANGLE, barX, ly + 0.08, barW * 0.85 * parseFloat(cat.pct) / 100, 0.10, cat.color, { r: 0.03 });
    st(sl, barX + barW * 0.85 + 0.03, ly, 0.22, 0.26, cat.pct,
       { fs: 6.5, color: C.muted2, align: "right" });
  });

  // Bottom nav
  ss(sl, pres.shapes.RECTANGLE, SX, SY + SH - 0.40, SW, 0.40, "0F2219");
  ["🏠","📷","📊","⚙️"].forEach((ic, i) => {
    st(sl, SX + 0.10 + i * (SW - 0.20) / 3, SY + SH - 0.35, 0.38, 0.30, ic,
       { fs: 11, align: "center" });
  });
  ss(sl, pres.shapes.ROUNDED_RECTANGLE,
     SX + 0.10 + 2 * (SW - 0.20) / 3 + 0.06, SY + SH - 0.08,
     0.26, 0.04, C.green, { r: 0.02 });
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE 4 — Google Drive Backup
// ══════════════════════════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  addBg(sl);
  leftBlock(sl,
    "BACKUP  ·  ALWAYS SAFE",
    "Dữ liệu của bạn,",
    "luôn an toàn.",
    "Tự động sao lưu lên Google Drive mỗi tối lúc 8h. Khôi phục 1 chạm.",
    [
      "☁️  Google Drive backup tự động",
      "🔐  Bảo mật OAuth 2.0",
      "🔄  Khôi phục dữ liệu dễ dàng",
    ],
    "04"
  );
  addPhone(sl);
  statusBar(sl);

  // Settings header
  ss(sl, pres.shapes.RECTANGLE, SX, SY + 0.18, SW, 0.38, "0F2A1C");
  st(sl, SX + 0.06, SY + 0.18, SW - 0.12, 0.38, "Cài đặt & Sao lưu",
     { fs: 8, bold: true, align: "center" });

  // Google account card
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 0.64, SW - 0.12, 0.82, C.card,
     { r: 0.08, line: { color: C.cardBdr, width: 1 } });
  // Avatar circle
  ss(sl, pres.shapes.OVAL, SX + 0.14, SY + 0.76, 0.36, 0.36, C.greenD);
  st(sl, SX + 0.14, SY + 0.76, 0.36, 0.36, "👤",
     { fs: 12, align: "center", valign: "middle" });
  st(sl, SX + 0.58, SY + 0.72, SW - 0.76, 0.20, "Alex Nguyen",
     { fs: 8, bold: true });
  st(sl, SX + 0.58, SY + 0.92, SW - 0.76, 0.16, "alex@gmail.com",
     { fs: 6.5, color: C.muted2 });
  // "Đã kết nối" badge
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.58, SY + 1.13, 0.72, 0.18, "0A3320", { r: 0.04 });
  st(sl, SX + 0.58, SY + 1.13, 0.72, 0.18, "✓ Đã kết nối",
     { fs: 6, color: C.greenL, align: "center" });

  // Last backup row
  ss(sl, pres.shapes.RECTANGLE, SX + 0.06, SY + 1.54, SW - 0.12, 0.01, "1A3A28");
  st(sl, SX + 0.10, SY + 1.60, SW * 0.60, 0.28, "Lần sao lưu cuối",
     { fs: 7.5, color: C.muted2 });
  st(sl, SX + 0.10, SY + 1.86, SW * 0.60, 0.22, "10/06/2026 · 20:00",
     { fs: 7, bold: true, color: C.white });
  ss(sl, pres.shapes.OVAL, SX + SW - 0.42, SY + 1.64, 0.22, 0.22, "0A3320");
  st(sl, SX + SW - 0.42, SY + 1.64, 0.22, 0.22, "☁",
     { fs: 9, color: C.greenL, align: "center" });

  // Auto backup toggle row
  ss(sl, pres.shapes.RECTANGLE, SX + 0.06, SY + 2.14, SW - 0.12, 0.01, "1A3A28");
  st(sl, SX + 0.10, SY + 2.20, SW * 0.65, 0.26, "Tự động sao lưu 8h tối",
     { fs: 7.5, color: C.white });
  // Toggle ON
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + SW - 0.54, SY + 2.22, 0.44, 0.22, C.green, { r: 0.11 });
  ss(sl, pres.shapes.OVAL, SX + SW - 0.56 + 0.26, SY + 2.23, 0.18, 0.18, C.white);

  // Backup now button
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 2.58, SW - 0.12, 0.42, C.green, { r: 0.08 });
  st(sl, SX + 0.06, SY + 2.58, SW - 0.12, 0.42, "☁  Sao lưu ngay",
     { fs: 9, bold: true, color: C.white, align: "center" });

  // Restore button
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 3.08, SW - 0.12, 0.42, C.greenD,
     { r: 0.08, line: { color: C.green, width: 1 } });
  st(sl, SX + 0.06, SY + 3.08, SW - 0.12, 0.42, "↩  Khôi phục dữ liệu",
     { fs: 9, color: C.greenL, align: "center" });

  // Success status
  ss(sl, pres.shapes.ROUNDED_RECTANGLE, SX + 0.06, SY + 3.62, SW - 0.12, 0.32, "061A0E",
     { r: 0.05, line: { color: "0A3320", width: 1 } });
  st(sl, SX + 0.06, SY + 3.62, SW - 0.12, 0.32, "✓  Đã sao lưu thành công",
     { fs: 7, color: C.greenL, align: "center" });

  // Bottom nav
  ss(sl, pres.shapes.RECTANGLE, SX, SY + SH - 0.40, SW, 0.40, "0F2219");
  ["🏠","📷","📊","⚙️"].forEach((ic, i) => {
    st(sl, SX + 0.10 + i * (SW - 0.20) / 3, SY + SH - 0.35, 0.38, 0.30, ic,
       { fs: 11, align: "center" });
  });
  ss(sl, pres.shapes.ROUNDED_RECTANGLE,
     SX + 0.10 + 3 * (SW - 0.20) / 3 + 0.04, SY + SH - 0.08,
     0.26, 0.04, C.green, { r: 0.02 });
}

// ── Write ─────────────────────────────────────────────────────────────────────
pres.writeFile({ fileName: "C:/Users/Admin/Documents/vinh/tui-than-tai/docs/TuiThanTai-StoreScreenshots.pptx" })
  .then(() => console.log("✅  Saved: docs/TuiThanTai-StoreScreenshots.pptx"))
  .catch(e => console.error("❌", e));
