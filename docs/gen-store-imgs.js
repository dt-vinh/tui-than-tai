/**
 * Túi Thần Tài — Google Play Store Promotional Screenshots
 * Tạo 4 ảnh PNG 1080×1920 dạng như CapMoney
 * Run: node docs/gen-store-imgs.js
 */
const fs   = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const OUT_DIR  = "C:/Users/Admin/Documents/vinh/tui-than-tai/docs/store-imgs";
const SHOTS    = "C:/Users/Admin/Downloads/New folder";
const CHROME   = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

// ── Slides definition ────────────────────────────────────────────────────────
const SLIDES = [
  {
    id: "01-home",
    tag: "SMART MONEY · PERSONAL FINANCE",
    line1: "Quản lý tiền,",
    line2: "trong tầm tay.",
    sub:  "Theo dõi thu chi, ngân sách và thống kê — tất cả trong một app duy nhất.",
    img:  `${SHOTS}/Screenshot_2026-06-10-21-59-14-07.jpg`,
  },
  {
    id: "02-ai-scan",
    tag: "AI · OCR · GEMINI VISION",
    line1: "Chụp ảnh.",
    line2: "AI tự ghi chép.",
    sub:  "Gemini Vision nhận diện số tiền từ hoá đơn, tờ tiền, screenshot trong tích tắc.",
    img:  `${SHOTS}/Screenshot_2026-06-10-22-00-08-41.jpg`,
  },
  {
    id: "03-report",
    tag: "INSIGHTS · SEE THE BIG PICTURE",
    line1: "Biết rõ tiền",
    line2: "đi đâu, vì sao.",
    sub:  "Biểu đồ thu chi theo danh mục và thời gian. Rõ từng đồng, từng ngày.",
    img:  `${SHOTS}/Screenshot_2026-06-10-22-00-46-15.jpg`,
  },
  {
    id: "04-backup",
    tag: "BACKUP · GOOGLE DRIVE · AUTO",
    line1: "Dữ liệu của bạn,",
    line2: "luôn an toàn.",
    sub:  "Tự động sao lưu lên Google Drive lúc 8:00 tối. Khôi phục chỉ một chạm.",
    img:  `${SHOTS}/Screenshot_2026-06-10-22-00-59-99.jpg`,
  },
];

