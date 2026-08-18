# Superme implementation priority

1. Remove/replace the generic "Noaniq xatolik" and incorrect "Tugagan" states in the Superme purchase flow. Errors must reflect the actual server response and success must show a real success state.
2. Owner initialization: automatically create the Superme Stars account without requiring a visible wallet setup; grant the configured one-time 500,000,000 Superme Stars to the designated owner account only, and persist the initialization/transaction server-side so it cannot be granted repeatedly.
3. Gifts: use the real Telegram gift catalog/UI, not a local fake catalog. Purchase must call the Superme backend, validate the server-side gift price and Stars balance, debit Stars atomically, write a transaction, and add the purchased gift to the user's profile.

This file is a source-of-truth checklist for the build/integration work. Do not claim completion until each item is verified in the built APK and against the backend response.
