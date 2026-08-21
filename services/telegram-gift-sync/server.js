import express from 'express';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const app = express();
app.use(express.json({ limit: '1mb' }));

const PORT = process.env.PORT || 10000;
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const PAYMENT_PROVIDER_TOKEN = process.env.TELEGRAM_PAYMENT_PROVIDER_TOKEN || '';
const PAYMENT_CURRENCY = process.env.PAYMENT_CURRENCY || 'UZS';
const SYNC_INTERVAL_MS = Number(process.env.GIFT_SYNC_INTERVAL_MS || 300000);
const SUPERME_OWNER_ID = String(process.env.SUPERME_OWNER_ID || '').trim();
const SUPERME_INITIAL_STARS = Number(process.env.SUPERME_INITIAL_STARS || 500000000);
const WELCOME_STARS = Number(process.env.SUPERME_WELCOME_STARS || 10000);
const GIFT_DELIVERY_MODE = String(process.env.GIFT_DELIVERY_MODE || 'bot').trim().toLowerCase();

const STAR_AMOUNTS = [100, 150, 250, 350, 500, 750, 1000, 1500, 2500, 5000, 10000];
const STAR_PRICES_UZS = [10000, 15000, 25000, 35000, 40000, 50000, 30000, 60000, 90000, 50000, 120000];
const STAR_PRICE_BY_AMOUNT = Object.fromEntries(STAR_AMOUNTS.map((a, i) => [String(a), STAR_PRICES_UZS[i]]));

const DATA_DIR = path.join(process.cwd(), 'data');
const DATA_FILE = path.join(DATA_DIR, 'superme-commerce.json');
let cache = { ok: false, updated_at: null, gifts: [], error: null };
let commerce = loadCommerce();

function loadCommerce() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
      return {
        balances: parsed.balances || {},
        transactions: parsed.transactions || {},
        history: parsed.history || [],
        owner_initialized: Boolean(parsed.owner_initialized),
        welcome_claimed: parsed.welcome_claimed || {},
        orders: parsed.orders || {},
        premium: parsed.premium || {}
      };
    }
  } catch (e) {
    console.error(`Commerce store load failed: ${e.message}`);
  }
  return { balances: {}, transactions: {}, history: [], owner_initialized: false, welcome_claimed: {}, orders: {}, premium: {} };
}

