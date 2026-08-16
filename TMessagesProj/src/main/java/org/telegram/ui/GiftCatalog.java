package org.telegram.ui;

/** Local Superme gift catalog. */
public final class GiftCatalog {
    public static final int COUNT = 300000;
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

    /** Prices increase with rarity so the catalog feels like a real collectible market. */
    public static long price(int id) {
        int i = Math.max(1, id);
        if (i >= 280001) return 100000;
        if (i >= 220001) return 50000;
        if (i >= 160001) return 25000;
        if (i >= 100001) return 10000;
        if (i >= 50001) return 5000;
        if (i >= 20001) return 2500;
        if (i >= 5001) return 1000;
        if (i >= 1001) return 500;
        if (i >= 201) return 250;
        if (i >= 51) return 100;
        return 15 + ((i - 1) % 6) * 10;
    }

    public static String theme(int id) {
        String[] t = {"Classic","Holiday","Love","Food","Galaxy","Nature","Fantasy","Premium","Royal","Celestial"};
        return t[(Math.max(1, id) - 1) % t.length];
    }

    public static String rarity(int id) {
        int i = Math.max(1, id);
        if (i >= 280001) return "Mythic";
        if (i >= 220001) return "Legendary+";
        if (i >= LEGENDARY_START) return "Legendary";
        if (i >= 50001) return "Epic";
        if (i >= 20001) return "Rare";
        return "Common";
    }

    public static boolean isLegendary(int id) { return id >= LEGENDARY_START && id <= COUNT; }
    public static String line(int id) { return "#" + id + " " + emoji(id) + " " + name(id) + " • ⭐ " + price(id) + " • " + rarity(id); }
    public static boolean isValid(int id) { return id >= 1 && id <= COUNT; }
}
