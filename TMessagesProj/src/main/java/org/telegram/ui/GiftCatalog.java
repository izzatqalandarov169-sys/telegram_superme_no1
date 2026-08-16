package org.telegram.ui;

public final class GiftCatalog {
    public static final int COUNT = 1200;
    private static final String[] NAMES = {
        "Cake","Bunny","Rose","Heart","Star","Moon","Sun","Peach","Lollipop","Balloon",
        "Flower","Bouquet","Crown","Ring","Diamond","Crystal","Ball","Cookie","Candy","Cupcake",
        "Gift Box","Teddy","Panda","Fox","Cat","Dog","Hamster","Monkey","Koala","Penguin",
        "Parrot","Peacock","Butterfly","Bee","Ladybug","Dragon","Unicorn","Robot","Rocket","Planet",
        "Comet","Rainbow","Cloud","Snowflake","Fire","Lightning","Watermelon","Lemon","Apple","Grape",
        "Cherry","Strawberry","Mango","Bread","Coffee","Tea","Juice","Microphone","Guitar","Drum"
    };
    private static final String[] THEMES = {
        "Classic","Royal","Galaxy","Neon","Crystal","Golden","Rainbow","Cyber","Ocean","Forest",
        "Arctic","Lunar","Solar","Magic","Fantasy","Retro","Pixel","Cosmic","Cute","Festival"
    };
    private static final String[] EMOJIS = {
        "🎂","🐰","🌹","❤️","⭐","🌙","☀️","🍑","🍭","🎈","🌸","💐","👑","💍","💎","🔮",
        "🎱","🍪","🍬","🧁","🎁","🧸","🐼","🦊","🐱","🐶","🐹","🐵","🐨","🐧","🦜","🦚",
        "🦋","🐝","🐞","🐉","🦄","🤖","🚀","🪐","☄️","🌈","☁️","❄️","🔥","⚡","🍉","🍋",
        "🍎","🍇","🍒","🍓","🥭","🥖","☕","🍵","🧃","🎤","🎸","🥁"
    };
    private static final long[] PRICES = {15,25,50,75,100,150,250,500,750,1000,2500,5000};

    private GiftCatalog() {}

    public static String name(int id) {
        int index = Math.max(0, id - 1) % COUNT;
        return THEMES[index / NAMES.length] + " " + NAMES[index % NAMES.length];
    }

    public static String emoji(int id) {
        int index = Math.max(0, id - 1) % NAMES.length;
        return EMOJIS[index];
    }

    public static long price(int id) {
        int index = Math.max(0, id - 1) % PRICES.length;
        return PRICES[index];
    }

    public static String rarity(int id) {
        int index = Math.max(0, id - 1) % 100;
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
