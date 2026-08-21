package org.telegram.ui;

/** Central pricing/configuration for the Superme commerce layer. */
public final class SupermeCommerceConfig {
    private SupermeCommerceConfig() { }

    // Owner's Superme balance remains separate from monetary prices.
    public static final long OWNER_INITIAL_STARS = 500_000_000L;

    // FREE MODE: all Superme monetary prices are 0 UZS.
    public static final long PREMIUM_MONTHLY_UZS = 0L;
    public static final long PREMIUM_YEARLY_UZS = 0L;
    public static final long BUSINESS_MONTHLY_UZS = 0L;
    public static final long BUSINESS_YEARLY_UZS = 0L;

    // Superme Stars prices for Premium/Business are also free in this mode.
    public static final long PREMIUM_MONTHLY_STARS = 0L;
    public static final long PREMIUM_YEARLY_STARS = 0L;
    public static final long BUSINESS_MONTHLY_STARS = 0L;
    public static final long BUSINESS_YEARLY_STARS = 0L;

    // Star packages have no monetary price in FREE MODE.
    public static final long[] STAR_PACKS = {
            100L, 150L, 250L, 350L, 500L, 750L,
            1_000L, 1_500L, 2_500L, 5_000L, 10_000L
    };

    public static final long[] STAR_PRICES_UZS = {
            0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L
    };

    public static long priceForStars(long stars) {
        for (int i = 0; i < STAR_PACKS.length; i++) {
            if (STAR_PACKS[i] == stars) return STAR_PRICES_UZS[i];
        }
        return -1L;
    }
}
