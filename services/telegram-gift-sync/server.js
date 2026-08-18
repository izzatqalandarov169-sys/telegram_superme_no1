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

const DATA_DIR = path.join(process.cwd(), 'data');
const DATA_FILE = path.join(DATA_DIR, 'superme-commerce.json');

let cache = { ok: false, updated_at: null, gifts: [], error: null };
let commerce = loadCommerce();

function loadCommerce() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    }
  } catch (e) {
    console.error(`Commerce store load failed: ${e.message}`);
  }
  return { balances: {}, transactions: {}, history: [] };
}

function saveCommerce() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(commerce, null, 2), 'utf8');
}

function getBalance(userId) {
  if (commerce.balances[userId] == null) {
    commerce.balances[userId] = userId === SUPERME_OWNER_ID ? SUPERME_INITIAL_STARS : 0;
    saveCommerce();
  }
  return Number(commerce.balances[userId] || 0);
}

function errorResponse(res, status, error) {
  return res.status(status).json({ ok: false, error });
}

async function telegram(method, params = {}) {
  if (!BOT_TOKEN) throw new Error('TELEGRAM_BOT_TOKEN is not configured');
  const r = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(params)
  });
  const data = await r.json();
  if (!data.ok) throw new Error(data.description || `Telegram API error ${r.status}`);
  return data.result;
}

async function sync() {
  try {
    const result = await telegram('getAvailableGifts');
    cache = {
      ok: true,
      updated_at: new Date().toISOString(),
      gifts: (result.gifts || []).map(g => ({
        id: String(g.id),
        star_count: Number(g.star_count || 0),
        default_symbol: g.sticker?.emoji || '🎁',
        sticker: g.sticker || null,
        remaining_count: g.remaining_count ?? null,
        total_count: g.total_count ?? null
      })),
      error: null
    };
    console.log(`Telegram gifts synced: ${cache.gifts.length}`);
  } catch (e) {
    cache.error = String(e.message || e);
    console.error(cache.error);
  }
}

app.get('/health', (_req, res) => res.json({
  ok: true,
  updated_at: cache.updated_at,
  count: cache.gifts.length,
  error: cache.error,
  commerce_configured: Boolean(SUPERME_OWNER_ID)
}));
app.get('/api/telegram-gifts', (_req, res) => res.json(cache));
app.get('/api/gifts', (_req, res) => res.json(cache));

app.get('/api/superme/balance', (req, res) => {
  if (!SUPERME_OWNER_ID) return errorResponse(res, 503, 'SUPERME_OWNER_ID_NOT_CONFIGURED');
  const userId = String(req.get('X-Client-Id') || '').trim();
  if (!userId || userId !== SUPERME_OWNER_ID) return errorResponse(res, 403, 'FORBIDDEN');
  return res.json({ ok: true, balance: getBalance(userId) });
});

app.post('/api/purchase/gift', (req, res) => {
  if (!SUPERME_OWNER_ID) return errorResponse(res, 503, 'SUPERME_OWNER_ID_NOT_CONFIGURED');

  const userId = String(req.get('X-Client-Id') || '').trim();
  const requestId = String(req.get('X-Request-Id') || '').trim();
  if (!userId || userId !== SUPERME_OWNER_ID) return errorResponse(res, 403, 'FORBIDDEN');
  if (!requestId) return errorResponse(res, 400, 'REQUEST_ID_REQUIRED');

  if (commerce.transactions[requestId]) {
    return res.json(commerce.transactions[requestId].response);
  }

  const giftId = String(req.body?.gift_id ?? '').trim();
  const stars = Number(req.body?.stars);
  const recipientId = String(req.body?.recipient_id ?? '').trim();
  const giftTitle = String(req.body?.gift_title ?? 'Gift').slice(0, 120);
  const message = String(req.body?.message ?? '').slice(0, 4096);

  if (!giftId || !Number.isSafeInteger(stars) || stars <= 0) {
    return errorResponse(res, 400, 'INVALID_GIFT_PRICE');
  }
  if (!recipientId) return errorResponse(res, 400, 'INVALID_RECIPIENT');

  const balance = getBalance(userId);
  if (balance < stars) return errorResponse(res, 402, 'INSUFFICIENT_SUPERME_STARS');

  const nextBalance = balance - stars;
  const transactionId = crypto.randomUUID();
  const transaction = {
    id: transactionId,
    request_id: requestId,
    type: 'gift_purchase',
    user_id: userId,
    recipient_id: recipientId,
    gift_id: giftId,
    gift_title: giftTitle,
    stars,
    message,
    created_at: new Date().toISOString(),
    balance_before: balance,
    balance_after: nextBalance
  };
  const response = { ok: true, transaction_id: transactionId, balance: nextBalance };

  commerce.balances[userId] = nextBalance;
  commerce.transactions[requestId] = { transaction, response };
  commerce.history.push(transaction);
  saveCommerce();

  console.log(`Superme gift purchase: ${transactionId} user=${userId} gift=${giftId} stars=${stars} balance=${nextBalance}`);
  return res.json(response);
});

sync();
setInterval(sync, SYNC_INTERVAL_MS).unref();

app.listen(PORT, () => console.log(`Gift sync service listening on ${PORT}`));
