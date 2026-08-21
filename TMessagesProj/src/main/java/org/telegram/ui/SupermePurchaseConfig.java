package org.telegram.ui;

/** Central Superme-local catalog/pricing. This does not call Telegram's payment service. */
public final class SupermePurchaseConfig {
    private SupermePurchaseConfig() {}

    public static final long OWNER_USER_ID = 8572946823L;
    public static final long OWNER_INITIAL_STARS = 500_000_000L;

    // FREE MODE: all Superme monetary prices are 0 UZS.
    public static final long PREMIUM_MONTHLY_UZS = 0L;
    public static final long PREMIUM_YEARLY_UZS = 0L;
    public static final long BUSINESS_MONTHLY_UZS = 0L;
    public static final long BUSINESS_YEARLY_UZS = 0L;

    public static final long[] STAR_AMOUNTS = {
            100, 150, 250, 350, 500, 750, 1000, 1500, 2500, 5000, 10000
    };

    // FREE MODE: every Superme Stars package costs 0 UZS.
    public static final long[] STAR_PRICES_UZS = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
}
