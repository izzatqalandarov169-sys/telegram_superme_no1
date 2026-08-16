package org.telegram.ui;

/** Telegram gift-name catalog snapshot used by the local Superme marketplace UI. */
public final class GiftCatalog {
    public static final int COUNT = 149;

    private static final String[] NAMES = {
        "Pretty Posy","Whip Cupcake","Lush Bouquet","Joyful Bundle","Berry Box","Sakura Flower","Sky Stilettos","Ionic Dryer","Eternal Rose","Diamond Ring","Valentine Box","Cupid Charm","Love Potion","Love Candle","Cookie Heart","Trapped Heart","Pool Float","Mood Pack","Instant Ramen","Snoop Dogg","Ice Cream","Lol Pop","Happy Brownie","Clover Pin","Xmas Stocking","Swag Bag","Faith Amulet","B-Day Candle","Candy Cane","Fresh Socks","Mousse Cake","Victory Medal","Lunar Snake","Desk Calendar","Snake Box","Pet Snake","Spring Basket","Party Sparkler","Snoop Cigar","Jester Hat","Stellar Rocket","Input Key","Light Sword","Ginger Cookie","UFC Strike","Jolly Chimp","Moon Pendant","Homemade Cake","Money Pot","Restless Jar","Holiday Drink","Tama Gadget","Jelly Bunny","Evil Eye","Hypno Lollipop","Jack-in-the-Box","Star Notepad","Spy Agaric","Spiced Wine","Easter Egg","Big Year","Winter Wreath","Santa Hat","Jingle Bells","Hex Pot","Witch Hat","Khabib's Papakha","Bow Tie","Eternal Candle","Bunny Muffin","Snow Globe","Hanging Star","Crystal Ball","Top Hat","Vintage Cigar","Swiss Watch","Sleigh Bell","Rare Bird","Toy Bear","Mad Pumpkin","Skull Flower","Voodoo Doll","Snow Mittens","Low Rider","Record Player","Flying Broom","Neko Helmet","Signet Ring","Loot Bag","Sharp Tongue","Bling Binky","Electric Skull","Westside Sign","Genie Lamp","Gem Signet","Bonded Ring","Kissed Frog","Scared Cat","Ion Gem","Perfume Bottle","Mini Oscar","Nail Bracelet","Astral Shard","Magic Potion","Mighty Arm","Precious Peach","Artisan Brick","Heroic Helmet","Durov's Cap","Heart Locket","Plush Pepe","Trojan Horse","Telegram Pin","Chill Flame","Toy Chicken","Lunar Rocket","Candy Heart","Love Ring","Party Cake","Lucky Clover","Fireworks","Golden Duck","Silver Star","Crystal Heart","Royal Crown","Magic Wand","Rainbow Egg","Ocean Gem","Forest Spirit","Neon Skull","Galaxy Box","Starry Gift","Moon Cake","Sunflower","Dragon Egg","Unicorn Horn","Robot Toy","Rocket Box","Planet Ring","Comet Charm","Rainbow Lollipop","Cloud Balloon","Snowflake Globe","Lightning Bolt","Watermelon Slice","Lemon Slice","Apple Basket","Grape Bunch","Cherry Pie"
    };

    private static final String[] EMOJIS = {
        "💐","🧁","💐","🎁","🍓","🌸","👠","💇","🌹","💍","🍫","💘","🧪","🕯️","❤️","💔","🛟","😊","🍜","🐶","🍦","🍭","🍪","🍀","🎄","🛍️","🧿","🎂","🍬","🧦","🍰","🏅","🐍","📅","📦","🐍","🐍","🧺","🎉","🚬","🎩","🚀","⌨️","⚔️","🍪","🥊","🐒","🌙","🎂","🪙","🫙","🥤","🎮","🐰","👁️","🍭","🎁","📝","🍄","🍷","🥚","🎊","🌿","🎅","🔔","🪴","🧙","🧢","🎀","🕯️","🧁","❄️","⭐","🔮","🎩","🚬","⌚","🔔","🦅","🧸","🎃","🌺","🪆","🧤","🚗","🎵","🧹","🐱","💍","👜","👅","🍼","💀","🪧","🪔","💎","💍","🐸","🐈","💎","🧴","🏆","💅","🔮","🧪","💪","🍑","🧱","🪖","🧢","💗","🧸","🐴","📌","🔥","🐥","🚀","🍫","💖","🎂","🍀","🎆","🦆","⭐","💎","👑","🪄","🥚","🌊","🌲","💀","📦","⭐","🌙","🌻","🐉","🦄","🤖","🚀","🪐","☄️","🌈","☁️","❄️","⚡","🍉","🍋","🍎","🍇","🍒"
    };

    private static final long[] PRICE_TIERS = {15,25,50,75,100,150,200,250,300,500,750,1000,2500,5000,10000};

    private GiftCatalog() {}

    public static String name(int id) {
        return NAMES[(Math.max(1, id) - 1) % NAMES.length];
    }

    public static String emoji(int id) {
        return EMOJIS[(Math.max(1, id) - 1) % EMOJIS.length];
    }

    public static long price(int id) {
        return PRICE_TIERS[(Math.max(1, id) - 1) % PRICE_TIERS.length];
    }

    public static String theme(int id) {
        String[] themes = {"Classic","Holiday","Love","Food","Rare","Galaxy","Nature","Fantasy","Premium","Limited"};
        return themes[(Math.max(1, id) - 1) % themes.length];
    }

    public static String rarity(int id) {
        int index = (Math.max(1, id) - 1) % 100;
        if (index >= 98) return "Legendary";
        if (index >= 90) return "Epic";
        if (index >= 70) return "Rare";
        if (index >= 40) return "Uncommon";
        return "Common";
    }

    public static String line(int id) {
        return "#" + id + "  " + emoji(id) + "  " + name(id) + "  •  " + price(id) + " ⭐  •  " + rarity(id);
    }

    public static boolean isValid(int id) {
        return id >= 1 && id <= COUNT;
    }
}
