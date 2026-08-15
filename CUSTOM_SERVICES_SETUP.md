# Custom services/admin/gifts changes

The provided ZIP contains a custom backend scaffold under `custom_backend/`.

Backend environment variables:
- `ADMIN_USER_ID`
- `GIFT_CHANNEL_URL`
- `GIFT_CHANNEL_ID`
- `TELEGRAM_BOT_TOKEN`

The backend provides custom app-level Stars/Premium/gifts/admin endpoints. Deploy `custom_backend/` to the backend service before using the endpoints.

Note: these app-level values do not mint real Telegram Stars or official Telegram Premium.
