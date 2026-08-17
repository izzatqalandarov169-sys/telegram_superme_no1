package org.telegram.ui;

/** Local Superme gift catalog. */
public final class GiftCatalog {
    public static final int COUNT = 150000;
    private static final int LEGENDARY_START = 100001;

    private static final String[] NAMES = {
        "Pretty Posy","Whip Cupcake","Lush Bouquet","Joyful Bundle","Berry Box","Sakura Flower","Eternal Rose","Diamond Ring","Valentine Box","Cupid Charm",
        "Galaxy Box","Royal Crown","Dragon Egg","Unicorn Horn","Crystal Heart","Magic Wand","Golden Duck","Starry Gift","Moon Pendant","Rocket Box",
        "Golden Rose","Pearl Heart","Rainbow Cake","Lucky Clover","Moon Rabbit","Sun Crown","Cherry Bloom","Ocean Pearl","Crystal Star","Silver Swan",
        "Ruby Heart","Emerald Leaf","Sapphire Drop","Amethyst Moon","Golden Phoenix","Cosmic Orb","Neon Butterfly","Dream Cloud","Magic Potion","Royal Dragon",
        "Aurora Flower","Celestial Ring","Starlight Box","Velvet Heart","Diamond Crown","Firework Gift","Rainbow Heart","Golden Ribbon","Mystic Cat","Lunar Rabbit",
        "Solar Flame","Galaxy Rose","Crystal Butterfly","Ocean Crown","Fantasy Castle","Royal Horse","Golden Angel","Moonlight Rose","Comet Charm","Meteor Box",
        "Ice Crown","Fire Heart","Thunder Orb","Cloud Bunny","Lucky Star","Magic Book","Treasure Chest","Golden Coin","Pearl Necklace","Crystal Key"
    };

    private static final String[] EMOJIS = {
        "💐","🧁","🌸","🎁","🍓","🌺","🌹","💍","🍫","💘","📦","👑","🐉","🦄","💎","🪄","🦆","⭐","🌙","🚀",
        "🌻","🪷","🍰","🍀","🐇","☀️","🍒","🫧","🌟","🦢","❤️","🍃","💠","🔮","🔥","🔵","🦋","☁️","🧪","🐲",
        "🌌","💫","✨","🎀","💎","🎆","🌈","🎗️","🐈","🌙","☀️","🌹","🦋","👑","🏰","🐎","😇","🌷","☄️","🌠",
        "❄️","❤️‍🔥","⚡","🐰","🍯","📖","🧰","🪙","📿","🗝️"
    };

    private GiftCatalog() {}

    public static String name(int id) {
        int i = Math.max(1, id);
        return NAMES[(i - 1) % NAMES.length] + (i > NAMES.length ? " #" + i : "");
    }

    public static String emoji(int id) {
        return EMOJIS[(Math.max(1, id) - 1) % EMOJIS.length];
    }

    /** Prices are Star amounts used by the local Superme marketplace. */
    public static long price(int id) {
        int i = Math.max(1, id);
        // The requested cat gift (Mystic Cat, #49) is explicitly priced at 8.9M Stars.
        if (i == 49) return 8_900_000L;
        if (i >= 140001) return 50_000_000L;
        if (i >= 120001) return 25_000_000L;
        if (i >= 100001) return 10_000_000L;
        if (i >= 80001) return 5_000_000L;
        if (i >= 60001) return 2_500_000L;
        if (i >= 40001) return 1_000_000L;
        if (i >= 20001) return 250_000L;
        if (i >= 10001) return 100_000L;
        if (i >= 5001) return 50_000L;
        if (i >= 1001) return 10_000L;
        if (i >= 501) return 5_000L;
        if (i >= 201) return 2_500L;
        if (i >= 51) return 1_000L;
        return 100L + ((i - 1) % 10) * 100L;
    }

    public static String theme(int id) {
        String[] t = {"Classic","Holiday","Love","Food","Galaxy","Nature","Fantasy","Premium","Royal","Celestial"};
        return t[(Math.max(1, id) - 1) % t.length];
    }

    public static String rarity(int id) {
        int i = Math.max(1, id);
        if (i >= 140001) return "Mythic";
        if (i >= 120001) return "Legendary+";
        if (i >= LEGENDARY_START) return "Legendary";
        if (i >= 50001) return "Epic";
        if (i >= 20001) return "Rare";
        return "Common";
    }

    public static boolean isLegendary(int id) { return id >= LEGENDARY_START && id <= COUNT; }
    public static String line(int id) { return "#" + id + " " + emoji(id) + " " + name(id) + " • ⭐ " + price(id) + " • " + rarity(id); }
    public static boolean isValid(int id) { return id >= 1 && id <= COUNT; }
}
