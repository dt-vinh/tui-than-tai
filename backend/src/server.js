import { createHmac, pbkdf2Sync, randomBytes, timingSafeEqual } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import http from 'node:http';
import { DatabaseSync } from 'node:sqlite';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = join(__dirname, '..');
const dataDir = join(rootDir, 'data');
const uploadDir = process.env.UPLOAD_DIR || join(rootDir, 'uploads');
mkdirSync(dataDir, { recursive: true });
mkdirSync(uploadDir, { recursive: true });

const PORT = Number(process.env.PORT || 8080);
const JWT_SECRET = process.env.JWT_SECRET || 'dev-only-change-this-secret-before-production';
const ACCESS_TTL_SECONDS = 15 * 60;
const REFRESH_TTL_SECONDS = 30 * 24 * 60 * 60;

const db = new DatabaseSync(process.env.DB_PATH || join(dataDir, 'tui-than-tai.sqlite'));
db.exec('PRAGMA journal_mode = WAL;');
db.exec('PRAGMA foreign_keys = ON;');

const defaultCategories = [
  ['food', 'Ăn uống', 'Food & drink', 'food,restaurant,banh mi,meal,com,pho'],
  ['coffee', 'Cafe', 'Coffee', 'coffee,cafe,tra sua,drink'],
  ['transport', 'Đi lại', 'Transport', 'taxi,grab,bus,fuel,xang'],
  ['shopping', 'Mua sắm', 'Shopping', 'shop,market,store,clothes'],
  ['bills', 'Hóa đơn cố định', 'Bills', 'electricity,water,internet,phone,bill'],
  ['home', 'Nhà cửa', 'Home', 'household,gia dung,furniture'],
  ['health', 'Sức khỏe', 'Health', 'medicine,pharmacy,clinic'],
  ['entertainment', 'Giải trí', 'Entertainment', 'movie,game,karaoke'],
  ['travel', 'Du lịch', 'Travel', 'hotel,flight,trip'],
  ['family', 'Gia đình', 'Family', 'parents,bo me,family'],
  ['gifts', 'Quà tặng', 'Gifts', 'gift,donate'],
  ['repair', 'Sửa chữa', 'Repair', 'repair,service'],
  ['other', 'Khác', 'Other', 'other']
];

initDb();

