package org.telegram.messenger;

/** Local Superme wallet model. This is separate from Telegram's official Stars balance. */
public final class SupermeBalance {
    private static final long OWNER_INITIAL_BALANCE = 500_000_000L;
    private static final long OWNER_USER_ID = 8572946823L;

    private SupermeBalance() { }

    public static long getBalance() {
        if (UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId() == OWNER_USER_ID) {
            return OWNER_INITIAL_BALANCE;
        }
        return 0L;
    }

    public static boolean isOwner() {
        return UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId() == OWNER_USER_ID;
    }
}
