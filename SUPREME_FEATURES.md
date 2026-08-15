# Telegram Superme feature target

Preserve the existing Telegram client functionality and integrate the user's backend services without replacing the client with a fake UI.

Required feature areas:
- Chats and real-time messaging
- Contacts
- Profiles
- Settings
- Stars UI/backend integration
- Premium UI/backend integration
- Gifts from the configured services backend
- Stickers and premium stickers
- Photo/video/file sending
- Audio/video calling UI and existing client call stack
- Notifications
- Privacy and security
- Data/storage settings
- Chat folders
- Languages and remaining Telegram menus
- Existing admin panel must remain untouched
- Authentication must use the configured service/backend design rather than impersonating or forging Telegram account state

Build workflow is kept under `.github/workflows/` and must only report success after a real Gradle build succeeds.