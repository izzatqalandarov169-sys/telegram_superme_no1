package org.telegram.ui;

/**
 * Central pricing/configuration for the Superme commerce layer.
 *
 * This is intentionally separate from Telegram's own Stars/Premium payment
 * classes: Superme purchases are local/server-side purchases and must not be
 * presented as completed Telegram transactions.
 */
public final class SupermeCommerceConfig {
    private SupermeCommerceConfig() { }

    public static final long OWNER_INITIAL_STARS = 500_000_000L;
    public static final long OWNER_MONTHLY_STARS_GRANT = 500_000_000L;

    // Prices are Uzbek so'm, exactly as specified for the Superme store.
    public static final long PREMIUM_MONTHLY_UZS = 15_000L;
    public static final long PREMIUM_YEARLY_UZS = 45_000L;

    public static final long[] STAR_PACKS = {
            100L, 150L, 250L, 350L, 500L, 750L,
            1_000L, 1_500L, 2_500L, 5_000L, 10_000L
    };

    public static final long[] STAR_PRICES_UZS = {
            10_000L, 15_000L, 25_000L, 35_000L, 40_000L, 50_000L,
            30_000L, 60_000L, 90_000L, 50_000L, 120_000L
    };

    public static long priceForStars(long stars) {
        for (int i = 0; i < STAR_PACKS.length; i++) {
            if (STAR_PACKS[i] == stars) {
                return STAR_PRICES_UZS[i];
            }
        }
        return -1L;
    }
}
