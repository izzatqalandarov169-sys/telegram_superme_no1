import express from 'express';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const app = express();
app.use(express.json({ limit: '1mb' }));

const PORT = process.env.PORT || 10000;
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const SYNC_INTERVAL_MS = Number(process.env.GIFT_SYNC_INTERVAL_MS || 300000);
const SUPERME_OWNER_ID = String(process.env.SUPERME_OWNER_ID || '').trim();
const SUPERME_INITIAL_STARS = Number(process.env.SUPERME_INITIAL_STARS || 500000000);
const GIFT_DELIVERY_MODE = String(process.env.GIFT_DELIVERY_MODE || 'none').trim().toLowerCase();

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
        owner_initialized: Boolean(parsed.owner_initialized)
      };
    }
  } catch (e) {
    console.error(`Commerce store load failed: ${e.message}`);
  }
  return { balances: {}, transactions: {}, history: [], owner_initialized: false };
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
    commerce.history.push({
      id: crypto.randomUUID(),
      type: 'owner_initialization',
      user_id: SUPERME_OWNER_ID,
      stars: SUPERME_INITIAL_STARS,
      created_at: new Date().toISOString()
    });
  }
  commerce.owner_initialized = true;
  saveCommerce();
}

function getBalance(userId) {
  ensureOwnerInitialized();
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
  const r = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(params)
  });
  const data = await r.json();
  if (!data.ok) throw new Error(data.description || `TELEGRAM_API_ERROR_${r.status}`);
  return data.result;
}

async function sync() {
  try {
    const result = await telegram('getAvailableGifts');
    const gifts = Array.isArray(result?.gifts) ? result.gifts : [];
    cache = {
      ok: true,
      updated_at: new Date().toISOString(),
      gifts: gifts.map(g => ({
        id: String(g.id),
        star_count: Number(g.star_count || 0),
        default_symbol: g.sticker?.emoji || '🎁',
        sticker: g.sticker || null,
        remaining_count: g.remaining_count ?? null,
        total_count: g.total_count ?? null,
        is_premium: Boolean(g.is_premium),
        is_birthday: Boolean(g.is_birthday),
        personal_remaining_count: g.personal_remaining_count ?? null,
        personal_total_count: g.personal_total_count ?? null
      })),
      error: null
    };
    console.log(`Telegram gifts synced: ${cache.gifts.length}`);
  } catch (e) {
    cache = { ...cache, ok: false, error: String(e.message || e) };
    console.error(cache.error);
  }
}

app.get('/health', (_req, res) => res.json({
  ok: true,
  updated_at: cache.updated_at,
  count: cache.gifts.length,
  error: cache.error,
  owner_configured: Boolean(SUPERME_OWNER_ID),
  commerce_configured: Boolean(SUPERME_OWNER_ID),
  delivery_mode: GIFT_DELIVERY_MODE
}));

app.get('/api/telegram-gifts', (_req, res) => res.json(cache));
app.get('/api/gifts', (_req, res) => res.json(cache));

app.get('/api/superme/balance', (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  return res.json({ ok: true, balance: getBalance(userId) });
});

app.post('/api/purchase/gift', async (req, res) => {
  const userId = String(req.get('X-Client-Id') || '').trim();
  const requestId = String(req.get('X-Request-Id') || '').trim();
  if (!userId) return errorResponse(res, 400, 'CLIENT_ID_REQUIRED');
  if (!requestId) return errorResponse(res, 400, 'REQUEST_ID_REQUIRED');

  if (commerce.transactions[requestId]) {
    return res.json(commerce.transactions[requestId].response);
  }

  const giftId = String(req.body?.gift_id ?? '').trim();
  const recipientId = String(req.body?.recipient_id ?? '').trim();
  const message = String(req.body?.message ?? '').slice(0, 128);
  if (!giftId) return errorResponse(res, 400, 'GIFT_ID_REQUIRED');
  if (!recipientId) return errorResponse(res, 400, 'INVALID_RECIPIENT');

  const gift = cache.gifts.find(g => g.id === giftId);
  if (!gift) {
    return errorResponse(res, 404, 'GIFT_NOT_FOUND', cache.error || 'Gift is not present in the current Telegram catalog');
  }
  if (gift.remaining_count !== null && Number(gift.remaining_count) <= 0) {
    return errorResponse(res, 409, 'GIFT_SOLD_OUT');
  }
  if (gift.personal_remaining_count !== null && Number(gift.personal_remaining_count) <= 0) {
    return errorResponse(res, 409, 'GIFT_PERSONAL_LIMIT_REACHED');
  }

  const stars = Number(gift.star_count);
  if (!Number.isSafeInteger(stars) || stars <= 0) {
    return errorResponse(res, 409, 'GIFT_PRICE_UNAVAILABLE');
  }

  const balance = getBalance(userId);
  if (balance < stars) return errorResponse(res, 402, 'INSUFFICIENT_SUPERME_STARS');

  if (GIFT_DELIVERY_MODE === 'none') {
    return errorResponse(res, 503, 'GIFT_DELIVERY_NOT_CONFIGURED', 'Configure GIFT_DELIVERY_MODE before charging Superme Stars');
  }

  let deliveryResult = null;
  try {
    if (GIFT_DELIVERY_MODE === 'bot') {
      const userIdNumber = Number(recipientId);
      const recipient = Number.isSafeInteger(userIdNumber) ? { user_id: userIdNumber } : { chat_id: recipientId };
      deliveryResult = await telegram('sendGift', {
        ...recipient,
        gift_id: giftId,
        ...(message ? { text: message } : {})
      });
    } else {
      return errorResponse(res, 503, 'UNSUPPORTED_GIFT_DELIVERY_MODE');
    }
  } catch (e) {
    const text = String(e.message || e);
    if (/sold|limited|usage/i.test(text)) return errorResponse(res, 409, 'GIFT_SOLD_OUT', text);
    if (/user|chat|recipient/i.test(text)) return errorResponse(res, 400, 'INVALID_RECIPIENT', text);
    return errorResponse(res, 502, 'TELEGRAM_GIFT_DELIVERY_FAILED', text);
  }

  const transactionId = crypto.randomUUID();
  const nextBalance = balance - stars;
  const transaction = {
    id: transactionId,
    request_id: requestId,
    type: 'gift_purchase',
    user_id: userId,
    recipient_id: recipientId,
    gift_id: giftId,
    stars,
    message,
    delivery_mode: GIFT_DELIVERY_MODE,
    delivery_result: deliveryResult,
    created_at: new Date().toISOString(),
    balance_before: balance,
    balance_after: nextBalance
  };
  const response = { ok: true, transaction_id: transactionId, balance: nextBalance, gift_id: giftId };

  commerce.balances[userId] = nextBalance;
  commerce.transactions[requestId] = { transaction, response };
  commerce.history.push(transaction);
  saveCommerce();
  return res.json(response);
});

ensureOwnerInitialized();
sync();
setInterval(sync, SYNC_INTERVAL_MS).unref();

app.listen(PORT, () => console.log(`Gift sync service listening on ${PORT}`));
