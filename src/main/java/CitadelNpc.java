public enum CitadelNpc {
    BLACKSMITH(
            "Goran the Iron Hand",
            "BLACKSMITH & ARMORER",
            -7.5f,
            0,
            1.5f,
            45f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Knight.glb",
            "The Primordial Beacon's fire burns hot enough to temper Dawnsteel. Bring me crystals"
                + " and raise your battle rank, and I'll forge you weapons and armor that shatter"
                + " the frost legions.",
            "Dawnsteel armor is forged in the core embers of the Beacon. Only a seasoned Warden (Lv"
                    + " 10) can wield its full weight without freezing in the high passes."),
    ARCANIST(
            "Lady Elyndra",
            "BEACON SCHOLAR & ALCHEMIST",
            7.5f,
            0,
            1.5f,
            -45f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Mage.glb",
            "The Beacon is not merely a flame; it is the heartbeat of our world. As mentor of"
                    + " Mages, I attune staves, conduits, and enchanted robes for those who wield"
                    + " arcane power.",
            "The ancient texts say the Beacon holds back the Frost Sovereign's curse. If its core"
                    + " ever cools completely, the entire valley will freeze for eternity."),
    COMMANDER(
            "Commander Sean",
            "EXPEDITION GATEKEEPER",
            2.6f,
            0,
            29.5f,
            -60f,
            "resources/outpost/raider.glb",
            "Warden! Our scouts report heavy frost movements in the valley sectors. The gateway is"
                    + " aligned and charged with Beacon energy. Step into the portal when you are"
                    + " ready to deploy.",
            "Each chapter brings us closer to the Hollow King's fortress. Protect the Beacon in the"
                    + " field at all costs—if you fall, the expedition fails."),
    CARAVAN_TRADER(
            "Torvald the Caravan Master",
            "MERCHANT CARAVAN LEADER",
            -2.5f,
            0,
            15.5f,
            90f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Barbarian.glb",
            "Hail, Warden! Our merchant caravan just arrived through the high mountain passes. We"
                + " bring sturdy pack wagons, fresh trail rations, and restorative healing potions."
                + " Restock your satchel before you march past the south gates!",
            "The trade routes through the frost valleys are perilous, but as long as the Citadel"
                    + " stands and the Beacon burns, our caravans will keep supply lines open."),
    ARCHER(
            "Diana the Huntress",
            "ARCHER MENTOR & FLETCHER",
            -7.5f,
            0,
            -5.5f,
            45f,
            "resources/outpost/hero_archer.glb",
            "Keep your draw steady and your eye sharp, marksman! I craft precision recurve bows"
                    + " and hunting crossbows. Show me your skill on the training range, and I will"
                    + " carve you arms fit for a master.",
            "A good bow is an extension of the archer's will. Against the frost legions, distance"
                    + " and true aim are your greatest allies."),
    SURVIVOR_BRAM(
            "Bram the Woodcutter",
            "RESCUED CITIZEN",
            -8.0f,
            0,
            7.5f,
            25f,
            "resources/outpost/citizens/bram.glb",
            "Warden! You pulled me out of that freezing pass. The hearth of Aethelgard Citadel has"
                    + " never felt so warm. Our people owe you our lives!",
            "I saw the frost raiders coming down from the ridge. If you hadn't arrived and escorted"
                    + " me to the sanctuary, I would be another frozen corpse."),
    SURVIVOR_KAELEN(
            "Kaelen the Scout",
            "RESCUED REFUGEE",
            -10.0f,
            0,
            11.5f,
            -35f,
            "resources/outpost/citizens/kaelen.glb",
            "I thought the shadows had claimed me for sure until you broke their lines. Bless the"
                    + " Primordial Beacon, and bless your blade!",
            "I'm keeping watch over the southern ramparts now. Whenever you venture out, remember"
                    + " that everyone inside these walls is counting on you."),
    SURVIVOR_ROWAN(
            "Rowan the Herbalist",
            "RESCUED SURVIVOR",
            9.0f,
            0,
            8.5f,
            -60f,
            "resources/outpost/citizens/rowan.glb",
            "Thank you for saving me, Warden! I brought whatever healing herbs I could salvage to"
                    + " help Lady Elyndra brew tonics for our defenders.",
            "The frost beasts fear the Beacon's light. As long as you keep its flame burning"
                    + " bright on your expeditions, we have hope."),
    SURVIVOR_THERON(
            "Theron the Mason",
            "RESCUED ELDER",
            6.0f,
            0,
            14.0f,
            15f,
            "resources/outpost/citizens/theron.glb",
            "In all my years in the valley, I have never seen such courage. You brought me home"
                    + " through the blizzard. May the sun shine upon you!",
            "These castle walls were built by our ancestors to guard the Sun-Spires. With you"
                    + " defending the Beacon, the Citadel will stand strong."),
    SURVIVOR_MIRA(
            "Mira the Weaver",
            "RESCUED TAILOR",
            -4f,
            0,
            20f,
            90f,
            "resources/outpost/citizens/mira.glb",
            "I thought the caravan would never reach home. Now I can mend coats for the families"
                    + " arriving through the Beacon.",
            "A warm cloak matters as much as steel in these valleys. Every family you bring back"
                    + " gives this city another reason to keep going."),
    SURVIVOR_OWEN(
            "Owen the Baker",
            "RESCUED BAKER",
            5f,
            0,
            22f,
            -90f,
            "resources/outpost/citizens/owen.glb",
            "The ovens are cold, but I saved my starter and a sack of grain. We will have bread"
                    + " again, thanks to you.",
            "The gangs took our village stores before winter. Here, we share what we have. Bring"
                    + " the others home, Warden.");

    public static final CitadelNpc[] RESCUED_SURVIVORS = {
        SURVIVOR_BRAM,
        SURVIVOR_KAELEN,
        SURVIVOR_ROWAN,
        SURVIVOR_THERON,
        SURVIVOR_MIRA,
        SURVIVOR_OWEN
    };

    public static CitadelNpc[] rescuedNpcs(int count) {
        int n = Math.max(0, Math.min(RESCUED_SURVIVORS.length, count));
        CitadelNpc[] result = new CitadelNpc[n];
        System.arraycopy(RESCUED_SURVIVORS, 0, result, 0, n);
        return result;
    }

    public boolean isSurvivor() {
        return this != BLACKSMITH
                && this != ARCANIST
                && this != ARCHER
                && this != COMMANDER
                && this != CARAVAN_TRADER;
    }

    public HeroClass mentorClass() {
        return switch (this) {
            case BLACKSMITH -> HeroClass.WARRIOR;
            case ARCANIST -> HeroClass.MAGE;
            case ARCHER -> HeroClass.ARCHER;
            default -> null;
        };
    }

    private final String name;
    private final String title;
    private final float x;
    private final float y;
    private final float z;
    private final float yaw;
    private final String modelPath;
    private final String greeting;
    private final String lore;

    CitadelNpc(
            String name,
            String title,
            float x,
            float y,
            float z,
            float yaw,
            String modelPath,
            String greeting,
            String lore) {
        this.name = name;
        this.title = title;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.modelPath = modelPath;
        this.greeting = greeting;
        this.lore = lore;
    }

    public String npcName() {
        return name;
    }

    public String title() {
        return title;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public String modelPath() {
        return modelPath;
    }

    public String greeting() {
        return greeting;
    }

    public String dynamicGreeting(Progression progress) {
        if (progress == null) return greeting;
        boolean matches = mentorClass() == progress.heroClass();
        if (!progress.hasWeapon()) {
            return switch (this) {
                case BLACKSMITH ->
                        matches
                                ? "Greetings, recruit! You walk the Citadel barehanded. Take this"
                                      + " tempered Longsword—no Warrior steps into the frost wilds"
                                      + " without steel in hand! Speak with me whenever you have"
                                      + " crystals to forge greater weapons."
                                : "Hold, recruit. You carry no weapon, but your calling is not the"
                                        + " blade. Visit your class mentor ("
                                        + progress.heroClass().label
                                        + ") to receive your starter weapon!";
                case ARCANIST ->
                        matches
                                ? "Welcome, apprentice of the arcane! Before you step outside these"
                                      + " walls, take this primed Sun-Wand to channel the Beacon's"
                                      + " embers into your spells! Return to me when you gather"
                                      + " crystals to attune greater staves."
                                : "May the Beacon guide you, traveler. But your spirit is not"
                                        + " attuned to the arcane. Visit your class mentor ("
                                        + progress.heroClass().label
                                        + ") to receive your starter weapon.";
                case ARCHER ->
                        matches
                                ? "Welcome to the range, hunter! You'll need more than quick feet"
                                      + " out there—take this Hunting Bow. Keep your eye sharp and"
                                      + " your aim steady! Return to me when you are ready to craft"
                                      + " heavier recurves and crossbows."
                                : "Stand down, recruit. I only train archers and fletch bows. Visit"
                                        + " your class mentor ("
                                        + progress.heroClass().label
                                        + ") in the Citadel to claim your weapon.";
                case COMMANDER ->
                        "HALT, RECRUIT! The wilds beyond the Citadel walls are teeming with frost"
                            + " beasts! You cannot pass through this gate unarmed! Go speak with"
                            + " your class mentor ("
                                + progress.heroClass().label
                                + ") to receive your weapon before deploying!";
                default -> greeting;
            };
        }
        return switch (this) {
            case BLACKSMITH ->
                    matches
                            ? "Welcome back to the forge, Warrior! Bring me crystals and raise your"
                                    + " combat rank to forge heavier blades and dawnsteel armor."
                            : "Hold, traveler. I am the mentor of Warriors. I only forge steel"
                                    + " swords and heavy armor for Warriors! For "
                                    + progress.heroClass().label
                                    + " equipment, visit your own mentor.";
            case ARCANIST ->
                    matches
                            ? "The Beacon shines bright today, Mage. Let us attune your staves and"
                                    + " weave your enchanted robes."
                            : "Greetings, traveler. I am the mentor of Mages. I only attune"
                                    + " conduits and staves for Mages. For "
                                    + progress.heroClass().label
                                    + " equipment, visit your own mentor.";
            case ARCHER ->
                    matches
                            ? "Back from the hunt, Archer? Bring me crystals and raise your rank to"
                                    + " craft heavier bows and hunting crossbows."
                            : "Greetings, traveler. I am the mentor of Archers. I only craft bows,"
                                    + " crossbows, and ranger armor for Archers. For "
                                    + progress.heroClass().label
                                    + " equipment, visit your own mentor.";
            case COMMANDER ->
                    "Warden! The gateway is aligned and charged with Beacon energy. Step through"
                            + " the gate or into the portal to deploy.";
            default -> greeting;
        };
    }

    public String lore() {
        return lore;
    }

    public boolean isNear(float px, float pz, float radius) {
        float dx = px - x, dz = pz - z;
        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
