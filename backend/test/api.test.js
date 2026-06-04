import assert from 'node:assert/strict';
import { mkdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
import test from 'node:test';

const root = new URL('..', import.meta.url);
const port = 19080 + Math.floor(Math.random() * 1000);
const baseUrl = `http://127.0.0.1:${port}`;
const tempRoot = join(tmpdir(), `tui-than-tai-backend-test-${Date.now()}`);
const dbPath = join(tempRoot, 'test.sqlite');
const uploadDir = join(tempRoot, 'uploads');

let child;

test.before(async () => {
  mkdirSync(uploadDir, { recursive: true });
  child = spawn(process.execPath, ['src/server.js'], {
    cwd: new URL('.', root),
    env: {
      ...process.env,
      PORT: String(port),
      JWT_SECRET: 'test-secret',
      DB_PATH: dbPath,
      UPLOAD_DIR: uploadDir
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  child.stdout.on('data', chunk => process.stdout.write(chunk));
  child.stderr.on('data', chunk => process.stderr.write(chunk));
  await waitForHealth();
});

test.after(async () => {
  if (child) {
    await stopChild(child);
  }
  rmSync(tempRoot, { recursive: true, force: true });
});

test('health returns ok and version', async () => {
  const response = await get('/health');
  assert.equal(response.ok, true);
  assert.equal(typeof response.version, 'number');
});

test('auth register, duplicate protection, invalid login, valid login', async () => {
  const body = { email: 'user@example.com', password: 'Password123!', name: 'User' };
  const registered = await post('/auth/register', body);
  assert.equal(registered.user.email, body.email);
  assert.ok(registered.accessToken);
  assert.ok(registered.refreshToken);

  const duplicate = await postRaw('/auth/register', body);
  assert.equal(duplicate.status, 409);

  const badLogin = await postRaw('/auth/login', { email: body.email, password: 'wrong' });
  assert.equal(badLogin.status, 401);

  const loggedIn = await post('/auth/login', { email: body.email, password: body.password });
  assert.ok(loggedIn.accessToken);
});

test('categories are seeded from the video-derived taxonomy', async () => {
  const token = await tokenFor('categories@example.com');
  const response = await get('/categories', token);
  assert.equal(response.categories.length, 13);
  assert.equal(response.categories[0].nameVi, 'Ăn uống');
  assert.equal(response.categories.at(-1).id, 'other');
});

test('expense push, pull, update, delete tombstone', async () => {
  const token = await tokenFor('expense@example.com');
  const expense = {
    id: 'expense-1',
    title: 'Bánh mì',
    amount: 20000,
    categoryId: 'food',
    wallet: 'Ví cá nhân',
    note: 'offline capture',
    spentAt: Date.now(),
    updatedAt: Date.now()
  };
  const pushed = await post('/sync/push', { expenses: [expense] }, token);
  assert.equal(pushed.expenses.length, 1);
  assert.equal(pushed.expenses[0].amount, 20000);

  const pulled = await get('/sync/pull?sinceVersion=0', token);
  assert.equal(pulled.expenses.length, 1);
  assert.equal(pulled.expenses[0].title, 'Bánh mì');

  const updated = await put('/expenses/expense-1', { ...expense, amount: 25000, updatedAt: Date.now() + 1 }, token);
  assert.equal(updated.expense.amount, 25000);

  const deleted = await del('/expenses/expense-1', token);
  assert.equal(deleted.id, 'expense-1');
  assert.ok(deleted.deletedAt);

  const pulledAfterDelete = await get('/sync/pull?sinceVersion=0', token);
  assert.equal(pulledAfterDelete.expenses[0].deletedAt > 0, true);
});

test('receipt upload stores metadata and returns upload path', async () => {
  const token = await tokenFor('receipt@example.com');
  const response = await post('/receipts', {
    fileName: 'receipt.jpg',
    mimeType: 'image/jpeg',
    dataBase64: Buffer.from('fake-image').toString('base64')
  }, token);
  assert.ok(response.id);
  assert.match(response.path, /^\/uploads\/.+\.jpg$/);
});

async function waitForHealth() {
  const started = Date.now();
  while (Date.now() - started < 20_000) {
    try {
      const response = await fetch(`${baseUrl}/health`);
      if (response.ok) return;
    } catch {
      // keep waiting
    }
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  throw new Error('backend did not start');
}

async function tokenFor(email) {
  const response = await post('/auth/register', { email, password: 'Password123!', name: email.split('@')[0] });
  return response.accessToken;
}

async function get(path, token = '') {
  return request('GET', path, undefined, token);
}

async function post(path, body, token = '') {
  return request('POST', path, body, token);
}

async function put(path, body, token = '') {
  return request('PUT', path, body, token);
}

async function del(path, token = '') {
  return request('DELETE', path, undefined, token);
}

async function postRaw(path, body, token = '') {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: headers(token),
    body: JSON.stringify(body)
  });
  return { status: response.status, body: await response.json() };
}

async function request(method, path, body, token = '') {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: headers(token),
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await response.text();
  const json = text ? JSON.parse(text) : {};
  assert.equal(response.ok, true, `${method} ${path} failed: ${response.status} ${text}`);
  return json;
}

function headers(token) {
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
}

function stopChild(processHandle) {
  return new Promise(resolve => {
    processHandle.once('exit', resolve);
    processHandle.kill();
    setTimeout(resolve, 2_000);
  });
}
