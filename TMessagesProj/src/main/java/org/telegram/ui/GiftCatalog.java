package org.telegram.ui;

/** Local Superme gift catalog. */
public final class GiftCatalog {
    public static final int COUNT = 300000;
    private static final int LEGENDARY_START = 100001;
    private static final String[] NAMES = {"Pretty Posy","Whip Cupcake","Lush Bouquet","Joyful Bundle","Berry Box","Sakura Flower","Eternal Rose","Diamond Ring","Valentine Box","Cupid Charm","Galaxy Box","Royal Crown","Dragon Egg","Unicorn Horn","Crystal Heart","Magic Wand","Golden Duck","Starry Gift","Moon Pendant","Rocket Box"};
    private static final String[] EMOJIS = {"💐","🧁","🌸","🎁","🍓","🌸","🌹","💍","🍫","💘","📦","👑","🐉","🦄","💎","🪄","🦆","⭐","🌙","🚀"};
    private static final long[] PRICE_TIERS = {15,25,50,75,100,150,200,250,300,500,750,1000,2500,5000,10000};
    private GiftCatalog() {}
    public static String name(int id) { int i=Math.max(1,id); return NAMES[(i-1)%NAMES.length]+(i>NAMES.length?" #"+i:""); }
    public static String emoji(int id) { return EMOJIS[(Math.max(1,id)-1)%EMOJIS.length]; }
    public static long price(int id) { return PRICE_TIERS[(Math.max(1,id)-1)%PRICE_TIERS.length]; }
    public static String theme(int id) { String[] t={"Classic","Holiday","Love","Food","Galaxy","Nature","Fantasy","Premium"}; return t[(Math.max(1,id)-1)%t.length]; }
    public static String rarity(int id) { return Math.max(1,id)>=LEGENDARY_START?"Legendary":"Common"; }
    public static boolean isLegendary(int id) { return id>=LEGENDARY_START && id<=COUNT; }
    public static String line(int id) { return "#"+id+" "+emoji(id)+" "+name(id)+" • ⭐ "+price(id)+" • "+rarity(id); }
    public static boolean isValid(int id) { return id>=1 && id<=COUNT; }
}