function initDb() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT NOT NULL UNIQUE,
      name TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      salt TEXT NOT NULL,
      created_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS categories (
      id TEXT PRIMARY KEY,
      name_vi TEXT NOT NULL,
      name_en TEXT NOT NULL,
      keywords TEXT NOT NULL,
      updated_at INTEGER NOT NULL,
      deleted_at INTEGER
    );
    CREATE TABLE IF NOT EXISTS expenses (
      id TEXT NOT NULL,
      user_id TEXT NOT NULL,
      title TEXT NOT NULL,
      amount INTEGER NOT NULL,
      currency TEXT NOT NULL DEFAULT 'VND',
      category_id TEXT NOT NULL DEFAULT 'other',
      wallet TEXT NOT NULL DEFAULT 'Personal',
      note TEXT,
      receipt_path TEXT,
      ocr_text TEXT,
      spent_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      deleted_at INTEGER,
      server_version INTEGER NOT NULL,
      PRIMARY KEY (id, user_id),
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE TABLE IF NOT EXISTS receipts (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      expense_id TEXT,
      file_name TEXT NOT NULL,
      mime_type TEXT NOT NULL,
      path TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE TABLE IF NOT EXISTS meta (
      key TEXT PRIMARY KEY,
      value INTEGER NOT NULL
    );
  `);

  const insertCategory = db.prepare(`
    INSERT OR IGNORE INTO categories (id, name_vi, name_en, keywords, updated_at)
    VALUES (?, ?, ?, ?, ?)
  `);
  const now = Date.now();
  for (const row of defaultCategories) insertCategory.run(...row, now);
  db.prepare(`INSERT OR IGNORE INTO meta (key, value) VALUES ('server_version', 0)`).run();
}

function nextVersion() {
  db.prepare(`UPDATE meta SET value = value + 1 WHERE key = 'server_version'`).run();
  return db.prepare(`SELECT value FROM meta WHERE key = 'server_version'`).get().value;
}

function jsonResponse(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Authorization, Content-Type',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS'
  });
  res.end(body);
}

function notFound(res) {
  jsonResponse(res, 404, { error: 'not_found' });
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', chunk => {
      size += chunk.length;
      if (size > 16 * 1024 * 1024) reject(new Error('payload_too_large'));
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      const raw = Buffer.concat(chunks).toString('utf8');
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error('invalid_json'));
      }
    });
    req.on('error', reject);
  });
}

function base64url(input) {
  return Buffer.from(input).toString('base64url');
}

function signToken(payload, ttlSeconds) {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'HS256', typ: 'JWT' };
  const body = { ...payload, iat: now, exp: now + ttlSeconds };
  const head = base64url(JSON.stringify(header));
  const data = base64url(JSON.stringify(body));
  const sig = createHmac('sha256', JWT_SECRET).update(`${head}.${data}`).digest('base64url');
  return `${head}.${data}.${sig}`;
}

function verifyToken(token) {
  const parts = String(token || '').split('.');
  if (parts.length !== 3) throw new Error('invalid_token');
  const [head, data, sig] = parts;
  const expected = createHmac('sha256', JWT_SECRET).update(`${head}.${data}`).digest('base64url');
  const a = Buffer.from(sig);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) throw new Error('invalid_token');
  const payload = JSON.parse(Buffer.from(data, 'base64url').toString('utf8'));
  if (payload.exp < Math.floor(Date.now() / 1000)) throw new Error('expired_token');
  return payload;
}

function hashPassword(password, salt = randomBytes(16).toString('hex')) {
  const hash = pbkdf2Sync(String(password), salt, 310000, 32, 'sha256').toString('hex');
  return { salt, hash };
}

function requireAuth(req) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  const payload = verifyToken(token);
  const user = db.prepare(`SELECT id, email, name FROM users WHERE id = ?`).get(payload.sub);
  if (!user) throw new Error('invalid_user');
  return user;
}

function authPayload(user) {
  return {
    user,
    accessToken: signToken({ sub: user.id, type: 'access' }, ACCESS_TTL_SECONDS),
    refreshToken: signToken({ sub: user.id, type: 'refresh' }, REFRESH_TTL_SECONDS)
  };
}

async function route(req, res) {
  if (req.method === 'OPTIONS') return jsonResponse(res, 204, {});

  const url = new URL(req.url, `http://${req.headers.host}`);
  try {
    if (req.method === 'GET' && url.pathname === '/health') {
      const version = db.prepare(`SELECT value FROM meta WHERE key = 'server_version'`).get().value;
      return jsonResponse(res, 200, { ok: true, version, time: Date.now() });
    }

    if (req.method === 'POST' && url.pathname === '/auth/register') {
      const body = await parseBody(req);
      if (!body.email || !body.password || !body.name) {
        return jsonResponse(res, 400, { error: 'missing_required_fields' });
      }
      const email = String(body.email).trim().toLowerCase();
      const existing = db.prepare(`SELECT id FROM users WHERE email = ?`).get(email);
      if (existing) return jsonResponse(res, 409, { error: 'email_exists' });
      const id = cryptoId();
      const { salt, hash } = hashPassword(body.password);
      db.prepare(`
        INSERT INTO users (id, email, name, password_hash, salt, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
      `).run(id, email, String(body.name).trim(), hash, salt, Date.now());
      return jsonResponse(res, 201, authPayload({ id, email, name: String(body.name).trim() }));
    }

    if (req.method === 'POST' && url.pathname === '/auth/login') {
      const body = await parseBody(req);
      const email = String(body.email || '').trim().toLowerCase();
      const user = db.prepare(`SELECT * FROM users WHERE email = ?`).get(email);
      if (!user) return jsonResponse(res, 401, { error: 'invalid_credentials' });
      const { hash } = hashPassword(body.password || '', user.salt);
      if (hash !== user.password_hash) return jsonResponse(res, 401, { error: 'invalid_credentials' });
      return jsonResponse(res, 200, authPayload({ id: user.id, email: user.email, name: user.name }));
    }

    if (req.method === 'POST' && url.pathname === '/auth/refresh') {
      const body = await parseBody(req);
      const payload = verifyToken(body.refreshToken);
      if (payload.type !== 'refresh') return jsonResponse(res, 401, { error: 'invalid_token' });
      const user = db.prepare(`SELECT id, email, name FROM users WHERE id = ?`).get(payload.sub);
      if (!user) return jsonResponse(res, 401, { error: 'invalid_token' });
      return jsonResponse(res, 200, authPayload(user));
    }

    if (req.method === 'GET' && url.pathname === '/categories') {
      requireAuth(req);
      const rows = db.prepare(`
        SELECT id, name_vi AS nameVi, name_en AS nameEn, keywords, updated_at AS updatedAt, deleted_at AS deletedAt
        FROM categories
        ORDER BY rowid
      `).all();
      return jsonResponse(res, 200, { categories: rows });
    }

    if (url.pathname === '/expenses') {
      const user = requireAuth(req);
      if (req.method === 'GET') {
        const rows = db.prepare(`
          SELECT * FROM expenses
          WHERE user_id = ? AND deleted_at IS NULL
          ORDER BY spent_at DESC, updated_at DESC
        `).all(user.id);
        return jsonResponse(res, 200, { expenses: rows.map(expenseDto) });
      }
      if (req.method === 'POST') {
        const body = await parseBody(req);
        const saved = upsertExpense(user.id, body);
        return jsonResponse(res, 201, { expense: saved });
      }
    }

    const expenseMatch = url.pathname.match(/^\/expenses\/([^/]+)$/);
    if (expenseMatch) {
      const user = requireAuth(req);
      const id = decodeURIComponent(expenseMatch[1]);
      if (req.method === 'PUT') {
        const body = await parseBody(req);
        const saved = upsertExpense(user.id, { ...body, id });
        return jsonResponse(res, 200, { expense: saved });
      }
      if (req.method === 'DELETE') {
        const version = nextVersion();
        db.prepare(`
          UPDATE expenses SET deleted_at = ?, updated_at = ?, server_version = ?
          WHERE id = ? AND user_id = ?
        `).run(Date.now(), Date.now(), version, id, user.id);
        return jsonResponse(res, 200, { id, deletedAt: Date.now(), serverVersion: version });
      }
    }

    if (req.method === 'POST' && url.pathname === '/receipts') {
      const user = requireAuth(req);
      const body = await parseBody(req);
      if (!body.dataBase64 || !body.fileName) {
        return jsonResponse(res, 400, { error: 'missing_file' });
      }
      const id = cryptoId();
      const safeExt = normalizeExt(body.fileName);
      const fileName = `${id}${safeExt}`;
      const path = join(uploadDir, fileName);
      writeFileSync(path, Buffer.from(body.dataBase64, 'base64'));
      db.prepare(`
        INSERT INTO receipts (id, user_id, expense_id, file_name, mime_type, path, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `).run(id, user.id, body.expenseId || null, String(body.fileName), body.mimeType || 'image/jpeg', path, Date.now());
      return jsonResponse(res, 201, { id, path: `/uploads/${fileName}` });
    }

    if (req.method === 'POST' && url.pathname === '/sync/push') {
      const user = requireAuth(req);
      const body = await parseBody(req);
      const expenses = Array.isArray(body.expenses) ? body.expenses : [];
      const saved = expenses.map(item => upsertExpense(user.id, item));
      return jsonResponse(res, 200, { expenses: saved, serverVersion: currentVersion() });
    }

    if (req.method === 'GET' && url.pathname === '/sync/pull') {
      const user = requireAuth(req);
      const sinceVersion = Number(url.searchParams.get('sinceVersion') || 0);
      const expenses = db.prepare(`
        SELECT * FROM expenses
        WHERE user_id = ? AND server_version > ?
        ORDER BY server_version ASC
      `).all(user.id, sinceVersion).map(expenseDto);
      const categories = db.prepare(`
        SELECT id, name_vi AS nameVi, name_en AS nameEn, keywords, updated_at AS updatedAt, deleted_at AS deletedAt
        FROM categories
      `).all();
      return jsonResponse(res, 200, { expenses, categories, serverVersion: currentVersion() });
    }

    return notFound(res);
  } catch (error) {
    if (['invalid_token', 'expired_token', 'invalid_user'].includes(error.message)) {
      return jsonResponse(res, 401, { error: error.message });
    }
    if (error.message === 'payload_too_large' || error.message === 'invalid_json') {
      return jsonResponse(res, 400, { error: error.message });
    }
    console.error(error);
    return jsonResponse(res, 500, { error: 'internal_error' });
  }
}

function upsertExpense(userId, input) {
  const now = Date.now();
  const id = input.id || cryptoId();
  const incomingUpdatedAt = Number(input.updatedAt || now);
  const existing = db.prepare(`SELECT * FROM expenses WHERE id = ? AND user_id = ?`).get(id, userId);
  if (existing && Number(existing.updated_at) > incomingUpdatedAt) return expenseDto(existing);
  const version = nextVersion();
  db.prepare(`
    INSERT INTO expenses (
      id, user_id, title, amount, currency, category_id, wallet, note, receipt_path,
      ocr_text, spent_at, updated_at, deleted_at, server_version
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id, user_id) DO UPDATE SET
      title = excluded.title,
      amount = excluded.amount,
      currency = excluded.currency,
      category_id = excluded.category_id,
      wallet = excluded.wallet,
      note = excluded.note,
      receipt_path = excluded.receipt_path,
      ocr_text = excluded.ocr_text,
      spent_at = excluded.spent_at,
      updated_at = excluded.updated_at,
      deleted_at = excluded.deleted_at,
      server_version = excluded.server_version
  `).run(
    id,
    userId,
    String(input.title || 'Expense'),
    Number(input.amount || 0),
    input.currency || 'VND',
    input.categoryId || input.category_id || 'other',
    input.wallet || 'Personal',
    input.note || null,
    input.receiptPath || input.receipt_path || null,
    input.ocrText || input.ocr_text || null,
    Number(input.spentAt || input.spent_at || now),
    incomingUpdatedAt,
    input.deletedAt || input.deleted_at || null,
    version
  );
  return expenseDto(db.prepare(`SELECT * FROM expenses WHERE id = ? AND user_id = ?`).get(id, userId));
}

function expenseDto(row) {
  return {
    id: row.id,
    title: row.title,
    amount: row.amount,
    currency: row.currency,
    categoryId: row.category_id,
    wallet: row.wallet,
    note: row.note,
    receiptPath: row.receipt_path,
    ocrText: row.ocr_text,
    spentAt: row.spent_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
    serverVersion: row.server_version
  };
}

function currentVersion() {
  return db.prepare(`SELECT value FROM meta WHERE key = 'server_version'`).get().value;
}

function cryptoId() {
  return randomBytes(16).toString('hex');
}

function normalizeExt(fileName) {
  const ext = extname(String(fileName)).toLowerCase();
  if (['.jpg', '.jpeg', '.png', '.webp'].includes(ext)) return ext;
  return '.jpg';
}

const server = http.createServer(route);
server.listen(PORT, () => {
  console.log(`Tui Than Tai backend listening on http://localhost:${PORT}`);
});