function saveCommerce() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  const tmp = `${DATA_FILE}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(commerce, null, 2), 'utf8');
  fs.renameSync(tmp, DATA_FILE);
}

function ensureOwnerInitialized() {
  if (!SUPERME_OWNER_ID || commerce.owner_initialized) return;
  if (commerce.balances[SUPERME_OWNER_ID] == null) {
    commerce.balances[SUPERME_OWNER_ID] = SUPERME_INITIAL_STARS;
    commerce.history.push({ id: crypto.randomUUID(), type: 'owner_initialization', user_id: SUPERME_OWNER_ID, stars: SUPERME_INITIAL_STARS, created_at: new Date().toISOString() });
  }
  commerce.owner_initialized = true;
  saveCommerce();
}

function ensureWelcomeBonus(userId) {
  ensureOwnerInitialized();
  if (!userId || userId === SUPERME_OWNER_ID || commerce.welcome_claimed[userId]) return false;
  if (commerce.balances[userId] == null) commerce.balances[userId] = 0;
  commerce.balances[userId] += WELCOME_STARS;
  commerce.welcome_claimed[userId] = { stars: WELCOME_STARS, created_at: new Date().toISOString() };
  commerce.history.push({ id: crypto.randomUUID(), type: 'welcome_bonus', user_id: userId, stars: WELCOME_STARS, created_at: new Date().toISOString(), message: 'Sovg‘a Dilshod & ChatGPT’dan' });
  saveCommerce();
  return true;
}

function getBalance(userId) {
  ensureWelcomeBonus(userId);
  if (commerce.balances[userId] == null) {
    commerce.balances[userId] = 0;
    saveCommerce();
  }
  return Number(commerce.balances[userId] || 0);
}

function errorResponse(res, status, error, details = null) {
  const body = { ok: false, error };
  if (details) body.details = String(details).slice(0, 500);
  return res.status(status).json(body);
}

async function telegram(method, params = {}) {
  if (!BOT_TOKEN) throw new Error('TELEGRAM_BOT_TOKEN_NOT_CONFIGURED');
  const r = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(params) });
  const data = await r.json();
  if (!data.ok) throw new Error(data.description || `TELEGRAM_API_ERROR_${r.status}`);
  return data.result;
}

async function sync() {
  try {
    const result = await telegram('getAvailableGifts');
    const gifts = Array.isArray(result?.gifts) ? result.gifts : [];
    cache = { ok: true, updated_at: new Date().toISOString(), gifts: gifts.map(g => ({
      id: String(g.id), star_count: Number(g.star_count || 0), default_symbol: g.sticker?.emoji || '🎁', sticker: g.sticker || null,
      remaining_count: g.remaining_count ?? null, total_count: g.total_count ?? null, is_premium: Boolean(g.is_premium), is_birthday: Boolean(g.is_birthday),
      personal_remaining_count: g.personal_remaining_count ?? null, personal_total_count: g.personal_total_count ?? null
    })), error: null };
  } catch (e) {
    cache = { ...cache, ok: false, error: String(e.message || e) };
  }
}

app.get('/health', (_req, res) => res.json({ ok: true, updated_at: cache.updated_at, count: cache.gifts.length, error: cache.error, owner_configured: Boolean(SUPERME_OWNER_ID), commerce_configured: true, payment_configured: Boolean(PAYMENT_PROVIDER_TOKEN), delivery_mode: GIFT_DELIVERY_MODE }));
app.get('/api/telegram-gifts', (_req, res) => res.json(cache));
app.get('/api/gifts', (_req, res) => res.json(cache));

app.get('/api/superme/balance', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  const isNew = !commerce.welcome_claimed[userId] && userId !== SUPERME_OWNER_ID;
  const balance = getBalance(userId);
  return res.json({ ok: true, balance, welcome_bonus: isNew ? WELCOME_STARS : 0 });
});

// Backward-compatible endpoints used by the Android client.
app.get('/superme/external/balance', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  const isNew = !commerce.welcome_claimed[userId] && userId !== SUPERME_OWNER_ID;
  const stars = getBalance(userId);
  return res.json({ ok: true, stars, balance: stars, welcome_bonus: isNew ? WELCOME_STARS : 0 });
});
app.get('/superme/external/gifts', (_req, res) => res.json(cache));
app.get('/superme/external/profile/gifts', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const gifts = commerce.history.filter(x => x.type === 'gift_purchase' && (x.recipient_id === userId || x.user_id === userId));
  return res.json({ ok: true, gifts });
});
app.get('/superme/external/catalog/stars', (_req, res) => res.json({ ok: true, amounts: STAR_AMOUNTS.map((stars, i) => ({ stars, price_uzs: STAR_PRICES_UZS[i] })) }));

app.post('/api/purchase/gift', async (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const requestId = String(req.get('X-Request-Id') || '').trim();
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  if (!requestId) return errorResponse(res, 400, 'REQUEST_ID_REQUIRED');
  if (commerce.transactions[requestId]) return res.json(commerce.transactions[requestId].response);

  const giftId = String(req.body?.gift_id ?? '').trim();
  const recipientId = String(req.body?.recipient_id ?? '').trim();
  const message = String(req.body?.message ?? '').slice(0, 128);
  if (!giftId) return errorResponse(res, 400, 'GIFT_ID_REQUIRED');
  if (!recipientId) return errorResponse(res, 400, 'INVALID_RECIPIENT');

  const gift = cache.gifts.find(g => g.id === giftId);
  if (!gift) return errorResponse(res, 404, 'GIFT_NOT_FOUND', cache.error || 'Gift is not present in Telegram available-gifts catalog');
  const stars = Number(gift.star_count);
  if (!Number.isSafeInteger(stars) || stars <= 0) return errorResponse(res, 409, 'GIFT_PRICE_UNAVAILABLE');
  const balance = getBalance(userId);
  if (balance < stars) return errorResponse(res, 402, 'INSUFFICIENT_SUPERME_STARS');
  if (GIFT_DELIVERY_MODE !== 'bot') return errorResponse(res, 503, 'GIFT_DELIVERY_NOT_CONFIGURED', 'Set GIFT_DELIVERY_MODE=bot');

  let deliveryResult;
  try {
    const recipientNumber = Number(recipientId);
    const recipient = Number.isSafeInteger(recipientNumber) ? { user_id: recipientNumber } : { chat_id: recipientId };
    deliveryResult = await telegram('sendGift', { ...recipient, gift_id: giftId, ...(message ? { text: message } : {}) });
  } catch (e) {
    return errorResponse(res, 502, 'TELEGRAM_GIFT_DELIVERY_FAILED', String(e.message || e));
  }

  const transactionId = crypto.randomUUID();
  const nextBalance = balance - stars;
  const transaction = { id: transactionId, request_id: requestId, type: 'gift_purchase', user_id: userId, recipient_id: recipientId, gift_id: giftId, stars, message, delivery_mode: GIFT_DELIVERY_MODE, delivery_result: deliveryResult, created_at: new Date().toISOString(), balance_before: balance, balance_after: nextBalance };
  const response = { ok: true, transaction_id: transactionId, balance: nextBalance, gift_id: giftId };
  commerce.balances[userId] = nextBalance;
  commerce.transactions[requestId] = { transaction, response };
  commerce.history.push(transaction);
  saveCommerce();
  return res.json(response);
});

// Real Telegram provider invoice for Superme Stars. No fake/test success is returned.
app.post('/superme/external/stars-invoice', async (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const stars = Number(req.body?.stars);
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  if (!STAR_PRICE_BY_AMOUNT[String(stars)]) return errorResponse(res, 400, 'INVALID_STAR_PACKAGE');
  if (!PAYMENT_PROVIDER_TOKEN) return errorResponse(res, 503, 'REAL_PAYMENT_NOT_CONFIGURED', 'Set TELEGRAM_PAYMENT_PROVIDER_TOKEN from a Telegram payment provider');
  try {
    const invoice = await telegram('createInvoiceLink', {
      title: `${stars} Superme Stars`, description: `Superme Stars package`, payload: JSON.stringify({ type: 'stars', user_id: userId, stars }), provider_token: PAYMENT_PROVIDER_TOKEN,
      currency: PAYMENT_CURRENCY, prices: [{ label: `${stars} Superme Stars`, amount: STAR_PRICE_BY_AMOUNT[String(stars)] }]
    });
    const orderId = crypto.randomUUID();
    commerce.orders[orderId] = { id: orderId, type: 'stars', user_id: userId, stars, price_uzs: STAR_PRICE_BY_AMOUNT[String(stars)], status: 'pending', created_at: new Date().toISOString() };
    saveCommerce();
    return res.json({ ok: true, order_id: orderId, invoice_url: invoice, stars, price_uzs: STAR_PRICE_BY_AMOUNT[String(stars)] });
  } catch (e) { return errorResponse(res, 502, 'PAYMENT_INVOICE_FAILED', String(e.message || e)); }
});

app.post('/telegram/webhook', (req, res) => {
  const payment = req.body?.message?.successful_payment;
  if (!payment) return res.json({ ok: true });
  let payload;
  try { payload = JSON.parse(payment.invoice_payload || '{}'); } catch { payload = {}; }
  if (payload.type !== 'stars' || !payload.user_id || !Number.isSafeInteger(Number(payload.stars))) return res.json({ ok: true });
  if (payment.currency !== PAYMENT_CURRENCY) return res.json({ ok: true });
  const txKey = `payment:${payment.telegram_payment_charge_id}`;
  if (commerce.transactions[txKey]) return res.json({ ok: true });
  const userId = String(payload.user_id); const stars = Number(payload.stars);
  const before = getBalance(userId); const after = before + stars;
  const transaction = { id: crypto.randomUUID(), type: 'stars_payment', user_id: userId, stars, created_at: new Date().toISOString(), balance_before: before, balance_after: after, charge_id: payment.telegram_payment_charge_id };
  commerce.balances[userId] = after;
  commerce.transactions[txKey] = { response: { ok: true, balance: after }, transaction };
  commerce.history.push(transaction); saveCommerce();
  return res.json({ ok: true });
});

app.post('/superme/external/subscription-order', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const productId = String(req.body?.product_id || '').trim();
  if (!userId || !productId) return errorResponse(res, 400, 'INVALID_ORDER');
  const price = productId === 'premium_month' ? 15000 : productId === 'premium_year' ? 45000 : 0;
  if (!price) return errorResponse(res, 400, 'UNKNOWN_PRODUCT');
  const orderId = crypto.randomUUID();
  commerce.orders[orderId] = { id: orderId, type: 'subscription', product_id: productId, user_id: userId, price_uzs: price, status: 'pending_payment', created_at: new Date().toISOString() };
  saveCommerce();
  return res.json({ ok: true, order_id: orderId, status: 'pending_payment', price_uzs: price, real_payment_required: true });
});

app.post('/superme/external/subscription-stars', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const productId = String(req.body?.product_id || '').trim();
  const price = productId === 'premium_month' ? 1000 : productId === 'premium_year' ? 500 : 0;
  if (!userId || !price) return errorResponse(res, 400, 'INVALID_PRODUCT');
  const balance = getBalance(userId);
  if (balance < price) return errorResponse(res, 402, 'INSUFFICIENT_SUPERME_STARS');
  const next = balance - price;
  commerce.balances[userId] = next;
  commerce.premium[userId] = { product_id: productId, activated_at: new Date().toISOString() };
  saveCommerce();
  return res.json({ ok: true, balance: next, product_id: productId, type: 'superme_premium' });
});

ensureOwnerInitialized();
sync();
setInterval(sync, SYNC_INTERVAL_MS).unref();
app.listen(PORT, () => console.log(`Gift sync service listening on ${PORT}`));
