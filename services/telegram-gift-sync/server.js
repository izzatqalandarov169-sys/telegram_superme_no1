import express from 'express';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const app = express();
app.use(express.json({ limit: '1mb' }));
const PORT = process.env.PORT || 10000;
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '';
const SYNC_INTERVAL_MS = Number(process.env.GIFT_SYNC_INTERVAL_MS || 300000);
const SUPERME_OWNER_ID = String(process.env.SUPERME_OWNER_ID || '8572946823').trim();
const SUPERME_INITIAL_STARS = 500000000;
// Temporary private/testing mode: every NEW Superme account gets 500M Stars.
// Before any public release, lower this value and enable paid packages.
const WELCOME_STARS = 500000000;
const FREE_MODE = true;

const DATA_DIR = path.join(process.cwd(), 'data');
const DATA_FILE = path.join(DATA_DIR, 'superme-commerce.json');
let cache = { ok: false, updated_at: null, gifts: [], error: null };
let commerce = loadCommerce();

function loadCommerce() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const p = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
      return { balances: p.balances || {}, transactions: p.transactions || {}, history: p.history || [], owner_initialized: Boolean(p.owner_initialized), welcome_claimed: p.welcome_claimed || {}, gifts: p.gifts || {}, premium: p.premium || {}, business: p.business || {} };
    }
  } catch (e) { console.error(`Commerce load failed: ${e.message}`); }
  return { balances: {}, transactions: {}, history: [], owner_initialized: false, welcome_claimed: {}, gifts: {}, premium: {}, business: {} };
}
function saveCommerce() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  const tmp = `${DATA_FILE}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(commerce, null, 2));
  fs.renameSync(tmp, DATA_FILE);
}
function ensureOwner() {
  if (commerce.owner_initialized) return;
  if (commerce.balances[SUPERME_OWNER_ID] == null) {
    commerce.balances[SUPERME_OWNER_ID] = SUPERME_INITIAL_STARS;
    commerce.history.push({ id: crypto.randomUUID(), type: 'owner_initialization', user_id: SUPERME_OWNER_ID, stars: SUPERME_INITIAL_STARS, created_at: new Date().toISOString() });
  }
  commerce.owner_initialized = true;
  saveCommerce();
}
function ensureWelcome(userId) {
  ensureOwner();
  if (!userId || userId === SUPERME_OWNER_ID || commerce.welcome_claimed[userId]) return false;
  if (commerce.balances[userId] == null) commerce.balances[userId] = 0;
  commerce.balances[userId] += WELCOME_STARS;
  commerce.welcome_claimed[userId] = { stars: WELCOME_STARS, created_at: new Date().toISOString() };
  commerce.history.push({ id: crypto.randomUUID(), type: 'welcome_bonus', user_id: userId, stars: WELCOME_STARS, created_at: new Date().toISOString(), message: 'Sovg‘a Dilshod & ChatGPT’dan' });
  saveCommerce();
  return true;
}
function balance(userId) { ensureWelcome(userId); if (commerce.balances[userId] == null) { commerce.balances[userId] = 0; saveCommerce(); } return Number(commerce.balances[userId]); }
function fail(res, status, error, details = '') { return res.status(status).json({ ok: false, error, ...(details ? { details } : {}) }); }

async function telegram(method, params = {}) {
  if (!BOT_TOKEN) throw new Error('TELEGRAM_CATALOG_TOKEN_NOT_CONFIGURED');
  const r = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(params) });
  const d = await r.json();
  if (!d.ok) throw new Error(d.description || `TELEGRAM_API_ERROR_${r.status}`);
  return d.result;
}

async function syncCatalog() {
  try {
    const result = await telegram('getAvailableGifts');
    const source = Array.isArray(result?.gifts) ? result.gifts : [];
    const gifts = source.map((g, i) => ({
      id: String(g.id), source_id: String(g.id),
      name: g.sticker?.emoji ? `${g.sticker.emoji} Superme Gift` : `Superme Gift ${i + 1}`,
      symbol: g.sticker?.emoji || '🎁', sticker: g.sticker || null,
      source_star_count: Number(g.star_count || 0),
      price_uzs: 0,
      superme_stars: Number(g.star_count || 0),
      creator: i % 2 === 0 ? 'Dilshod' : 'ChatGPT',
      owner: 'Dilshod',
      remaining_count: g.remaining_count ?? null,
      total_count: g.total_count ?? null,
      is_premium: Boolean(g.is_premium), is_birthday: Boolean(g.is_birthday)
    }));
    cache = { ok: true, updated_at: new Date().toISOString(), gifts, error: null };
  } catch (e) { cache = { ...cache, ok: false, error: String(e.message || e) }; }
}

app.get('/health', (_req, res) => res.json({ ok: true, free_mode: FREE_MODE, welcome_stars: WELCOME_STARS, updated_at: cache.updated_at, gift_count: cache.gifts.length, catalog_error: cache.error, owner_id: SUPERME_OWNER_ID }));
app.get('/api/telegram-gifts', (_req, res) => res.json(cache));
app.get('/api/gifts', (_req, res) => res.json(cache));
app.get('/superme/external/gifts', (_req, res) => res.json(cache));
app.get('/superme/external/catalog/stars', (_req, res) => res.json({ ok: true, free_mode: true, amounts: [100,150,250,350,500,750,1000,1500,2500,5000,10000].map(stars => ({ stars, price_uzs: 0 })) }));

app.get('/api/superme/balance', (req, res) => {
  const id = String(req.get('X-Client-Id') || '').trim();
  if (!id) return fail(res, 400, 'CLIENT_ID_REQUIRED');
  return res.json({ ok: true, balance: balance(id), welcome_bonus: id === SUPERME_OWNER_ID ? 0 : WELCOME_STARS });
});
app.get('/superme/external/balance', (req, res) => {
  const id = String(req.get('X-Client-Id') || '').trim();
  if (!id) return fail(res, 400, 'CLIENT_ID_REQUIRED');
  return res.json({ ok: true, stars: balance(id), balance: balance(id), welcome_bonus: id === SUPERME_OWNER_ID ? 0 : WELCOME_STARS });
});

// Superme-only gift purchase. Telegram is never called here.
app.post('/api/purchase/gift', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const requestId = String(req.get('X-Request-Id') || crypto.randomUUID()).trim();
  if (!userId) return fail(res, 400, 'CLIENT_ID_REQUIRED');
  if (commerce.transactions[requestId]) return res.json(commerce.transactions[requestId].response);
  const giftId = String(req.body?.gift_id || '').trim();
  const recipientId = String(req.body?.recipient_id || '').trim();
  if (!giftId || !recipientId) return fail(res, 400, 'INVALID_GIFT_OR_RECIPIENT');
  const gift = cache.gifts.find(g => g.id === giftId);
  if (!gift) return fail(res, 404, 'GIFT_NOT_FOUND', 'Gift is not currently in the catalog');
  const stars = Number(gift.superme_stars || 0);
  const before = balance(userId);
  if (before < stars) return fail(res, 402, 'INSUFFICIENT_SUPERME_STARS');
  const after = before - stars;
  const tx = { id: crypto.randomUUID(), request_id: requestId, type: 'gift_purchase', user_id: userId, recipient_id: recipientId, gift_id: giftId, stars, price_uzs: 0, creator: gift.creator, owner: gift.owner, created_at: new Date().toISOString(), balance_before: before, balance_after: after };
  commerce.balances[userId] = after;
  if (!commerce.gifts[recipientId]) commerce.gifts[recipientId] = [];
  commerce.gifts[recipientId].push({ ...gift, received_from: userId, received_at: tx.created_at, transaction_id: tx.id });
  commerce.transactions[requestId] = { transaction: tx, response: { ok: true, transaction_id: tx.id, balance: after, gift_id: giftId, price_uzs: 0 } };
  commerce.history.push(tx); saveCommerce();
  return res.json(commerce.transactions[requestId].response);
});

app.get('/superme/external/profile/gifts', (req, res) => {
  const id = String(req.get('X-Client-Id') || '').trim();
  if (!id) return fail(res, 400, 'CLIENT_ID_REQUIRED');
  return res.json({ ok: true, gifts: commerce.gifts[id] || [] });
});

// Free Premium and Business: no monetary payment and no Stars charge.
app.post('/superme/external/subscription-order', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const productId = String(req.body?.product_id || '').trim();
  if (!userId || !productId) return fail(res, 400, 'INVALID_ORDER');
  const isBusiness = productId.startsWith('business');
  const store = isBusiness ? commerce.business : commerce.premium;
  store[userId] = { product_id: productId, price_uzs: 0, free: true, activated_at: new Date().toISOString() };
  saveCommerce();
  return res.json({ ok: true, order_id: crypto.randomUUID(), status: 'completed', price_uzs: 0, free: true, product_id: productId });
});
app.post('/superme/external/subscription-stars', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const productId = String(req.body?.product_id || '').trim();
  if (!userId || !productId) return fail(res, 400, 'INVALID_PRODUCT');
  const isBusiness = productId.startsWith('business');
  const store = isBusiness ? commerce.business : commerce.premium;
  store[userId] = { product_id: productId, price_uzs: 0, stars_charged: 0, free: true, activated_at: new Date().toISOString() };
  saveCommerce();
  return res.json({ ok: true, balance: balance(userId), product_id: productId, type: isBusiness ? 'superme_business' : 'superme_premium', stars_charged: 0, price_uzs: 0 });
});

// Payment is intentionally disabled while FREE_MODE is active.
app.post('/superme/external/stars-invoice', (_req, res) => fail(res, 403, 'FREE_MODE_PAYMENT_DISABLED', 'Superme is currently free: all monetary purchases cost 0 UZS.'));

ensureOwner();
syncCatalog();
setInterval(syncCatalog, SYNC_INTERVAL_MS).unref();
app.listen(PORT, () => console.log(`Superme commerce server listening on ${PORT} (FREE_MODE=${FREE_MODE}, WELCOME_STARS=${WELCOME_STARS})`));
