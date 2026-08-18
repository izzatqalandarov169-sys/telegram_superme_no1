package org.telegram.ui;

/** Central Superme-local catalog/pricing. This does not call Telegram's payment service. */
public final class SupermePurchaseConfig {
    private SupermePurchaseConfig() {}

    public static final long OWNER_USER_ID = 8572946823L;
    public static final long OWNER_INITIAL_STARS = 500_000_000L;

    public static final long PREMIUM_MONTHLY_UZS = 15_000L;
    public static final long PREMIUM_YEARLY_UZS = 45_000L;

    public static final long[] STAR_AMOUNTS = {
            100, 150, 250, 350, 500, 750, 1000, 1500, 2500, 5000, 10000
    };
    public static final long[] STAR_PRICES_UZS = {
            10_000, 15_000, 25_000, 35_000, 40_000, 50_000,
            30_000, 60_000, 90_000, 50_000, 120_000
    };
}