// ── HTML template ─────────────────────────────────────────────────────────────
function html(s) {
  // NFC-normalize all text to avoid decomposed diacritics (e.g. tiê`n → tiền)
  const nfc = (t) => t.normalize("NFC");
  const imgUrl = "file:///" + s.img.replace(/\\/g, "/").replace(/ /g, "%20");

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@700;800&display=swap" rel="stylesheet">
<style>
* { margin:0; padding:0; box-sizing:border-box; }

body {
  width: 1080px;
  height: 1920px;
  background: linear-gradient(150deg, #061210 0%, #0E2E1E 45%, #061210 100%);
  font-family: 'Segoe UI', Arial, sans-serif;
  overflow: hidden;
  position: relative;
}

/* ── Atmospheric glows ── */
.glow1 {
  position:absolute; width:700px; height:700px;
  top:-180px; right:-150px;
  background: radial-gradient(circle, rgba(15,143,95,0.18) 0%, transparent 65%);
}
.glow2 {
  position:absolute; width:600px; height:600px;
  bottom:300px; left:-150px;
  background: radial-gradient(circle, rgba(15,143,95,0.14) 0%, transparent 65%);
}

/* ── Category tag ── */
.tag {
  position:absolute;
  top: 130px; left: 72px;
  font-size: 26px;
  letter-spacing: 3.5px;
  color: #507060;
  font-weight: 400;
  white-space: nowrap;
}

/* ── Headline ── */
.headline {
  position:absolute;
  top: 202px; left: 72px; right: 72px;
}
.headline .l1 {
  display:block;
  font-family: 'Be Vietnam Pro', 'Segoe UI', Arial, sans-serif;
  font-size: 108px;
  font-weight: bold;
  color: #FFFFFF;
  line-height: 1.08;
}
.headline .l2 {
  display:block;
  font-family: 'Be Vietnam Pro', 'Segoe UI', Arial, sans-serif;
  font-size: 108px;
  font-weight: bold;
  color: #E8C86A;
  line-height: 1.08;
  margin-top: 6px;
}

/* ── Subtitle ── */
.sub {
  position:absolute;
  top: 556px; left: 72px; right: 72px;
  font-size: 36px;
  color: #8AADA0;
  line-height: 1.45;
}

/* ── Phone mockup wrapper ── */
.phone-wrap {
  position: absolute;
  top: 710px;
  left: 50%;
  transform: translateX(-50%);
  width: 460px;
  height: 920px;
}

/* Drop shadow behind phone */
.phone-shadow {
  position:absolute;
  top:18px; left:12px;
  width:460px; height:920px;
  background: rgba(0,0,0,0.55);
  border-radius: 54px;
  filter: blur(22px);
}

/* Phone outer frame */
.phone-frame {
  position:relative;
  width:460px; height:920px;
  background: #1A2E22;
  border-radius: 54px;
  border: 4.5px solid #2A4A34;
  overflow: hidden;
}

/* Screen area (inset) */
.screen {
  position:absolute;
  top: 12px; left: 10px;
  width: calc(100% - 20px);
  height: calc(100% - 24px);
  border-radius: 46px;
  overflow: hidden;
  background: #f0ecf8;
}
.screen img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top center;
}

/* Dynamic island */
.island {
  position:absolute;
  top: 18px; left:50%; transform:translateX(-50%);
  width: 90px; height: 22px;
  background: #0A1810;
  border-radius: 11px;
  z-index: 10;
}

/* ── App name (bottom) ── */
.bottom {
  position:absolute;
  bottom: 88px; left: 72px;
}
.divider {
  width: 56px; height: 4px;
  background: #0C2E1A;
  border-radius: 2px;
  margin-bottom: 18px;
}
.appname {
  font-size: 38px;
  font-weight: 700;
  color: #C9A84C;
  letter-spacing: 0.5px;
}
.tagline {
  font-size: 24px;
  color: #507060;
  margin-top: 6px;
  letter-spacing: 0.5px;
}
</style>
</head>
<body>
  <div class="glow1"></div>
  <div class="glow2"></div>

  <div class="tag">${nfc(s.tag)}</div>

  <div class="headline">
    <span class="l1">${nfc(s.line1)}</span>
    <span class="l2">${nfc(s.line2)}</span>
  </div>

  <p class="sub">${nfc(s.sub)}</p>

  <div class="phone-wrap">
    <div class="phone-shadow"></div>
    <div class="phone-frame">
      <div class="island"></div>
      <div class="screen">
        <img src="${imgUrl}" alt="screenshot" />
      </div>
    </div>
  </div>

  <div class="bottom">
    <div class="divider"></div>
    <div class="appname">Túi Thần Tài</div>
    <div class="tagline">Quản lý chi tiêu thông minh</div>
  </div>
</body>
</html>`;
}

// ── Generate each slide ───────────────────────────────────────────────────────
SLIDES.forEach((s) => {
  const htmlFile = path.join(OUT_DIR, `${s.id}.html`).replace(/\\/g, "/");
  const pngFile  = path.join(OUT_DIR, `${s.id}.png`).replace(/\\/g, "/");

  // Write HTML
  fs.writeFileSync(htmlFile, html(s), "utf8");

  // Capture with Chrome headless
  const fileUrl = "file:///" + htmlFile.replace(/ /g, "%20");
  const cmd = `"${CHROME}" --headless=new --disable-gpu --screenshot="${pngFile}" --window-size=1080,1920 --hide-scrollbars --force-device-scale-factor=1 --run-all-compositor-stages-before-draw --virtual-time-budget=3000 "${fileUrl}"`;

  console.log(`Rendering ${s.id}...`);
  try {
    execSync(cmd, { timeout: 30000, stdio: "pipe" });
    console.log(`  ✅  ${pngFile}`);
  } catch (e) {
    console.error(`  ❌  ${s.id}:`, e.message.slice(0, 200));
  }
});

console.log("\nDone! Files saved to:", OUT_DIR);
