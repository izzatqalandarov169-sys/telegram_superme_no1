# Superme Services Integration

This layer is intended to keep the Telegram-style client UI while routing Stars, Premium and Gifts through the project's own backend services.

## Required client capabilities
- Real-time chats/messages
- Contacts and profiles
- Settings/privacy/security
- Media/file sending
- Stickers and premium stickers
- Notifications
- Chat folders
- Storage/data settings
- Audio/video call UI
- Stars
- Premium
- Gifts
- Admin panel remains untouched
- Authentication is isolated from Telegram's production account service and must use the project's own backend/authentication flow.

Do not store production secrets in the repository. Configure backend URLs and credentials through build-time secrets/environment variables.
