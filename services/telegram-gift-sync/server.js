import express from 'express';

const app = express();
app.use(express.json({ limit: '1mb' }));

const PORT = process.env.PORT || 10000;
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const SYNC_INTERVAL_MS = Number(process.env.GIFT_SYNC_INTERVAL_MS || 300000);

let cache = { ok: false, updated_at: null, gifts: [], error: null };

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

app.get('/health', (_req, res) => res.json({ ok: true, updated_at: cache.updated_at, count: cache.gifts.length, error: cache.error }));
app.get('/api/telegram-gifts', (_req, res) => res.json(cache));
app.get('/api/gifts', (_req, res) => res.json(cache));

sync();
setInterval(sync, SYNC_INTERVAL_MS).unref();

app.listen(PORT, () => console.log(`Gift sync service listening on ${PORT}`));
